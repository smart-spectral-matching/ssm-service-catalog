package gov.ornl.rse.datastreams.ssm_bats_rest_api.services;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QueryException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig.Fuseki;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ConfigUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDataset;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.CustomizedBatsCollection;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.AbbreviatedJson;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.CollectionUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.DateUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.JsonUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.RdfModelWriter;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.sparql.DatasetSparql;

@Component
public class GraphService {

    /**
     * Setup logger for GraphService.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        GraphService.class
    );

    /**
     * Configuration of application from properties.
    */
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Configuration utilities.
    */
    @Autowired
    private ConfigUtils configUtils;

    /**
     * Collection utilities.
    */
    @Autowired
    private CollectionUtils collectionUtils;

    /**
     * @return shorthand for the Fuseki configuration
     */
    private Fuseki fuseki() {
        return appConfig.getFuseki();
    }

    /**
     * Class ObjectMapper.
    */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Assert / checks if Apache Jena Model exists in Fuseki / TDB database and log.
     *
     * @param model        Model to check existence for
     * @param modelUuid    UUID of Model to report on error
     */
    private void assertModelExists(
        final Model model,
        final String modelUuid
    ) throws Exception {
        if (model == null) {
            String message = "Model " + modelUuid + " Not Found";
            LOGGER.error(message);
            throw new Exception(message);
        }
    }

    /**
     * Returns modified input JSON-LD with `@graph` at top-level.
     *
     * @param jsonldNode  JSON-LD to modify if it has @graph
     * @return            Modified JSON-LD
    */
    private JsonNode formatGraphNode(final JsonNode jsonldNode)
    throws IOException {
        LOGGER.info("Checking for @graph in dataset...");

        if (jsonldNode.has("@graph") && jsonldNode.get("@graph").isObject()) {
            // Merge @graph node into top-level and remove duplicate @id node
            LOGGER.info("Moving @graph to top-level of dataset...");
            JsonNode graphNode = ((ObjectNode) jsonldNode).remove("@graph");
            ((ObjectNode) jsonldNode).remove("@id");

            ObjectReader objectReader = MAPPER.readerForUpdating(
                jsonldNode
            );
            return objectReader.readValue(graphNode);
        }
        return jsonldNode;
    }

    /**
     * Returns modified JSON-LD w/ `@base` and `@id` inserted with URI.
     *
     * @param jsonld  JSON-LD to modify with new `@base` and `@id`
     * @param baseUri URI to use for `@base` and `@id` in the document
     * @return        Modified JSON-LD
    */
    private String addBaseToContextToJsonLD(
        final String jsonld,
        final String baseUri
    )
    throws IOException {
        // Create default output JSON-LD
        String newJsonLd = jsonld;

        // Get the @context block of the input JSON-LD
        ObjectNode jsonldNode = MAPPER.readValue(jsonld, ObjectNode.class);
        JsonNode contextNode = jsonldNode.get("@context");

        // If @context is array, replace/add @base entry with input base uri
        if (contextNode.isArray()) {

            // Re-create @context block while leaving out pre-existing @base
            ArrayNode newContextNode = MAPPER.createArrayNode();
            for (final JsonNode elementNode: contextNode) {
                if (!elementNode.has("@base")) {
                    newContextNode.add(elementNode);
                }
            }

            // Add new @base to @context block
            ObjectNode baseContext = MAPPER.createObjectNode();
            baseContext.put("@base", baseUri);
            newContextNode.add(baseContext);

            // Update JSON-LD with modified @context block
            jsonldNode.set("@context", newContextNode);

            // Update JSON-LD with new @id to match @base in @context
            jsonldNode.put("@id", baseUri);

            newJsonLd = jsonldNode.toString();
        }

        return newJsonLd;
    }

    /**
     * Transform incoming input JSON-LD prior to ingestion.
     *
     * @param collectionTitle    Title of the collection collection for the dataset
     * @param datasetUUID       UUID for the dataset
     * @param inputJsonld     Input JSON-LD from User as JsonNode
     * @return Transformed JSON-LD for SSM formatting
     */
    private String transformJsonld(
        final String collectionTitle,
        final String datasetUUID,
        final String inputJsonld
    ) throws IOException, JsonProcessingException, JsonMappingException {
        // check if we have a @graph node, need to move all fields to top-level
        JsonNode scidataNode = formatGraphNode(MAPPER.readTree(inputJsonld));

        // TODO this needs to be tested with enormous collections,
        // and verify that memory leaks won't happen here.
        scidataNode = JsonUtils.clearTimestamps(scidataNode);

        // replace @base in @context block w/ new URI
        String datasetUri = configUtils.getDatasetUri(collectionTitle, datasetUUID);
        String outputJsonld = addBaseToContextToJsonLD(scidataNode.toString(), datasetUri + "/");
        return outputJsonld;
    }


    /**
     * Converts SciData JSON-LD payload into Model.
     *
     * @param jsonld           SciData JSON-LD to convert to Model
     * @param modelUUID        UUID of output model
     * @param priorCreatedTime Get value from prior model if updating, null if creating
     * @return                 BatsModel of the JSON-LD
    */
    private Model jsonldToModel(
        final String jsonld,
        final String modelUUID,
        final String priorCreatedTime
    ) throws IOException, NoSuchAlgorithmException, UnsupportedEncodingException {
        // transform from JSON-LD string to Jena Model
        LOGGER.info("Creating model: " + modelUUID);
        StringReader reader = new StringReader(jsonld); //NOPMD
        Model model = ModelFactory.createDefaultModel();

        // TODO try to use Model.read(InputStream, String) here instead,
        // to avoid possible character encoding issues
        model.read(reader, null, "JSON-LD");
        reader.close();

        // add metadata information
        final String now = DateUtils.now();
        model.createResource(JsonUtils.METADATA_URI)
            .addProperty(DCTerms.created, priorCreatedTime == null ? now : priorCreatedTime)
            .addProperty(DCTerms.modified, now);

        return model;
    }

    /**
     * Construct the body response for the GET method of datasets.
     *
     * @param endpointUrl URI used to query Fuseki for the count
     * @param datasets     Generic object, representing list of datasets from SPARQL query
     * @param datasetsUri  Uri to use for the datasets
     * @param pageSize   Size of the pages for pagination
     * @param pageNumber Page number for pagination
     * @param returnFull boolean for returning full dataset or not
     * @return Body for JSON response as a Map for list of datasets
     */
    private Map<String, Object> constructDatasetsBody(
        final String endpointUrl,
        final Object datasets,
        final String datasetsUri,
        final int pageSize,
        final int pageNumber,
        final boolean returnFull
    ) throws QueryException {
        final Map<String, Object> body = new LinkedHashMap<>();
        final int datasetCount = DatasetSparql.getDatasetCount(endpointUrl);
        /*
        cheeky way to avoid the division twice,
        compare Option 1 vs Option 2 here:
        https://stackoverflow.com/a/21830188
        */
        final int totalPages = (datasetCount - 1) / pageSize + 1;

        body.put("data", datasets);

        // TODO remember to update the values with the full URI once this is changed
        body.put("first", datasetsUri + "?pageNumber=1&pageSize="
            + pageSize + "&returnFull=" + returnFull);
        body.put("previous", datasetsUri + "?pageNumber="
            + (pageNumber > 1 ? pageNumber - 1 : 1) + "&pageSize="
            + pageSize + "&returnFull=" + returnFull);
        body.put("next", datasetsUri + "?pageNumber="
            + (pageNumber < totalPages ? pageNumber + 1 : totalPages)
            + "&pageSize=" + pageSize + "&returnFull=" + returnFull);
        body.put("last", datasetsUri + "?pageNumber=" + totalPages
            + "&pageSize=" + pageSize + "&returnFull=" + returnFull);
        body.put("total", datasetCount);
        return body;
    }

    /**
     * Get a Model that belongs to the given Collection.
     *
     * @param collectionTitle Title of the Collection
     * @param modelUuid    UUID of the Model to fetch
     * @return Apache Jena Model for UUID
     * @throws Exception
     */
    public Model getModel(
        final String collectionTitle,
        final String modelUuid
    ) throws Exception {
        CustomizedBatsCollection collection = collectionUtils.getCollection(collectionTitle);
        String modelUri = configUtils.getDatasetUri(collectionTitle, modelUuid);
        Model model = collection.getModel(modelUri);
        assertModelExists(model, modelUuid);
        return model;
    }

    /**
     * Get JSON of Model UUID using internal converter graph JSON-LD -> SSM JSON.
     *
     * @param collectionTitle  Collection title
     * @param modelUUID     Model UUID
     *
     * @return SSM JSON representation of Model
     * @throws Exception
     */
    public String getModelJson(
        final String collectionTitle,
        final String modelUUID
    ) throws Exception {
        // Gets model uri for graph
        String endpointUrl = fuseki().getURI() + "/" + collectionTitle;
        Model model = getModel(collectionTitle, modelUUID);
        String modelUri = configUtils.getDatasetUri(collectionTitle, modelUUID);
        String json = AbbreviatedJson.getJson(endpointUrl, model, modelUri);
        return json;
    }

    /**
     * Get JSON-LD of Model UUID.
     *
     * @param collectionTitle  Collection title
     * @param modelUUID     Model UUID
     *
     * @return JSON-LD representation of Model
     * @throws Exception
     */
    public String getModelJsonld(
        final String collectionTitle,
        final String modelUUID
    ) throws Exception {
        Model model = getModel(collectionTitle, modelUUID);
        String jsonld = RdfModelWriter.getJsonldForModel(model);
        return jsonld;
    }

    /**
     * Get list of Datasets for Collection w/ pagination.
     *
     * @param collectionTitle Collection to get dataset UUIDs for
     * @param pageNumber   Page number to get for dataset UUIDs
     * @param pageSize     Size of pages (number of datasets per page)
     * @param returnFull   Boolean if we want full datasets or just summaries (default: summaries)
     *
     * @return Map of the dataset objects via UUIDs
     */
    public Map<String, Object> getDatasets(
        final String collectionTitle,
        final int pageNumber,
        final int pageSize,
        final boolean returnFull
    ) {
        CustomizedBatsCollection collection = collectionUtils.getCollection(collectionTitle);

        // final PropertyEnum[]
        // pmd does not recognize that this will always be closed
        String endpointUrl = fuseki().getURI() + "/" + collectionTitle;
        String datasetsUri = configUtils.getCollectionUri(collectionTitle) + "/datasets";

        try {
            //Add each found dataset
            if (returnFull) {
                List<BatsDataset> datasets = DatasetSparql.getFullModels(
                    pageSize,
                    pageNumber,
                    endpointUrl,
                    collection
                );
                Map<String, Object> body = constructDatasetsBody(
                    endpointUrl, datasets, datasetsUri,
                    pageSize, pageNumber, returnFull);
                return body;
            } else {
                // build the actual body
                List<Map<String, Object>> datasets = DatasetSparql.getDatasetSummaries(
                    pageSize,
                    pageNumber,
                    endpointUrl
                );
                Map<String, Object> body = constructDatasetsBody(
                    endpointUrl, datasets, datasetsUri,
                    pageSize, pageNumber, returnFull);
                return body;
            }
        } catch (QueryException ex) {
            return Collections.emptyMap();
        }
    }

    /**
     * Get list of UUIDS for the Datasets in Collection.
     *
     * @param collectionTitle Title for the collection to pull dataset UUIDs for
     *
     * @return String with the list of uuids
     * @throws JsonProcessingException
     */
    public String getDatasetUUIDsForCollection(
        final String collectionTitle
    ) throws JsonProcessingException {
        // Check if collection exists
        collectionUtils.getCollection(collectionTitle);

        String endpointUrl = fuseki().getURI() + "/" + collectionTitle;

        String output;
        try {
            ArrayNode uuidArray = DatasetSparql.getDatasetUuids(endpointUrl);
            output = MAPPER.writeValueAsString(uuidArray);
        } catch (QueryException ex) {
            output = MAPPER.writeValueAsString(Collections.EMPTY_LIST);
        }
        return output;
    }

    /**
     * Get the created time from Model UUID.
     *
     * @param collectionTitle Collection the Model UUID belongs to
     * @param modelUUID    Model UUID to get created time for
     *
     * @return String with created time for Model UUID
     *
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    public String getCreatedTimeForModel(
        final String collectionTitle,
        final String modelUUID
    ) throws JsonMappingException, JsonProcessingException {
        LOGGER.info("Pulling create time for model: " + modelUUID);

        CustomizedBatsCollection collection = collectionUtils.getCollection(collectionTitle);

        String modelUri = configUtils.getDatasetUri(collectionTitle, modelUUID);
        Model model = collection.getModel(modelUri);
        String modelJsonld = RdfModelWriter.getJsonldForModel(model);

        // Get saved "created" value, assume it exists exactly once
        JsonNode createdTimeNode = MAPPER
            .readTree(modelJsonld)
            .findValue(DCTerms.created.getLocalName());

        return createdTimeNode.textValue();
    }

    /**
     * Merge Dataset UUID and new JSON-LD together.
     *
     * @param collectionTitle Collection that Dataset UUID belongs to
     * @param datasetUUID    Dataset UUID to merge JSON-LD with
     * @param newJsonld    New JSON-LD to merge with Dataset UUID
     *
     * @return Merged JSON-LD as String
     * @throws Exception
     */
    public String mergeJsonldForDataset(
        final String collectionTitle,
        final String datasetUUID,
        final String newJsonld
    ) throws Exception {
        // Dataset JSON-LD
        String datasetJsonld = getModelJsonld(collectionTitle, datasetUUID);
        JsonNode datasetNode = MAPPER.readTree(datasetJsonld);

        // New JSON-LD to merge
        JsonNode newNode = MAPPER.readTree(newJsonld);

        // Merge dataset and new JSON-LD via nodes
        JsonNode mergedDatasetNode = JsonUtils.merge(datasetNode, newNode);
        String mergedDatasetJsonld = mergedDatasetNode.toString();

        return mergedDatasetJsonld;
    }

    /**
     * Upload Dataset to Dataset UUID in graph database w/o prior created time.
     *
     * @param collectionTitle  Collection title
     * @param datasetUUID     Dataset UUID
     * @param jsonld        JSON-LD to upload
     *
     * @return BatsDataset of the new upload Dataset
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public BatsDataset uploadJsonld(
        final String collectionTitle,
        final String datasetUUID,
        final String jsonld
    ) throws IOException, NoSuchAlgorithmException {
        return uploadJsonld(collectionTitle, datasetUUID, jsonld, null);
    }

    /**
     * Upload Model to Model UUID in graph database.
     *
     * @param collectionTitle  Collection title
     * @param modelUUID     Model UUID
     * @param jsonld        JSON-LD to upload
     * @param priorCreatedTime Prior created time to add
     *
     * @return BatsModel of the new upload Model
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public BatsDataset uploadJsonld(
        final String collectionTitle,
        final String modelUUID,
        final String jsonld,
        final String priorCreatedTime
    ) throws IOException, NoSuchAlgorithmException {
        LOGGER.info("Uploading model to graph: " + modelUUID);

        // Check if collection exists
        CustomizedBatsCollection collection = collectionUtils.getCollection(collectionTitle);

        // Transform input JSON-LD to format for Apache Jena
        String modifiedJsonld = transformJsonld(collectionTitle, modelUUID, jsonld);

        // Add Model to graph database
        Model model = jsonldToModel(modifiedJsonld, modelUUID, priorCreatedTime);
        String modelUri = configUtils.getDatasetUri(collectionTitle, modelUUID);
        collection.updateModel(modelUri, model);
        Model newModel = collection.getModel(modelUri);

        return new BatsDataset(modelUUID, RdfModelWriter.getJsonldForModel(newModel));
    }

    /**
     * Delete Dataset UUID from Collection.
     *
     * @param collectionTitle Title of Collection dataset belongs to
     * @param datasetUUID UUID of Dataset to delete
     */
    public void delete(
        final String collectionTitle,
        final String datasetUUID
    ) {
        CustomizedBatsCollection collection = collectionUtils.getCollection(collectionTitle);
        String datasetUri = configUtils.getDatasetUri(collectionTitle, datasetUUID);
        collection.deleteDataset(datasetUri);
    }
}
