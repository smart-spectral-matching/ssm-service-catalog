package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.io.StringReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.ornl.rse.bats.DataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.RdfModelWriter;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.UUIDGenerator;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.FusekiConfig;

@RestController
@RequestMapping("/datasets")
public class BatsModelController {
    private static final Logger logger = LoggerFactory.getLogger(BatsModelController.class);

    @Autowired
    private FusekiConfig fusekiConfig;

    private boolean doesDataSetExist(DataSet dataset) {
                // Get the dataset
        logger.info("Pulling dataset: " + dataset.getName());
        Dataset contents = dataset.getJenaDataset();
        if ( contents == null ) {
            logger.info("Dataset " + dataset.getName() + " NOT FOUND!");
            return false;
        } else {
            logger.info("Dataset " + dataset.getName() + " exists!");
            return true;
        }
    }

    // CREATE
    @RequestMapping(value = "/{dataset_uuid}/models", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public String createModel(
        @PathVariable("dataset_uuid") String datasetUUID,
        @RequestBody String jsonPayload
    ) throws Exception {
        // Initialize dataset
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHost());
        dataset.setPort(fusekiConfig.getPort());

        // Check if dataset exists
        if (! doesDataSetExist(dataset)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " NOT FOUND!");
        }

        logger.info("Extracting JSON-LD -> model");
        // JSON -> Tree
        ObjectMapper mapper = new ObjectMapper();
        JsonNode treeNode = mapper.readTree(jsonPayload);

        // Create Model UUID
        String modelUUID = UUIDGenerator.generateUUID();

        logger.info("Uploading model: " + modelUUID);
        // Tree -> JSON -> Jena Model
        StringReader reader = new StringReader(treeNode.toString());
        Model model = ModelFactory.createDefaultModel().read(reader, null, "JSON-LD");

        // Jena Model -> BATS DataSet
        try {
            dataset.updateModel(modelUUID, model);
            logger.info("Model uploaded!");
        } catch (Exception e) {
            logger.error("Unable to upload model on the remote Fuseki server.", e);
        }
        return modelUUID;
    }

    // READ
    @RequestMapping(value = "/{dataset_uuid}/models/{model_uuid}", method = RequestMethod.GET)
    public String getModel(
        @PathVariable("dataset_uuid") String datasetUUID,
        @PathVariable("model_uuid") String modelUUID
    ) {
        // Initialize dataset
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHost());
        dataset.setPort(fusekiConfig.getPort());

        // Check if dataset exists
        if (! doesDataSetExist(dataset)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " NOT FOUND!");
        }

        // Get the dataset's model
        logger.info("Pulling model: " + modelUUID);
        String jsonld = new String();
        try {
            Model model = dataset.getModel(modelUUID);
            jsonld = RdfModelWriter.model2jsonld(model);
        } catch (Exception e) {
            logger.error("Unable to get model on the remote Fuseki server.", e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Model Not Found");
        }
        return jsonld;
    }

    // UPDATE (replace)
    //@RequestMapping(value = "/{dataset_uuid}/models/{model_uuid}", method = RequestMethod.PUT)

    // UPDATE (partial)
    @RequestMapping(value = "/{dataset_uuid}/models/{model_uuid}", method = RequestMethod.PATCH)
    @ResponseStatus(HttpStatus.OK)
    public String updateModelPartial(
        @PathVariable("dataset_uuid") String datasetUUID,
        @PathVariable("model_uuid") String modelUUID,
        @RequestBody String jsonPayload
    ) throws IOException {
        // Initialize dataset
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHost());
        dataset.setPort(fusekiConfig.getPort());

        // Check if dataset exists
        if (! doesDataSetExist(dataset)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " NOT FOUND!");
        }

        // Get the dataset's model
        logger.info("Pulling model: " + modelUUID);
        String modelJSONLD = new String();
        try {
            Model model = dataset.getModel(modelUUID);
            modelJSONLD = RdfModelWriter.model2jsonld(model);
        } catch (Exception e) {
            logger.error("Unable to get model on the remote Fuseki server.", e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Model Not Found");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode modelNode = mapper.readTree(modelJSONLD);
        logger.info("Pulled model: " + modelUUID);

        logger.info("Extracting JSON-LD from body data");
        // JSON -> Tree
        JsonNode payloadNode = mapper.readTree(jsonPayload);

        // Merge payload with model
        JsonNode mergedModelNode = mapper.readerForUpdating(modelNode).readValue(payloadNode);

        // Merged Tree -> Merged JSON -> Jena Model
        StringReader reader = new StringReader(mergedModelNode.toString());
        Model mergedModel = ModelFactory.createDefaultModel().read(reader, null, "JSON-LD");

        // Upload merged model
        try {
            dataset.updateModel(modelUUID, mergedModel);
            logger.info("Model udated and uploaded!");
        } catch (Exception e) {
            logger.error("Unable to upload model on the remote Fuseki server.", e);
        }

        return RdfModelWriter.model2jsonld(mergedModel);

    }

    // DELETE
    @RequestMapping(value = "/{dataset_uuid}/models/{model_uuid}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModel(
        @PathVariable("dataset_uuid") String datasetUUID,
        @PathVariable("model_uuid") String modelUUID
    ) {
        // Initialize dataset
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHost());
        dataset.setPort(fusekiConfig.getPort());

        // Check if dataset exists
        if (! doesDataSetExist(dataset)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " NOT FOUND!");
        }

        // Delete the dataset's model
        logger.info("Deleting model: " + modelUUID);
        String jsonld = new String();
        try {
            dataset.deleteModel(modelUUID);
        } catch (Exception e) {
            logger.error("Unable to delete model on the remote Fuseki server.", e);
        }
    }
}