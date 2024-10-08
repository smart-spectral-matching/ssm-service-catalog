package gov.ornl.rse.datastreams.ssm_bats_rest_api.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.JsonConversionType;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.DocumentDataset;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.repositories.DocumentRepository;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.JsonUtils;

@Component
public class DocumentService {

    /**
     * Setup logger for DocumentService.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        DocumentService.class
    );

    /**
     * Class ObjectMapper.
    */
    private static final ObjectMapper MAPPER = new ObjectMapper();


    /**
     * Configuration of application from properties.
    */
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Graph service to retrieve JSON-LD.
     */
    @Autowired
    private GraphService graphService;

    /**
     * Document store repository for dataset documents.
     */
    @Autowired
    private DocumentRepository repository;

    /**
     * Write temporary file for JSON-LD.
     *
     * @param jsonld JSON-LD to write to temporary file
     *
     * @return File object for temporary file with JSON-LD
     */
    private File writeTemporaryJsonld(
        final String jsonld
    ) throws IOException {
        // Use parser service to perform json conversion
        File tmpFile = File.createTempFile("jsonld", ".jsonld");
        BufferedWriter bf = Files.newBufferedWriter(Paths.get(tmpFile.getAbsolutePath()));
        try {
            bf.write(jsonld);
        } finally {
            bf.close();
        }
        return tmpFile;
    }

    /**
     * Get SSM JSON from a JSON-LD in a file.
     *
     * @param jsonldFile File containing JSON-LD to convert
     *
     * @return SSM JSON from conversion using file converter service
     *
     * @throws Exception
     */
    private String getJsonFromFileConverterService(
        final File jsonldFile
    ) throws Exception {
        // Get HTTP request ready for file converter service
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("upload_file", jsonldFile);
        final HttpEntity entity = builder.build();

        final String jsonEndpoint = "/convert/json";
        final String fileConverterUri = appConfig.getFileConverter().getURI() + jsonEndpoint;
        HttpPost request = new HttpPost(fileConverterUri);
        request.setEntity(entity);

        // Save file converter service's response
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(request);
        String json = IOUtils.toString(
            response.getEntity().getContent(),
            StandardCharsets.UTF_8
        );
        int status = response.getStatusLine().getStatusCode();

        // Check if correct status returned; exception if not
        if (status != HttpStatus.OK.value()) {
            LOGGER.error("Unable to get JSON from file converter service.");
            throw new Exception(
                "File converter service error; response code: " + status
            );
        }

        return json;
    }

    /**
     * Get JSON-LD for Dataset from document store.
     *
     * @param datasetUUID UUID of Dataset to get JSON-LD
     *
     * @return JSON-LD for Dataset UUID
     */
    public String getJsonld(final String datasetUUID) throws ResourceNotFoundException {
        LOGGER.info("Getting json-ld for dataset " + datasetUUID + " from document store...");
        DocumentDataset documentDataset;
            documentDataset = repository.findById(datasetUUID).orElseThrow(
                () -> new ResourceNotFoundException(
                    "Dataset " + datasetUUID + " not found in document store"
                )
            );
        LOGGER.info("Retrieved json-ld for dataset " + datasetUUID + " from document store.");
        return documentDataset.getJsonld();
    }

    /**
     * Get JSON for Dataset from document store.
     *
     * @param datasetUUID UUID of Dataset to get JSON
     *
     * @return JSON-LD for Dataset UUID
     */
    public String getJson(final String datasetUUID) throws ResourceNotFoundException {
        LOGGER.info("Getting json for dataset " + datasetUUID + " from document store...");
        DocumentDataset documentDataset;
            documentDataset = repository.findById(datasetUUID).orElseThrow(
                () -> new ResourceNotFoundException(
                    "Dataset " + datasetUUID + " not found in document store"
                )
            );
        LOGGER.info("Retrieved json for dataset " + datasetUUID + " from document store.");
        return documentDataset.getJson();
    }

    /**
     * Merge Dataset UUID JSON-LD and new JSON-LD together.
     *
     * @param datasetUUID    Dataset UUID to merge JSON-LD with
     * @param newJsonld    New JSON-LD to merge with Dataset UUID
     *
     * @return Merged JSON-LD as String
     *
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    public String mergeJsonldForDataset(
        final String datasetUUID,
        final String newJsonld
    ) throws JsonMappingException, JsonProcessingException {
        // Dataset JSON-LD
        String datasetJsonld = getJsonld(datasetUUID);
        JsonNode datasetNode = MAPPER.readTree(datasetJsonld);

        // New JSON-LD to merge
        JsonNode newNode = MAPPER.readTree(newJsonld);

        // Merge dataset and new JSON-LD via nodes
        JsonNode mergedDatasetNode = JsonUtils.merge(datasetNode, newNode);
        String mergedDatasetJsonld = mergedDatasetNode.toString();

        return mergedDatasetJsonld;
    }

    /**
     * Upload JSON-LD to Dataset UUID in document store.
     *
     * @param collectionTitle  Collection title
     * @param datasetUUID     Dataset UUID
     * @param jsonldPayload JSON-LD for Dataset
     *
     * @throws Exception
     */
    public void upload(
        final String collectionTitle,
        final String datasetUUID,
        final String jsonldPayload
    ) throws Exception {
        // Create abbreviated json
        LOGGER.info("Creating json for document store...");

        // Get JSON-LD -> SSM JSON conversion
        String json = "";
        if (appConfig.getJsonConversion().equals(JsonConversionType.EMBEDDED)) {
            json = graphService.getModelJson(collectionTitle, datasetUUID);
        } else if (
            appConfig.getJsonConversion().equals(JsonConversionType.FILE_CONVERTER_SERVICE)
        ) {
            File tmpFile = this.writeTemporaryJsonld(jsonldPayload);
            json = getJsonFromFileConverterService(tmpFile);
        }

         // Create document
        DocumentDataset document = new DocumentDataset();
        document.setDatasetId(datasetUUID);
        document.setJsonld(jsonldPayload);
        document.setJson(json);

        // Upload to document store
        repository.save(document);
    }

    /**
     * Delete Dataset UUID from document store.
     *
     * @param datasetUUID Dataset UUID to delete
     */
    public void delete(final String datasetUUID) {
        repository.delete(repository.findById(datasetUUID).get());
    }
}
