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
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsModel;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.CustomizedBatsDataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.AbbreviatedJson;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.DatasetUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.DateUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.JsonUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.RdfModelWriter;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.sparql.ModelSparql;

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
     * Dataset utilities.
    */
    @Autowired
    private DatasetUtils datasetUtils;

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
        LOGGER.info("Checking for @graph in model...");

        if (jsonldNode.has("@graph") && jsonldNode.get("@graph").isObject()) {
            // Merge @graph node into top-level and remove duplicate @id node
            LOGGER.info("Moving @graph to top-level of model...");
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
     * @param datasetTitle    Title of the dataset collection for the model
     * @param modelUUID       UUID for the model
     * @param inputJsonld     Input JSON-LD from User as JsonNode
     * @return Transformed JSON-LD for SSM formatting
     */
    private String transformJsonld(
        final String datasetTitle,
        final String modelUUID,
        final String inputJsonld
    ) throws IOException, JsonProcessingException, JsonMappingException {
        // check if we have a @graph node, need to move all fields to top-level
        JsonNode scidataNode = formatGraphNode(MAPPER.readTree(inputJsonld));

        // TODO this needs to be tested with enormous datasets,
        // and verify that memory leaks won't happen here.
        scidataNode = JsonUtils.clearTimestamps(scidataNode);

        // replace @base in @context block w/ new URI
        String modelUri = configUtils.getModelUri(datasetTitle, modelUUID);
        String outputJsonld = addBaseToContextToJsonLD(scidataNode.toString(), modelUri + "/");
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
     * Construct the body response for the GET method of models.
     *
     * @param endpointUrl URI used to query Fuseki for the count
     * @param models     Generic object, representing list of models from SPARQL query
     * @param modelsUri  Uri to use for the models
     * @param pageSize   Size of the pages for pagination
     * @param pageNumber Page number for pagination
     * @param returnFull boolean for returning full model or not
     * @return Body for JSON response as a Map for list of models
     */
    private Map<String, Object> constructModelsBody(
        final String endpointUrl,
        final Object models,
        final String modelsUri,
        final int pageSize,
        final int pageNumber,
        final boolean returnFull
    ) throws QueryException {
        final Map<String, Object> body = new LinkedHashMap<>();
        final int modelCount = ModelSparql.getModelCount(endpointUrl);
        /*
        cheeky way to avoid the division twice,
        compare Option 1 vs Option 2 here:
        https://stackoverflow.com/a/21830188
        */
        final int totalPages = (modelCount - 1) / pageSize + 1;

        body.put("data", models);

        // TODO remember to update the values with the full URI once this is changed
        body.put("first", modelsUri + "?pageNumber=1&pageSize="
            + pageSize + "&returnFull=" + returnFull);
        body.put("previous", modelsUri + "?pageNumber="
            + (pageNumber > 1 ? pageNumber - 1 : 1) + "&pageSize="
            + pageSize + "&returnFull=" + returnFull);
        body.put("next", modelsUri + "?pageNumber="
            + (pageNumber < totalPages ? pageNumber + 1 : totalPages)
            + "&pageSize=" + pageSize + "&returnFull=" + returnFull);
        body.put("last", modelsUri + "?pageNumber=" + totalPages
            + "&pageSize=" + pageSize + "&returnFull=" + returnFull);
        body.put("total", modelCount);
        return body;
    }

    /**
     * Get a Model that belongs to the given Dataset.
     *
     * @param datasetTitle Title of the Dataset
     * @param modelUuid    UUID of the Model to fetch
     * @return Apache Jena Model for UUID
     * @throws Exception
     */
    public Model getModel(
        final String datasetTitle,
        final String modelUuid
    ) throws Exception {
        CustomizedBatsDataSet dataset = datasetUtils.getDataset(datasetTitle);
        String modelUri = configUtils.getModelUri(datasetTitle, modelUuid);
        Model model = dataset.getModel(modelUri);
        assertModelExists(model, modelUuid);
        return model;
    }

    /**
     * Get JSON of Model UUID using internal converter graph JSON-LD -> SSM JSON.
     *
     * @param datasetTitle  Dataset title
     * @param modelUUID     Model UUID
     *
     * @return SSM JSON representation of Model
     * @throws Exception
     */
    public String getModelJson(
        final String datasetTitle,
        final String modelUUID
    ) throws Exception {
        // Gets model uri for graph
        String endpointUrl = fuseki().getURI() + "/" + datasetTitle;
        Model model = getModel(datasetTitle, modelUUID);
        String modelUri = configUtils.getModelUri(datasetTitle, modelUUID);
        String json = AbbreviatedJson.getJson(endpointUrl, model, modelUri);
        return json;
    }

    /**
     * Get JSON-LD of Model UUID.
     *
     * @param datasetTitle  Dataset title
     * @param modelUUID     Model UUID
     *
     * @return JSON-LD representation of Model
     * @throws Exception
     */
    public String getModelJsonld(
        final String datasetTitle,
        final String modelUUID
    ) throws Exception {
        Model model = getModel(datasetTitle, modelUUID);
        String jsonld = RdfModelWriter.getJsonldForModel(model);
        return jsonld;
    }

    /**
     * Get list of Models for Dataset w/ pagination.
     *
     * @param datasetTitle Dataset to get model UUIDs for
     * @param pageNumber   Page number to get for model UUIDs
     * @param pageSize     Size of pages (number of models per page)
     * @param returnFull   Boolean if we want full models or just summaries (default: summaries)
     *
     * @return Map of the model objects via UUIDs
     */
    public Map<String, Object> getModels(
        final String datasetTitle,
        final int pageNumber,
        final int pageSize,
        final boolean returnFull
    ) {
        CustomizedBatsDataSet dataset = datasetUtils.getDataset(datasetTitle);

        // final PropertyEnum[]
        // pmd does not recognize that this will always be closed
        String endpointUrl = fuseki().getURI() + "/" + datasetTitle;
        String modelsUri = configUtils.getDatasetUri(datasetTitle) + "/models";

        try {
            //Add each found model
            if (returnFull) {
                List<BatsModel> models = ModelSparql.getFullModels(
                    pageSize,
                    pageNumber,
                    endpointUrl,
                    dataset
                );
                Map<String, Object> body = constructModelsBody(
                    endpointUrl, models, modelsUri,
                    pageSize, pageNumber, returnFull);
                return body;
            } else {
                // build the actual body
                List<Map<String, Object>> models = ModelSparql.getModelSummaries(
                    pageSize,
                    pageNumber,
                    endpointUrl
                );
                Map<String, Object> body = constructModelsBody(
                    endpointUrl, models, modelsUri,
                    pageSize, pageNumber, returnFull);
                return body;
            }
        } catch (QueryException ex) {
            return Collections.emptyMap();
        }
    }

    /**
     * Get list of UUIDS for the Models in Dataset.
     *
     * @param datasetTitle Title for the dataset to pull model UUIDs for
     *
     * @return String with the list of uuids
     * @throws JsonProcessingException
     */
    public String getModelUUIDsForDataset(
        final String datasetTitle
    ) throws JsonProcessingException {
        // Check if dataset exists
        datasetUtils.getDataset(datasetTitle);

        String endpointUrl = fuseki().getURI() + "/" + datasetTitle;

        String output;
        try {
            ArrayNode uuidArray = ModelSparql.getModelUuids(endpointUrl);
            output = MAPPER.writeValueAsString(uuidArray);
        } catch (QueryException ex) {
            output = MAPPER.writeValueAsString(Collections.EMPTY_LIST);
        }
        return output;
    }

    /**
     * Get the created time from Model UUID.
     *
     * @param datasetTitle Dataset the Model UUID belongs to
     * @param modelUUID    Model UUID to get created time for
     *
     * @return String with created time for Model UUID
     *
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    public String getCreatedTimeForModel(
        final String datasetTitle,
        final String modelUUID
    ) throws JsonMappingException, JsonProcessingException {
        LOGGER.info("Pulling create time for model: " + modelUUID);

        CustomizedBatsDataSet dataset = datasetUtils.getDataset(datasetTitle);

        String modelUri = configUtils.getModelUri(datasetTitle, modelUUID);
        Model model = dataset.getModel(modelUri);
        String modelJsonld = RdfModelWriter.getJsonldForModel(model);

        // Get saved "created" value, assume it exists exactly once
        JsonNode createdTimeNode = MAPPER
            .readTree(modelJsonld)
            .findValue(DCTerms.created.getLocalName());

        return createdTimeNode.textValue();
    }

    /**
     * Merge Model UUID and new JSON-LD together.
     *
     * @param datasetTitle Dataset that Model UUID belongs to
     * @param modelUUID    Model UUID to merge JSON-LD with
     * @param newJsonld    New JSON-LD to merge with Model UUID
     *
     * @return Merged JSON-LD as String
     * @throws Exception
     */
    public String mergeJsonldForModel(
        final String datasetTitle,
        final String modelUUID,
        final String newJsonld
    ) throws Exception {
        // Model JSON-LD
        String modelJsonld = getModelJsonld(datasetTitle, modelUUID);
        JsonNode modelNode = MAPPER.readTree(modelJsonld);

        // New JSON-LD to merge
        JsonNode newNode = MAPPER.readTree(newJsonld);

        // Merge model and new JSON-LD via nodes
        JsonNode mergedModelNode = JsonUtils.merge(modelNode, newNode);
        String mergedModelJsonld = mergedModelNode.toString();

        return mergedModelJsonld;
    }

    /**
     * Upload Model to Model UUID in graph database w/o prior created time.
     *
     * @param datasetTitle  Dataset title
     * @param modelUUID     Model UUID
     * @param jsonld        JSON-LD to upload
     *
     * @return BatsModel of the new upload Model
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public BatsModel uploadJsonld(
        final String datasetTitle,
        final String modelUUID,
        final String jsonld
    ) throws IOException, NoSuchAlgorithmException {
        return uploadJsonld(datasetTitle, modelUUID, jsonld, null);
    }

    /**
     * Upload Model to Model UUID in graph database.
     *
     * @param datasetTitle  Dataset title
     * @param modelUUID     Model UUID
     * @param jsonld        JSON-LD to upload
     * @param priorCreatedTime Prior created time to add
     *
     * @return BatsModel of the new upload Model
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public BatsModel uploadJsonld(
        final String datasetTitle,
        final String modelUUID,
        final String jsonld,
        final String priorCreatedTime
    ) throws IOException, NoSuchAlgorithmException {
        LOGGER.info("Uploading model to graph: " + modelUUID);

        // Check if dataset exists
        CustomizedBatsDataSet dataset = datasetUtils.getDataset(datasetTitle);

        // Transform input JSON-LD to format for Apache Jena
        String modifiedJsonld = transformJsonld(datasetTitle, modelUUID, jsonld);

        // Add Model to graph database
        Model model = jsonldToModel(modifiedJsonld, modelUUID, priorCreatedTime);
        String modelUri = configUtils.getModelUri(datasetTitle, modelUUID);
        dataset.updateModel(modelUri, model);
        Model newModel = dataset.getModel(modelUri);

        return new BatsModel(modelUUID, RdfModelWriter.getJsonldForModel(newModel));
    }

    /**
     * Delete Model UUID from Dataset.
     *
     * @param datasetTitle Title of Dataset model belongs to
     * @param modelUUID UUID of Model to delete
     */
    public void delete(
        final String datasetTitle,
        final String modelUUID
    ) {
        CustomizedBatsDataSet dataset = datasetUtils.getDataset(datasetTitle);
        String modelUri = configUtils.getModelUri(datasetTitle, modelUUID);
        dataset.deleteModel(modelUri);
    }
}
