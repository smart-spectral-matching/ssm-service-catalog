package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDataset;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsModel;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsModelFormats;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.services.DocumentService;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.services.GraphService;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.UUIDGenerator;

@RestController
@RequestMapping("/datasets")
@Validated
public class ModelController {

    /**
     * Setup logger for ModelController.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        ModelController.class
    );

    /**
     * Graph service to retrieve JSON-LD.
     */
    @Autowired
    private GraphService graphService;

    /**
     * Document service to retrieve original JSON-LD and JSON.
     */
    @Autowired
    private DocumentService documentService;

    /**
     * Error message for uploading model.
    */
    private static final String UPLOAD_MODEL_ERROR =
        "Unable to upload model.";

    /**
     * Error message for reading model.
    */
    private static final String READ_MODEL_ERROR =
        "Unable to read model.";

    /**
     * Error message for deleting model.
    */
    private static final String DELETE_MODEL_ERROR =
        "Unable to delete model.";

    /**
     * FETCH a certain amount of Models for a Dataset.
     *
     * @param datasetTitle Title of the Dataset this model belongs to
     * @param pageNumber page number to start on,
     *    must be positive (default: 1)
     * @param pageSize number of results to return,
     *    must be positive (default: 5)
     * @param returnFull boolean for returning full model or not
     * @return List either BatsModels (full) or List of Map (not full)
     */
    @RequestMapping(
        value = "/{dataset_title}/models",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
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
        Map<String, Object> body = graphService.getModels(
            datasetTitle,
            pageNumber,
            pageSize,
            returnFull
        );
        return ResponseEntity.ok(body);
    }

    /**
     * CREATE a new Model in the Dataset collection.
     *
     * @param datasetTitle Title for Dataset collection to add the new Model
     * @param jsonldPayload JSON-LD of new Model
     * @return            BatsModel for created Model in the Dataset
     * @throws Exception
    */
    @RequestMapping(
        value = "/{dataset_title}/models",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ResponseEntity<?> createModel(
        @PathVariable("dataset_title") @Pattern(regexp = BatsDataset.TITLE_REGEX)
        final String datasetTitle,
        @RequestBody final String jsonldPayload
    ) throws
        Exception {

        String modelUUID = UUIDGenerator.generateUUID();

        // Create in the graph database
        try {
            graphService.uploadJsonld(datasetTitle, modelUUID, jsonldPayload);
        } catch (Exception e) {
            LOGGER.error(UPLOAD_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Model unable to be uploaded to graph database"
            );
        }

        // Create in the document database w/ rollback of graph database on error
        LOGGER.info("Uploading model to document store: " + modelUUID);
        try {
            documentService.upload(datasetTitle, modelUUID, jsonldPayload);
            LOGGER.info("Model uploaded to document store!");
        } catch (Exception e) {
            // Rollback graph database insert of model
            LOGGER.error("Unable to create model in document store: " + modelUUID);
            LOGGER.error("Rolling back create from graph database for model: " + modelUUID);
            LOGGER.error(UPLOAD_MODEL_ERROR, e);

            graphService.delete(datasetTitle, modelUUID);

            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Model unable to be uploaded to document store"
            );
        }

        String jsonld = graphService.getModelJsonld(datasetTitle, modelUUID);
        BatsModel batsModel = new BatsModel(modelUUID, jsonld);
        return new ResponseEntity<BatsModel>(batsModel, HttpStatus.CREATED);
    }

    /**
     * READ Model w/ given UUID in Dataset collection.
     *
     * @param datasetTitle Title for Dataset collection that Model belonds to
     * @param modelUUID    UUID for Model to retrieve from the Dataset
     * @param format       Format to return the model ["graph", "json", "jsonld"]
     * @return             Requested format of Model UUID
     * @throws Exception
    */
    @RequestMapping(
        value = "/{dataset_title}/models/{model_uuid}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> getModel(
        @PathVariable("dataset_title") @Pattern(regexp = BatsDataset.TITLE_REGEX)
        final String datasetTitle,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID,
        @RequestParam(name = "format", defaultValue = "json")
        final BatsModelFormats format
    ) throws Exception {
        try {
            documentService.getJson(modelUUID);
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Model " + modelUUID + " Not Found"
            );
        }
        String output;
        if (format == BatsModelFormats.GRAPH || format == BatsModelFormats.FULL) {
            String jsonld = graphService.getModelJsonld(datasetTitle, modelUUID);
            BatsModel batsModel = new BatsModel(modelUUID, jsonld);
            return ResponseEntity.ok(batsModel);
        }

        if (format == BatsModelFormats.JSONLD) {
            output = documentService.getJsonld(modelUUID);
        } else {
            output = documentService.getJson(modelUUID);
        }
        return ResponseEntity.ok(output);
    }

    /**
     * READ A list of all UUIDs for models belonging to the given dataset.
     *
     * @param datasetTitle Title of the dataset to find models for.
     * @return A JSON list of all UUIDs
     */
    @RequestMapping(
        value = "/{dataset_title}/models/uuids",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> getUUIDs(@PathVariable("dataset_title")
        @Pattern(regexp = BatsDataset.TITLE_REGEX) final String datasetTitle
    ) {
        try {
            String uuids = graphService.getModelUUIDsForDataset(datasetTitle);
            return ResponseEntity.ok(uuids);
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
     * @param datasetTitle  Title for Dataset collection that Model belonds to
     * @param modelUUID     UUID for Model to replace
     * @param jsonldPayload JSON-LD of new Model to replace current Model
     * @return              BatsModel for newly updated Model
     * @throws Exception
    */
    @RequestMapping(
        value = "/{dataset_title}/models/{model_uuid}",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> updateModelReplace(
        @PathVariable("dataset_title") @Pattern(regexp = BatsDataset.TITLE_REGEX)
        final String datasetTitle,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID,
        @RequestBody final String jsonldPayload
    ) throws
        Exception {

        // Cache old data for rollback
        LOGGER.info("Getting rollback json-ld for update");
        String oldJsonld = documentService.getJsonld(modelUUID);

        // Extract created timestamp
        String createdTime = graphService.getCreatedTimeForModel(datasetTitle, modelUUID);

        // Update graph database model
        LOGGER.info("Uploading model to graph database: " + modelUUID);
        graphService.uploadJsonld(datasetTitle, modelUUID, jsonldPayload, createdTime);

        // Add updated model to document store
        LOGGER.info("Uploading model to document store: " + modelUUID);
        try {
            documentService.upload(datasetTitle, modelUUID, jsonldPayload);
            LOGGER.info("Model uploaded to document store!");
        } catch (Exception e) {
            graphService.uploadJsonld(datasetTitle, modelUUID, oldJsonld, createdTime);
            LOGGER.error(UPLOAD_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Model unable to be uploaded to document store"
            );
        }

        String jsonld = graphService.getModelJsonld(datasetTitle, modelUUID);
        BatsModel batsModel = new BatsModel(modelUUID, jsonld);
        return ResponseEntity.ok(batsModel);
    }

    /**
     * UPDATE (PARTIAL) for Model w/ UUID in Dataset collection.
     *
     * @param datasetTitle Title for Dataset collection that Model belonds to
     * @param modelUUID   UUID for Model to partially update
     * @param jsonPayload Partial JSON-LD of new Model to update current Model
     * @return            BatsModel for newly updated Model
     * @throws Exception
    */
    @RequestMapping(
        value = "/{dataset_title}/models/{model_uuid}",
        method = RequestMethod.PATCH,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> updateModelPartial(
        @PathVariable("dataset_title") @Pattern(regexp = BatsDataset.TITLE_REGEX)
        final String datasetTitle,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID,
        @RequestBody final String jsonPayload
    ) throws Exception {

        // Cache old data for rollback
        LOGGER.info("Getting rollback json-ld...");
        String oldJsonld = documentService.getJsonld(modelUUID);

        // Get merged JSON-LD from Model and new JSON-LD in request
        LOGGER.info("Merging json-ld w/ graph model json-ld: " + modelUUID);
        String mergedGraphJsonld = graphService.mergeJsonldForModel(
            datasetTitle,
            modelUUID,
            jsonPayload
        );

        LOGGER.info("Merging json-ld w/ document json-ld: " + modelUUID);
        String mergedDocumentJsonld = documentService.mergeJsonldForModel(
            modelUUID,
            jsonPayload
        );

        // Extract created timestamp
        String createdTime = graphService.getCreatedTimeForModel(datasetTitle, modelUUID);

        // Update graph database model
        LOGGER.info("Uploading model to graph database: " + modelUUID);
        graphService.uploadJsonld(datasetTitle, modelUUID, mergedGraphJsonld, createdTime);

        // Add updated model to document store
        LOGGER.info("Uploading model to document store: " + modelUUID);
        try {
            documentService.upload(datasetTitle, modelUUID, mergedDocumentJsonld);
            LOGGER.info("Model uploaded to document store!");
        } catch (Exception e) {
            graphService.uploadJsonld(datasetTitle, modelUUID, oldJsonld, createdTime);
            LOGGER.error(UPLOAD_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Model unable to be uploaded to document store"
            );
        }

        String jsonld = graphService.getModelJsonld(datasetTitle, modelUUID);
        BatsModel batsModel = new BatsModel(modelUUID, jsonld);
        return ResponseEntity.ok(batsModel);
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
    ) throws IOException, NoSuchAlgorithmException {
        // Cache old data for rollback
        LOGGER.info("Getting rollback json-ld...");
        String oldJsonld = documentService.getJsonld(modelUUID);

        // Delete model from graph DB
        LOGGER.info("Deleting model: " + modelUUID + " from graph database");
        try {
            graphService.delete(datasetTitle, modelUUID);
        } catch (Exception e) {
            LOGGER.error(DELETE_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Model unable to be deleted from graph database"
            );
        }

        // Delete model from document store
        LOGGER.info("Deleting model: " + modelUUID + " from document store");
        try {
            documentService.delete(modelUUID);
        } catch (Exception e) {
            // Rolling back graph database deletion of model
            LOGGER.error("Unable to delete model in document store: " + modelUUID);
            LOGGER.error("Rolling back delete from graph database for model: " + modelUUID);

            graphService.uploadJsonld(datasetTitle, modelUUID, oldJsonld);

            LOGGER.error(DELETE_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Model unable to be deleted from document database"
            );
        }
    }
}
