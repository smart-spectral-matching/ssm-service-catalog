package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import org.apache.jena.query.QueryException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.DCTerms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.AbbreviatedJson;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.RdfModelWriter;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.UUIDGenerator;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig.Fuseki;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ConfigUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDataset;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsModel;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.CustomizedBatsDataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.DatasetUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.DateUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.JsonUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.sparql.ModelSparql;

@RestController
@RequestMapping("/datasets")
@Validated
public class BatsModelController {

    /**
     * Setup logger for BatsDatasetController.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        BatsModelController.class
    );

    /**
     * Configuration from properties.
    */
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Configuration utilities.
    */
    @Autowired
    private ConfigUtils configUtils;

    /**
     * @return shorthand for the Fuseki configuration
     */
    private Fuseki fuseki() {
        return appConfig.getFuseki();
    }

    /**
     * Class ObjectMapper.
    */
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE);

    /**
     * Error message for uploading model.
    */
    private static final String UPLOAD_MODEL_ERROR =
        "Unable to upload model on the remote Fuseki server.";

    /**
     * Error message for reading model.
    */
    private static final String READ_MODEL_ERROR =
        "Unable to read model on the remote Fuseki server.";

    /**
     * Error message for deleting model.
    */
    private static final String DELETE_MODEL_ERROR =
        "Unable to delete model on the remote Fuseki server.";

    /**
     * Error message for creating response from model.
    */
    private static final String RESPONSE_MODEL_ERROR =
    "Unable to create response for model from the remote Fuseki server.";

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
     * Converts SciData JSON-LD payload into Bats Model.
     *
     * @param jsonldNode  SciData JSON-LD to convert to Bats Model
     * @param datasetTitle Title of the Apache Jena Dataset this model belongs to
     * @param modelUUID   UUID of output model
     * @param dataset     Bats DataSet this model will belong to
     * @param priorCreatedTime get value from prior model if updating, null if creating
     * @return            BatsModel of the JSON-LD
    */
    private BatsModel jsonldToBatsModel(
        final JsonNode jsonldNode,
        final String datasetTitle,
        final String modelUUID,
        final CustomizedBatsDataSet dataset,
        final String priorCreatedTime
    ) throws
        IOException,
        NoSuchAlgorithmException,
        UnsupportedEncodingException {
        // Check if we have a @graph node, need to move all fields to top-level
        JsonNode scidataNode = formatGraphNode(jsonldNode);
        // TODO this needs to be tested with enormous datasets,
        // and verify that memory leaks won't happen here.
        scidataNode = JsonUtils.clearTimestamps(scidataNode);

        // Replace @base in @context block w/ new URI
        String modelUri = configUtils.getModelUri(datasetTitle, modelUUID);
        String scidataString = addBaseToContextToJsonLD(scidataNode.toString(), modelUri + "/");

        // Tree -> JSON -> Jena Model
        LOGGER.info("Uploading model: " + modelUUID);
        StringReader reader = new StringReader(scidataString); //NOPMD
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

        // Jena Model -> BATS DataSet
        try {
            dataset.updateModel(modelUri, model);
            LOGGER.info("Model uploaded!");
        } catch (Exception e) {
            LOGGER.error(UPLOAD_MODEL_ERROR, e);
        }

        Model newModel = dataset.getModel(modelUri);
        return new BatsModel(modelUUID, RdfModelWriter.getJsonldForModel(newModel));
    }

    /**
     * Construct the body response for the GET method of models.
     *
     * @param models     List of models from SPARQL query
     * @param modelsUri  Uri to use for the models
     * @param pageSize   Size of the pages for pagination
     * @param pageNumber Page number for pagination
     * @return Body for JSON response as a Map for list of models
     */
    private Map<String, Object> constructModelsBody(
        final List<Map<String, Object>> models,
        final String modelsUri,
        final int pageSize,
        final int pageNumber
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        /*
        cheeky way to avoid the division twice,
        compare Option 1 vs Option 2 here:
        https://stackoverflow.com/a/21830188
        */
        int modelCount = models.size();
        final int totalPages = (modelCount - 1) / pageSize + 1;

        body.put("data", models);

        // TODO remember to update the values with the full URI once this is changed
        body.put("first", modelsUri + "?pageNumber=1&pageSize="
            + pageSize + "&returnFull=false");
        body.put("previous", modelsUri + "?pageNumber="
            + (pageNumber > 1 ? pageNumber - 1 : 1) + "&pageSize="
            + pageSize + "&returnFull=false");
        body.put("next", modelsUri + "?pageNumber="
            + (pageNumber < totalPages ? pageNumber + 1 : totalPages)
            + "&pageSize=" + pageSize + "&returnFull=false");
        body.put("last", modelsUri + "?pageNumber=" + totalPages
            + "&pageSize=" + pageSize + "&returnFull=false");
        body.put("total", modelCount);
        return body;
    }

    /**
     * FETCH a certain amount of datasets.
     *
     * @param datasetTitle Title of the Apache Jena Dataset this model belongs to
     * @param pageNumber page number to start on,
     *    must be positive (default: 1)
     * @param pageSize number of results to return,
     *    must be positive (default: 5)
     * @param returnFull boolean for returning full model or not
     * @return List either BatsModels (full) or List of Map (not full)
     */
    @RequestMapping(
        value = "/{dataset_title}/models",
        method = RequestMethod.GET)
    public ResponseEntity<?> queryModels(
        @PathVariable("dataset_title") @Pattern(regexp = BatsDataset.TITLE_REGEX)
        final String datasetTitle,
        @RequestParam(name = "pageNumber", defaultValue = "1")
        @Min(1) final int pageNumber,
        @RequestParam(name = "pageSize", defaultValue = "5")
        @Min(1) final int pageSize,
        @RequestParam(name = "returnFull", defaultValue = "false")
        final boolean returnFull
        //@RequestParam(
        //    name = "returnProperties",
        //    defaultValue = ["uuid","title","url","created","modified"]
        //) final String[] returnProperties
        // ) @Valid final String[] returnProperties
    ) {
        // final PropertyEnum[]
        // pmd does not recognize that this will always be closed
        String endpointUrl = fuseki().getHostname() + ":"
            + fuseki().getPort()
            + "/" + datasetTitle;

        ResultSet modelResults;
        try {
            modelResults = ModelSparql.queryForModelSummaries(pageSize, pageNumber, endpointUrl);
        } catch (QueryException ex) {
            return ResponseEntity.ok(Collections.EMPTY_MAP);
        }

        //Add each found model to the response
        if (returnFull) {
            CustomizedBatsDataSet dataset = DatasetUtils.initDataset(
                datasetTitle,
                fuseki()
            );
            List<BatsModel> body = ModelSparql.getFullModelsFromResult(
                dataset,
                modelResults
            );
            return ResponseEntity.ok(body);
        } else {
            // build the actual body
            List<Map<String, Object>> models;
            models = ModelSparql.getModelSummariesFromResult(modelResults);
            String modelsUri = configUtils.getDatasetUri(datasetTitle) + "/models";
            Map<String, Object> body;
            body = constructModelsBody(models, modelsUri, pageSize, pageNumber);
            return ResponseEntity.ok(body);
        }
    }

    /**
     * CREATE a new Model in the Dataset collection.
     *
     * @param datasetTitle Title for Dataset collection to add the new Model
     * @param jsonPayload JSON-LD of new Model
     * @return            BatsModel for created Model in the Dataset
    */
    @RequestMapping(
        value = "/{dataset_title}/models",
        method = RequestMethod.POST
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public BatsModel createModel(
        @PathVariable("dataset_title") @Pattern(regexp = BatsDataset.TITLE_REGEX)
        final String datasetTitle,
        @RequestBody final String jsonPayload
    ) throws
        IOException,
        NoSuchAlgorithmException,
        UnsupportedEncodingException {

        CustomizedBatsDataSet dataset = DatasetUtils.initDataset(
            datasetTitle,
            fuseki()
        );

        // Check if dataset exists
        DatasetUtils.checkDataSetExists(dataset, fuseki(), LOGGER);

        // Create Model UUID
        String modelUUID = UUIDGenerator.generateUUID();

        // JSON -> Tree
        LOGGER.info("createModel: Extracting JSON-LD -> model");
        JsonNode jsonldNode = MAPPER.readValue(
            jsonPayload,
            JsonNode.class
        );

        return jsonldToBatsModel(jsonldNode, datasetTitle, modelUUID, dataset, null);
    }

    /**
     * READ Model w/ given UUID in Dataset collection.
     *
     * @param datasetTitle Title for Dataset collection that Model belonds to
     * @param modelUUID   UUID for Model to retrieve from the Dataset
     * @param full        Boolean flag to return either full JSON-LD
     *  or abbreviated JSON model
     * @return            BatsModel for given Model UUID
    */
    @RequestMapping(
        value = "/{dataset_title}/models/{model_uuid}",
        method = RequestMethod.GET
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BatsModel getModel(
        @PathVariable("dataset_title") @Pattern(regexp = BatsDataset.TITLE_REGEX)
        final String datasetTitle,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID,
        @RequestParam(name = "full", defaultValue = "false")
        final boolean full
    ) {
        CustomizedBatsDataSet dataset = DatasetUtils.initDataset(
            datasetTitle,
            fuseki()
        );

        // Check if dataset exists
        DatasetUtils.checkDataSetExists(dataset, fuseki(), LOGGER);

        // Get the dataset's model
        String modelUri = configUtils.getModelUri(datasetTitle, modelUUID);
        LOGGER.info("Pulling model: " + modelUUID);
        Model model = dataset.getModel(modelUri);
        if (model == null) {
            LOGGER.error(READ_MODEL_ERROR);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Model " + modelUUID + " Not Found"
            );
        }

        // Either return full model or the abbrev. version from full model
        try {
            if (full) {
                // Return the full JSON-LD model
                Model newModel = dataset.getModel(modelUri);
                return new BatsModel(modelUUID, RdfModelWriter.getJsonldForModel(newModel));
            } else {
                String json = AbbreviatedJson.getJson(model);
                return new BatsModel(modelUUID, json);
            }
        } catch (Exception e) {
            LOGGER.error(RESPONSE_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to create response for model: " + modelUUID
            );
        }
    }

    /**
     * READ A list of all UUIDs for models belonging to the given dataset.
     *
     * @param datasetTitle Title of the dataset to find models for.
     * @return A JSON list of all UUIDs
     */
    @RequestMapping(
        value = "/{dataset_title}/models/uuids",
        method = RequestMethod.GET
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> getUUIDs(@PathVariable("dataset_title")
        @Pattern(regexp = BatsDataset.TITLE_REGEX) final String datasetTitle) {

        String endpointUrl = fuseki().getHostname() + ":"
            + fuseki().getPort()
            + "/" + datasetTitle;

        ArrayNode uuidArray;
        try {
            uuidArray = ModelSparql.getModelUuids(endpointUrl);
        } catch (QueryException ex) {
            return ResponseEntity.ok(Collections.EMPTY_LIST);
        }

        try {
            //Return the JSON representation
            return ResponseEntity.ok(MAPPER.writeValueAsString(uuidArray));
        } catch (JsonProcessingException e) {
            LOGGER.error(READ_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                READ_MODEL_ERROR
            );
        }
    }

    /**
     * UPDATE (REPLACE) for Model w/ UUID in Dataset collection.
     *
     * @param datasetTitle Title for Dataset collection that Model belonds to
     * @param modelUUID   UUID for Model to replace
     * @param jsonPayload JSON-LD of new Model to replace current Model
     * @return            BatsModel for newly updated Model
    */
    @RequestMapping(
        value = "/{dataset_title}/models/{model_uuid}",
        method = RequestMethod.PUT
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BatsModel updateModelReplace(
        @PathVariable("dataset_title") @Pattern(regexp = BatsDataset.TITLE_REGEX)
        final String datasetTitle,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID,
        @RequestBody final String jsonPayload
    ) throws
        IOException,
        NoSuchAlgorithmException,
        UnsupportedEncodingException {

        CustomizedBatsDataSet dataset = DatasetUtils.initDataset(
            datasetTitle,
            fuseki()
        );

        // Check if dataset exists
        DatasetUtils.checkDataSetExists(dataset, fuseki(), LOGGER);

        // JSON -> Tree
        LOGGER.info("updateModelReplace: Extracting JSON-LD -> model");
        JsonNode jsonldNode = MAPPER.readTree(jsonPayload);

        /*
        Get the dataset's model. We want to extract the created timestamp,
        instead of updating it from user params or deleting it.
        */
        String modelUri = configUtils.getModelUri(datasetTitle, modelUUID);
        LOGGER.info("Pulling model: " + modelUUID);
        String modelJSONLD;
        try {
            Model model = dataset.getModel(modelUri);
            modelJSONLD = RdfModelWriter.getJsonldForModel(model);
        } catch (Exception e) {
            LOGGER.error(READ_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Model " + modelUUID + " Not Found"
            );
        }

        // Get saved "created" value, assume it exists exactly once
        JsonNode createdTimeNode = MAPPER
            .readTree(modelJSONLD)
            .findValue(DCTerms.created.getLocalName());

        return jsonldToBatsModel(jsonldNode, datasetTitle, modelUUID,
            dataset, createdTimeNode.textValue());
    }

    /**
     * UPDATE (PARTIAL) for Model w/ UUID in Dataset collection.
     *
     * @param datasetTitle Title for Dataset collection that Model belonds to
     * @param modelUUID   UUID for Model to partially update
     * @param jsonPayload Partial JSON-LD of new Model to update current Model
     * @return            BatsModel for newly updated Model
    */
    @RequestMapping(
        value = "/{dataset_title}/models/{model_uuid}",
        method = RequestMethod.PATCH
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BatsModel updateModelPartial(
        @PathVariable("dataset_title") @Pattern(regexp = BatsDataset.TITLE_REGEX)
        final String datasetTitle,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID,
        @RequestBody final String jsonPayload
    ) throws IOException, NoSuchAlgorithmException {

        CustomizedBatsDataSet dataset = DatasetUtils.initDataset(
            datasetTitle,
            fuseki()
        );

        // Check if dataset exists
        DatasetUtils.checkDataSetExists(dataset, fuseki(), LOGGER);

        // Get the dataset's model
        String modelUri = configUtils.getModelUri(datasetTitle, modelUUID);
        LOGGER.info("Pulling model: " + modelUUID);
        String modelJSONLD;
        try {
            Model model = dataset.getModel(modelUri);
            modelJSONLD = RdfModelWriter.getJsonldForModel(model);
        } catch (Exception e) {
            LOGGER.error(READ_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Model " + modelUUID + " Not Found"
            );
        }

        JsonNode modelNode = MAPPER.readTree(modelJSONLD);
        LOGGER.info("Pulled model: " + modelUUID);
        // Get saved "created" value, assume it exists exactly once
        JsonNode createdTimeNode = modelNode.findValue(DCTerms.created.getLocalName());

        LOGGER.info("updateModelPartial: Extracting JSON-LD from body data");
        // JSON -> Tree
        JsonNode payloadNode = MAPPER.readTree(jsonPayload);

        // Merge payload with model
        JsonNode mergedModelNode = JsonUtils.merge(modelNode, payloadNode);

        return jsonldToBatsModel(mergedModelNode, datasetTitle, modelUUID,
            dataset, createdTimeNode.textValue());
    }

    /**
     * DELETE Model w/ given UUID in Dataset collection.
     *
     * @param datasetTitle Title that Model belongs to
     * @param modelUUID   UUID of Model to delete from Dataset
    */
    @RequestMapping(
        value = "/{dataset_title}/models/{model_uuid}",
        method = RequestMethod.DELETE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModel(
        @PathVariable("dataset_title") @Pattern(regexp = BatsDataset.TITLE_REGEX)
        final String datasetTitle,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID
    ) {
        CustomizedBatsDataSet dataset = DatasetUtils.initDataset(
            datasetTitle,
            fuseki()
        );

        // Check if dataset exists
        DatasetUtils.checkDataSetExists(dataset, fuseki(), LOGGER);

        // Delete the dataset's model
        String modelUri = configUtils.getModelUri(datasetTitle, modelUUID);
        LOGGER.info("Deleting model: " + modelUUID);
        try {
            dataset.deleteModel(modelUri);
        } catch (Exception e) {
            LOGGER.error(DELETE_MODEL_ERROR, e);
        }
    }
}
