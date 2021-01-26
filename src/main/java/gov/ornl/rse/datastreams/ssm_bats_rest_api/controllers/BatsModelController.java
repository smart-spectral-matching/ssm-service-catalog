package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.io.StringReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.ornl.rse.bats.DataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.RdfModelWriter;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.UUIDGenerator;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.FusekiConfig;
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
    private FusekiConfig fusekiConfig;

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
    ) throws Exception {
        // Initialize dataset
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHost());
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
        LOGGER.info("Extracting JSON-LD -> model");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode scidataNode = mapper.readValue(
            jsonPayload,
            ObjectNode.class
        );

        // Check if we have a @graph node, need to move all fields to top-level
        LOGGER.info("Uploading model: " + modelUUID);
        JsonNode isGraphNode = scidataNode.get("@graph");
        if (isGraphNode != null) {
            // Merge @graph node into top-level and remove duplicate @id node
            JsonNode graphNode = scidataNode.remove("@graph");
            scidataNode.remove("@id");
            ObjectReader objectReader = mapper.readerForUpdating(scidataNode);
            scidataNode = objectReader.readValue(graphNode);
        }

        // Tree -> JSON -> Jena Model
        StringReader reader = new StringReader(scidataNode.toString());
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
        dataset.setHost(fusekiConfig.getHost());
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
    ) throws IOException {
        // Initialize dataset
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHost());
        dataset.setPort(fusekiConfig.getPort());

        // Check if dataset exists
        if (!doesDataSetExist(dataset)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " NOT FOUND!");
        }

        LOGGER.info("Extracting JSON-LD -> model");
        // JSON -> Tree
        ObjectMapper mapper = new ObjectMapper();
        JsonNode treeNode = mapper.readTree(jsonPayload);

        LOGGER.info("Uploading model: " + modelUUID);
        // Tree -> JSON -> Jena Model
        StringReader reader = new StringReader(treeNode.toString());
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
        dataset.setHost(fusekiConfig.getHost());
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

        LOGGER.info("Extracting JSON-LD from body data");
        // JSON -> Tree
        JsonNode payloadNode = mapper.readTree(jsonPayload);

        // Merge payload with model
        JsonNode mergedModelNode = mapper.readerForUpdating(modelNode)
                                         .readValue(payloadNode);

        // Merged Tree -> Merged JSON -> Jena Model
        StringReader reader = new StringReader(mergedModelNode.toString());
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
        dataset.setHost(fusekiConfig.getHost());
        dataset.setPort(fusekiConfig.getPort());

        // Check if dataset exists
        if (!doesDataSetExist(dataset)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " NOT FOUND!");
        }

        // Delete the dataset's model
        LOGGER.info("Deleting model: " + modelUUID);
        String jsonld = new String();
        try {
            dataset.deleteModel(modelUUID);
        } catch (Exception e) {
            LOGGER.error(DELETE_MODEL_ERROR, e);
        }
    }
}
