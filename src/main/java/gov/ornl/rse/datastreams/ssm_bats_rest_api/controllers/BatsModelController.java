package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

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

    // GET
    @RequestMapping(value = "/{dataset_uuid}/models/{model_uuid}", method = RequestMethod.GET)
    public String getDataSetById(
        @PathVariable("dataset_uuid") String datasetUUID,
        @PathVariable("model_uuid") String modelUUID
    ) {
        // Get the dataset
        logger.info("Pulling dataset: " + datasetUUID);
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHost());
        dataset.setPort(fusekiConfig.getPort());

        Dataset contents = dataset.getJenaDataset();
        if ( contents == null ) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DataSet Not Found");
        }
        logger.info("Dataset " + datasetUUID + " exists!");

        // Get the dataset's model
        logger.info("Pulling model: " + modelUUID);
        String jsonld = new String();
        try {
            Model model = dataset.getModel(modelUUID);
            jsonld = RdfModelWriter.model2jsonld(model);
        } catch (Exception e) {
            logger.error("Unable to get model on the remote Fuseki server.", e);
        }
        return jsonld;
    }

    // POST
    @RequestMapping(value = "/{dataset_uuid}/models", method = RequestMethod.POST)
    public String createDataset(
        @PathVariable("dataset_uuid") String datasetUUID,
        @RequestBody String jsonPayload
    ) throws Exception {
        // Get the dataset
        logger.info("Pulling dataset: " + datasetUUID);
        DataSet dataset = new DataSet();
        dataset.setName(datasetUUID);
        dataset.setHost(fusekiConfig.getHost());
        dataset.setPort(fusekiConfig.getPort());

        Dataset contents = dataset.getJenaDataset();
        if ( contents == null ) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DataSet Not Found");
        }
        logger.info("Dataset " + datasetUUID + " exists!");

        logger.info("Extracting JSON-LD -> model");
        // JSON -> Tree
        ObjectMapper mapper = new ObjectMapper();
        JsonNode treeNode = mapper.readTree(jsonPayload);

        // add stuff to tree here
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
}