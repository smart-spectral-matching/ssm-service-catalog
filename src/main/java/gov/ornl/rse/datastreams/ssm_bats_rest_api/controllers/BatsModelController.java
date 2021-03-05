package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.ornl.rse.bats.DataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.RdfModelWriter;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.UUIDGenerator;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ConfigUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.Fuseki;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsModel;

@RestController
@RequestMapping("/datasets")
public class BatsModelController {

    /**
     * Setup logger for BatsDatasetController.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        BatsModelController.class
    );

    /**
     * Setup Fuseki config.
    */
    @Autowired
    private Fuseki fusekiConfig;

    /**
     * Get API information.
    */
    @Autowired
    private ConfigUtils configUtils;

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
     * Return if given Apache Jena Dataset exists in Fuseki database.
     *
     * @param dataset Dataset to check for existence in Fuseki database
     * @return        Boolean; true if exists, false otherwise
    */
    private boolean doesDataSetExist(final DataSet dataset) {
        LOGGER.info("Pulling dataset: " + dataset.getName());
        Dataset contents = dataset.getJenaDataset();
        if (contents == null) {
            LOGGER.info("Dataset " + dataset.getName() + " NOT FOUND!");
            return false;
        } else {
            LOGGER.info("Dataset " + dataset.getName() + " exists!");
            return true;
        }
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
     * Returns modified input JSON-LD with `@graph` at top-level.
     *
     * @param jsonldNode  JSON-LD to modify if it has @graph
     * @return            Modified JSON-LD
    */
    private JsonNode formatGraphNode(final JsonNode jsonldNode)
    throws IOException {
        ObjectNode newJsonldNode = (ObjectNode) jsonldNode.deepCopy();

        LOGGER.info("Checking for @graph in model...");
        JsonNode isGraphNode = newJsonldNode.get("@graph");
        if (isGraphNode != null) {

            // Merge @graph node into top-level and remove duplicate @id node
            LOGGER.info("Moving @graph to top-level of model...");
            JsonNode graphNode = newJsonldNode.remove("@graph");
            newJsonldNode.remove("@id");

            ObjectMapper mapper = new ObjectMapper();
            ObjectReader objectReader = mapper.readerForUpdating(
                newJsonldNode
            );
            newJsonldNode = objectReader.readValue(graphNode);
        }
        return (JsonNode) newJsonldNode;
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
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonldNode = mapper.readValue(jsonld, ObjectNode.class);
        JsonNode contextNode = jsonldNode.get("@context");

        // If @context is array, replace/add @base entry with input base uri
        if (contextNode.isArray()) {

            // Re-create @context block while leaving out pre-existing @base
            ArrayNode newContextNode = mapper.createArrayNode();
            for (final JsonNode elementNode: contextNode) {
                if (!elementNode.has("@base")) {
                    newContextNode.add(elementNode);
                }
            }

            // Add new @base to @context block
            ObjectNode baseContext = mapper.createObjectNode();
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
     * @return            BatsModel of the JSON-LD
    */
    private BatsModel jsonldToBatsModel(
        final JsonNode jsonldNode,
        final String datasetUUID,
        final String modelUUID,
        final DataSet dataset
    ) throws
        IOException,
        NoSuchAlgorithmException,
        UnsupportedEncodingException {
        // Check if we have a @graph node, need to move all fields to top-level
        JsonNode scidataNode = formatGraphNode(jsonldNode);

        // Replace @base in @context block w/ new URI
        String scidataString = addBaseToContextToJsonLD(
            scidataNode.toString(),
            getModelUri(datasetUUID, modelUUID)
        );

        // Tree -> JSON -> Jena Model
        LOGGER.info("Uploading model: " + modelUUID);
        StringReader reader = new StringReader(scidataString);
        Model model = ModelFactory.createDefaultModel();
        model.read(reader, null, "JSON-LD");

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
        @PathVariable("dataset_uuid") final String datasetUUID,
        @RequestBody final String jsonPayload
    ) throws
        IOException,
        NoSuchAlgorithmException,
        UnsupportedEncodingException {

        // Initialize dataset
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHostname());
        dataset.setPort(fusekiConfig.getPort());

        // Check if dataset exists
        if (!doesDataSetExist(dataset)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " NOT FOUND!");
        }

        // Create Model UUID
        String modelUUID = UUIDGenerator.generateUUID();

        // JSON -> Tree
        LOGGER.info("createModel: Extracting JSON-LD -> model");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonldNode = mapper.readValue(
            jsonPayload,
            JsonNode.class
        );

        return jsonldToBatsModel(jsonldNode, datasetUUID, modelUUID, dataset);
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
        @PathVariable("dataset_uuid") final String datasetUUID,
        @PathVariable("model_uuid") final String modelUUID
    ) {
        // Initialize dataset
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHostname());
        dataset.setPort(fusekiConfig.getPort());

        // Check if dataset exists
        if (!doesDataSetExist(dataset)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " NOT FOUND!");
        }

        // Get the dataset's model
        LOGGER.info("Pulling model: " + modelUUID);
        try {
            Model model = dataset.getModel(modelUUID);
            return new BatsModel(modelUUID, RdfModelWriter.model2jsonld(model));
        } catch (Exception e) {
            LOGGER.error(READ_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Model Not Found"
            );
        }
    }

    /**
     * READ A list of all UUIDs for models belonging to the given dataset.
     *
     * @param datasetUUID The UUID of the dataset to find models for.
     * @return A JSON list of all UUIDs
     */
    @RequestMapping("/{dataset_uuid}/models/uuids")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String getUUIDs(@PathVariable("dataset_uuid")
            final String datasetUUID) {
        //SPARQL query to find all unique graphs
        ParameterizedSparqlString sparql = new ParameterizedSparqlString();
        sparql.append("SELECT DISTINCT ?model");
        sparql.append(" {");
        sparql.append("GRAPH ?model { ?x ?y ?z }");
        sparql.append("}");

        //Execute the query against the given dataset
        Query query = sparql.asQuery();
        String endpointURL = fusekiConfig.getHostname() + ":"
                + fusekiConfig.getPort()
                + "/" + datasetUUID;
        QueryExecution execution =
                QueryExecutionFactory.sparqlService(endpointURL, query);
        ResultSet results = execution.execSelect();

        //The JSON response being built
        ArrayNode response = new ArrayNode(new JsonNodeFactory(false));

        //Add each found model to the response
        while (results.hasNext()) {
            QuerySolution solution = results.next();
            RDFNode node = solution.get("?model");
            response.add(node.toString());
        }

        try {
            //Return the JSON representation
            return new ObjectMapper().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            //TODO Create error message scheme rather than empty JSON
            return "{}";
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
        @PathVariable("dataset_uuid") final String datasetUUID,
        @PathVariable("model_uuid") final String modelUUID,
        @RequestBody final String jsonPayload
    ) throws
        IOException,
        NoSuchAlgorithmException,
        UnsupportedEncodingException {

        // Initialize dataset
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHostname());
        dataset.setPort(fusekiConfig.getPort());

        // Check if dataset exists
        if (!doesDataSetExist(dataset)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " NOT FOUND!");
        }

        // JSON -> Tree
        LOGGER.info("updateModelReplace: Extracting JSON-LD -> model");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonldNode = mapper.readTree(jsonPayload);

        return jsonldToBatsModel(jsonldNode, datasetUUID, modelUUID, dataset);
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
        @PathVariable("dataset_uuid") final String datasetUUID,
        @PathVariable("model_uuid") final String modelUUID,
        @RequestBody final String jsonPayload
    ) throws IOException {
        // Initialize dataset
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHostname());
        dataset.setPort(fusekiConfig.getPort());

        // Check if dataset exists
        if (!doesDataSetExist(dataset)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " NOT FOUND!");
        }

        // Get the dataset's model
        LOGGER.info("Pulling model: " + modelUUID);
        String modelJSONLD = new String();
        try {
            Model model = dataset.getModel(modelUUID);
            modelJSONLD = RdfModelWriter.model2jsonld(model);
        } catch (Exception e) {
            LOGGER.error(READ_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Model Not Found"
            );
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode modelNode = mapper.readTree(modelJSONLD);
        LOGGER.info("Pulled model: " + modelUUID);

        LOGGER.info("updateModelPartial: Extracting JSON-LD from body data");
        // JSON -> Tree
        JsonNode payloadNode = mapper.readTree(jsonPayload);

        // Merge payload with model
        JsonNode mergedModelNode = mapper.readerForUpdating(modelNode)
                                         .readValue(payloadNode);

        // Check if we have a @graph node, need to move all fields to top-level
        JsonNode scidataNode = formatGraphNode(mergedModelNode);

        // Replace @base in @context block w/ new URI
        String scidataString = addBaseToContextToJsonLD(
            scidataNode.toString(),
            getModelUri(datasetUUID, modelUUID)
        );

        // Merged Tree -> Merged JSON -> Jena Model
        StringReader reader = new StringReader(scidataString);
        Model mergedModel = ModelFactory.createDefaultModel();
        mergedModel.read(reader, null, "JSON-LD");

        // Upload merged model
        try {
            dataset.updateModel(modelUUID, mergedModel);
            LOGGER.info("Model udated and uploaded!");
        } catch (Exception e) {
            LOGGER.error(UPLOAD_MODEL_ERROR, e);
        }

        Model newModel = dataset.getModel(modelUUID);
        return new BatsModel(modelUUID, RdfModelWriter.model2jsonld(newModel));
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
        @PathVariable("dataset_uuid") final String datasetUUID,
        @PathVariable("model_uuid") final String modelUUID
    ) {
        // Initialize dataset
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHostname());
        dataset.setPort(fusekiConfig.getPort());

        // Check if dataset exists
        if (!doesDataSetExist(dataset)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " NOT FOUND!");
        }

        // Delete the dataset's model
        LOGGER.info("Deleting model: " + modelUUID);
        try {
            dataset.deleteModel(modelUUID);
        } catch (Exception e) {
            LOGGER.error(DELETE_MODEL_ERROR, e);
        }
    }
}
