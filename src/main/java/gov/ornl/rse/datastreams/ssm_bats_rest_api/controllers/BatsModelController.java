package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.ornl.rse.bats.DataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.RdfModelWriter;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.UUIDGenerator;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig.Fuseki;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ConfigUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsModel;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.DatasetUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.DateUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.JsonUtils;

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
     * @return shorthand for the Fuseki configuration
     */
    private Fuseki fuseki() {
        return appConfig.getFuseki();
    }

    /**
     *
     * @param uuid dataset UUID from user parameter
     * @return Bats dataset with name, Fuseki host, and Fuseki port configured
     */
    private DataSet initDataset(final String uuid) {
        DataSet dataset = new DataSet();
        dataset.setName(uuid);
        dataset.setHost(fuseki().getHostname());
        dataset.setPort(fuseki().getPort());
        return dataset;
    }

    /**
     * Returns Model API URI given the Dataset and Model UUID.
     *
     * @param datasetUUID UUID for the Dataset the model belongs to
     * @param modelUUID   UUID for the Model
     * @return            Full URI for the Model
    */
    private String getModelUri(
        final String datasetUUID,
        final String modelUUID
    ) {
        String baseUri = configUtils.getBasePath();
        String datasetUri = baseUri + "/datasets/" + datasetUUID;
        String modelUri = datasetUri + "/models/" + modelUUID + "/";
        return modelUri.replace("\"", "");
    }

    /**
     * <p>
     * Prepare to query a dataset for all of its model UUIDs.
     * </p>
     * <p>
     * Note that the return value does not actually execute, as different
     * implementations may want to handle exceptions differently.
     * </p>
     *
     * @param datasetUUID UUID from the dataset.
     * @param queryStr literal query to call
     * @return a prepared query, ready to be executed
     */
    private QueryExecution prepareModelUUIDQuery(final String datasetUUID,
        final String queryStr) {
        //SPARQL query to find all unique graphs
        ParameterizedSparqlString sparql = new ParameterizedSparqlString();
        sparql.append(queryStr);

        //Prepare to execute the query against the given dataset
        Query query = sparql.asQuery();
        String endpointURL = fuseki().getHostname() + ":"
                + fuseki().getPort()
                + "/" + datasetUUID;
        return QueryExecutionFactory.sparqlService(endpointURL, query);
    }

    /**
     * Returns modified input JSON-LD with `@graph` at top-level.
     *
     * @param jsonldNode  JSON-LD to modify if it has @graph
     * @return            Modified JSON-LD
    */
    private JsonNode formatGraphNode(JsonNode jsonldNode)
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
            jsonldNode = objectReader.readValue(graphNode);
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
        LOGGER.info("Updating @base in @context block...");

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
     * @param datasetUUID UUID of the Apache Jena Dataset this model belongs to
     * @param modelUUID   UUID of output model
     * @param dataset     Bats DataSet this model will belong to
     * @param priorCreatedTime get value from prior model if updating, null if creating
     * @return            BatsModel of the JSON-LD
    */
    private BatsModel jsonldToBatsModel(
        final JsonNode jsonldNode,
        final String datasetUUID,
        final String modelUUID,
        final DataSet dataset,
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
        String scidataString = addBaseToContextToJsonLD(
            scidataNode.toString(),
            getModelUri(datasetUUID, modelUUID)
        );

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
            dataset.updateModel(modelUUID, model);
            LOGGER.info("Model uploaded!");
        } catch (Exception e) {
            LOGGER.error(UPLOAD_MODEL_ERROR, e);
        }

        Model newModel = dataset.getModel(modelUUID);
        return new BatsModel(modelUUID, RdfModelWriter.model2jsonld(newModel));
    }

    /**
     * Gets model data based on SPARQL query results.
     *
     * @param queryResults Results from previous SPARQL query
     * @return             List of Maps for model data
    */

    private List<Map<String, Object>> getModels(
        final ResultSet queryResults
    ) {
        List<Map<String, Object>> body = new ArrayList<>();
        while (queryResults.hasNext()) {
            QuerySolution solution = queryResults.next();
            Map<String, Object> map = new HashMap<String, Object>(); //NOPMD
            map.put("uuid", solution.get("?model").toString());
            map.put("title", solution.get("?title").toString());
            map.put("url", solution.get("?scidata_url").toString());
            map.put("created", solution.get("?created").toString());
            map.put("modified", solution.get("?modified").toString());
            body.add(map);
        }
        return body;
    }

    /**
     * Gets full models from dataset based on SPARQL query results.
     *
     * @param queryResults Results from previous SPARQL query
     * @param datasetUUID  UUID of the Apache Jena Dataset the models belong to
     * @return             List of BatsModel for the full models
    */
    private List<BatsModel> getFullModels(
        final String datasetUUID,
        final ResultSet queryResults
    ) {
        List<BatsModel> body = new ArrayList<>();
        DataSet dataset = initDataset(datasetUUID);
        while (queryResults.hasNext()) {
            QuerySolution solution = queryResults.next();
            RDFNode node = solution.get("?model");
            Model model = dataset.getModel(node.toString());
            try {
                body.add(
                    new BatsModel(//NOPMD
                        node.toString(),
                        RdfModelWriter.model2jsonld(model)
                    )
                );
            } catch (IOException e) {
                LOGGER.error(
                    "Unable to parse JSONLD from model {} dataset {}",
                    node.toString(),
                    datasetUUID
                );
            }
        }
        return body;
    }


    /**
     * FETCH a certain amount of datasets.
     *
     * @param datasetUUID UUID of the Apache Jena Dataset this model belongs to
     * @param pageNumber page number to start on,
     *    must be positive (default: 1)
     * @param pageSize number of results to return,
     *    must be positive (default: 5)
     * @param returnFull boolean for returning full model or not
     * @return List either BatsModels (full) or List of Map (not full)
     */
    @RequestMapping(
        value = "/{dataset_uuid}/models",
        method = RequestMethod.GET)
    public ResponseEntity<?> queryModels(
        @PathVariable("dataset_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String datasetUUID,
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
        String queryString =
            "PREFIX dcterm: <http://purl.org/dc/terms/>"
            + "PREFIX dcterms: <https://purl.org/dc/terms/>"
            + "PREFIX obo: <http://purl.obolibrary.org/obo/>"
            + "PREFIX prov: <http://www.w3.org/ns/prov#>"
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
            + "PREFIX rdfs: <https://www.w3.org/2000/01/rdf-schema#>"
            + "PREFIX sdo: <https://stuchalk.github.io/scidata/ontology/scidata.owl#>"
            + "PREFIX url: <http://schema.org/>"
            + "PREFIX urls: <https://schema.org/>"
            + "PREFIX xml: <http://www.w3.org/2001/XMLSchema#>"
            + "PREFIX xmls: <https://www.w3.org/2001/XMLSchema#>"
            + "SELECT ?model ?title ?scidata_url ?modified ?created "
            + "WHERE { "
            + "  GRAPH ?model { "
            + "    ?node1 dcterms:title ?_title . "
            + "    ?node2 dcterm:modified ?modified . "
            + "    ?node3 dcterm:created ?created . "
            + "    ?scidata_url rdf:type sdo:scidataFramework . "
            + "  } "
            + "  BIND( xml:string(?_title) as ?title)"
            + "}"
            + "ORDER BY DESC(?modified) "
            + "OFFSET " + (pageNumber * pageSize - pageSize) + " "
            + "LIMIT " + pageSize;

        QueryExecution execution = prepareModelUUIDQuery(//NOPMD
            datasetUUID, queryString
        );

        // immediately return 200 if the query was not valid
        ResultSet results;
        try {
            results = execution.execSelect();
        } catch (QueryException ex) {
            execution.close();
            return ResponseEntity.ok(Collections.EMPTY_LIST);
        }

        //Add each found model to the response
        if (returnFull) {
            List<BatsModel> body = getFullModels(datasetUUID, results);
            execution.close();
            return ResponseEntity.ok(body);
        } else {
            List<Map<String, Object>> body = getModels(results);
            execution.close();
            return ResponseEntity.ok(body);
        }

    }

    /**
     * CREATE a new Model in the Dataset collection.
     *
     * @param datasetUUID UUID for Dataset collection to add the new Model
     * @param jsonPayload JSON-LD of new Model
     * @return            BatsModel for created Model in the Dataset
    */
    @RequestMapping(
        value = "/{dataset_uuid}/models",
        method = RequestMethod.POST
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public BatsModel createModel(
        @PathVariable("dataset_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String datasetUUID,
        @RequestBody final String jsonPayload
    ) throws
        IOException,
        NoSuchAlgorithmException,
        UnsupportedEncodingException {

        // Initialize dataset
        DataSet dataset = initDataset(datasetUUID);

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

        return jsonldToBatsModel(jsonldNode, datasetUUID, modelUUID, dataset, null);
    }

    /**
     * READ Model w/ given UUID in Dataset collection w/ given UUID.
     *
     * @param datasetUUID UUID for Dataset collection that Model belonds to
     * @param modelUUID   UUID for Model to retrieve from the Dataset
     * @return            BatsModel for given Model UUID
    */
    @RequestMapping(
        value = "/{dataset_uuid}/models/{model_uuid}",
        method = RequestMethod.GET
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BatsModel getModel(
        @PathVariable("dataset_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String datasetUUID,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID
    ) {
        // Initialize dataset
        DataSet dataset = initDataset(datasetUUID);

        // Check if dataset exists
        DatasetUtils.checkDataSetExists(dataset, fuseki(), LOGGER);

        // Get the dataset's model
        LOGGER.info("Pulling model: " + modelUUID);
        try {
            Model model = dataset.getModel(modelUUID);
            return new BatsModel(modelUUID, RdfModelWriter.model2jsonld(model));
        } catch (Exception e) {
            LOGGER.error(READ_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Model " + modelUUID + " Not Found"
            );
        }
    }

    /**
     * READ A list of all UUIDs for models belonging to the given dataset.
     *
     * @param datasetUUID The UUID of the dataset to find models for.
     * @return A JSON list of all UUIDs
     */
    @RequestMapping(
        value = "/{dataset_uuid}/models/uuids",
        method = RequestMethod.GET
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> getUUIDs(@PathVariable("dataset_uuid")
        @Pattern(regexp = UUIDGenerator.UUID_REGEX) final String datasetUUID) {

        // pmd does not recognize that this is always being closed
        QueryExecution execution = prepareModelUUIDQuery(datasetUUID, // NOPMD
            "SELECT DISTINCT ?model {GRAPH ?model { ?x ?y ?z }}"
        );

        // immediately return 200 if the query was not valid
        ResultSet results;
        try {
            results = execution.execSelect();
        } catch (QueryException ex) {
            execution.close();
            return ResponseEntity.ok(Collections.EMPTY_LIST);
        }

        //The JSON response being built
        ArrayNode response = new ArrayNode(new JsonNodeFactory(false));

        //Add each found model to the response
        while (results.hasNext()) {
            QuerySolution solution = results.next();
            RDFNode node = solution.get("?model");
            response.add(node.toString());
        }
        execution.close();

        try {
            //Return the JSON representation
            return ResponseEntity.ok(MAPPER.writeValueAsString(response));
        } catch (JsonProcessingException e) {
            LOGGER.error(READ_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                READ_MODEL_ERROR
            );
        }
    }

    /**
     * UPDATE (REPLACE) for Model w/ UUID in Dataset collection w/ UUID.
     *
     * @param datasetUUID UUID for Dataset collection that Model belonds to
     * @param modelUUID   UUID for Model to replace
     * @param jsonPayload JSON-LD of new Model to replace current Model
     * @return            BatsModel for newly updated Model
    */
    @RequestMapping(
        value = "/{dataset_uuid}/models/{model_uuid}",
        method = RequestMethod.PUT
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BatsModel updateModelReplace(
        @PathVariable("dataset_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String datasetUUID,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID,
        @RequestBody final String jsonPayload
    ) throws
        IOException,
        NoSuchAlgorithmException,
        UnsupportedEncodingException {

        // Initialize dataset
        DataSet dataset = initDataset(datasetUUID);

        // Check if dataset exists
        DatasetUtils.checkDataSetExists(dataset, fuseki(), LOGGER);

        // JSON -> Tree
        LOGGER.info("updateModelReplace: Extracting JSON-LD -> model");
        JsonNode jsonldNode = MAPPER.readTree(jsonPayload);

        /*
        Get the dataset's model. We want to extract the created timestamp,
        instead of updating it from user params or deleting it.
        */
        LOGGER.info("Pulling model: " + modelUUID);
        String modelJSONLD;
        try {
            Model model = dataset.getModel(modelUUID);
            modelJSONLD = RdfModelWriter.model2jsonld(model);
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

        return jsonldToBatsModel(jsonldNode, datasetUUID, modelUUID,
            dataset, createdTimeNode.textValue());
    }

    /**
     * UPDATE (PARTIAL) for Model w/ UUID in Dataset collection w/ UUID.
     *
     * @param datasetUUID UUID for Dataset collection that Model belonds to
     * @param modelUUID   UUID for Model to partially update
     * @param jsonPayload Partial JSON-LD of new Model to update current Model
     * @return            BatsModel for newly updated Model
    */
    @RequestMapping(
        value = "/{dataset_uuid}/models/{model_uuid}",
        method = RequestMethod.PATCH
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BatsModel updateModelPartial(
        @PathVariable("dataset_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String datasetUUID,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID,
        @RequestBody final String jsonPayload
    ) throws IOException, NoSuchAlgorithmException {
        // Initialize dataset
        DataSet dataset = initDataset(datasetUUID);

        // Check if dataset exists
        DatasetUtils.checkDataSetExists(dataset, fuseki(), LOGGER);

        // Get the dataset's model
        LOGGER.info("Pulling model: " + modelUUID);
        String modelJSONLD;
        try {
            Model model = dataset.getModel(modelUUID);
            modelJSONLD = RdfModelWriter.model2jsonld(model);
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

        return jsonldToBatsModel(mergedModelNode, datasetUUID, modelUUID,
            dataset, createdTimeNode.textValue());
    }

    /**
     * DELETE Model w/ given UUID in Dataset collection.
     *
     * @param datasetUUID UUID that Model belongs to
     * @param modelUUID   UUID of Model to delete from Dataset
    */
    @RequestMapping(
        value = "/{dataset_uuid}/models/{model_uuid}",
        method = RequestMethod.DELETE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModel(
        @PathVariable("dataset_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String datasetUUID,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID
    ) {
        // Initialize dataset
        DataSet dataset = initDataset(datasetUUID);

        // Check if dataset exists
        DatasetUtils.checkDataSetExists(dataset, fuseki(), LOGGER);

        // Delete the dataset's model
        LOGGER.info("Deleting model: " + modelUUID);
        try {
            dataset.deleteModel(modelUUID);
        } catch (Exception e) {
            LOGGER.error(DELETE_MODEL_ERROR, e);
        }
    }
}
