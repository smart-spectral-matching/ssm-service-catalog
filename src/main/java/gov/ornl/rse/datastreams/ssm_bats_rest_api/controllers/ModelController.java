package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
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

import gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization.AuthorizationHandler;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization.Permissions;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsCollection;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsModel;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsModelFormats;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.services.DocumentService;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.services.GraphService;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.AuthorizationUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.UUIDGenerator;

@RestController
@RequestMapping("/collections")
@Validated
public class ModelController {

    /**
     * Setup logger for ModelController.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        ModelController.class
    );

    /**
     * Configuration from properties.
     */
    @Autowired
    private ApplicationConfig appConfig;

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
     * FETCH a certain amount of Models for a Collection.
     *
     * @param collectionTitle Title of the Collection this model belongs to
     * @param pageNumber page number to start on,
     *    must be positive (default: 1)
     * @param pageSize number of results to return,
     *    must be positive (default: 5)
     * @param returnFull boolean for returning full model or not
     * @return List either BatsModels (full) or List of Map (not full)
     */
    @RequestMapping(
        value = "/{collection_title}/models",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> queryModels(
        @PathVariable("collection_title") @Pattern(regexp = BatsCollection.TITLE_REGEX)
        final String collectionTitle,
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
            collectionTitle,
            pageNumber,
            pageSize,
            returnFull
        );

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Skip authorization checking if authorization is not enabled or no user is
        // logged in.
        if (authHandler != null) {

            String user = AuthorizationUtils.getUser();

            if (user != null) {

                // Get list of all returned uuids
                ArrayList<String> uuids = new ArrayList<String>();

                for (String key : body.keySet()) {
                    uuids.add(key);
                }

                // Map of uuids to models the user has authorization to read
                HashMap<String, Object> authorizedModels = new HashMap<String, Object>();

                // Get each model that the user has access to and put it in the list
                for (String key : uuids) {
                    if (authHandler.checkPermission(user, Permissions.READ, key)) {
                        authorizedModels.put(key, body.get(key));
                    }
                }

                body = authorizedModels;

            }
        }

        return ResponseEntity.ok(body);
    }

    /**
     * CREATE a new Model in the Collection collection.
     *
     * @param collectionTitle Title for Collection collection to add the new Model
     * @param jsonldPayload JSON-LD of new Model
     * @return            BatsModel for created Model in the Collection
     * @throws Exception
    */
    @RequestMapping(
        value = "/{collection_title}/models",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ResponseEntity<?> createModel(
        @PathVariable("collection_title") @Pattern(regexp = BatsCollection.TITLE_REGEX)
        final String collectionTitle,
        @RequestBody final String jsonldPayload
    ) throws
        Exception {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Skip authorization checking if authorization is not enabled or no user is
        // logged in.
        if (authHandler != null) {

            String user = AuthorizationUtils.getUser();

            // If the user doesn't have permission to create new models, return an error.
            if (user != null && !authHandler.checkDatasetCreationPermission(user)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User " + user + " lacks permission to create a new model.");
            }
        }

        String modelUUID = UUIDGenerator.generateUUID();

        // Create in the graph database
        try {
            graphService.uploadJsonld(collectionTitle, modelUUID, jsonldPayload);
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
            documentService.upload(collectionTitle, modelUUID, jsonldPayload);
            LOGGER.info("Model uploaded to document store!");
        } catch (Exception e) {
            // Rollback graph database insert of model
            LOGGER.error("Unable to create model in document store: " + modelUUID);
            LOGGER.error("Rolling back create from graph database for model: " + modelUUID);
            LOGGER.error(UPLOAD_MODEL_ERROR, e);

            graphService.delete(collectionTitle, modelUUID);

            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Model unable to be uploaded to document store"
            );
        }

        String jsonld = graphService.getModelJsonld(collectionTitle, modelUUID);
        BatsModel batsModel = new BatsModel(modelUUID, jsonld);
        return new ResponseEntity<BatsModel>(batsModel, HttpStatus.CREATED);
    }

    /**
     * READ Model w/ given UUID in Collection collection.
     *
     * @param collectionTitle Title for Collection collection that Model belonds to
     * @param modelUUID    UUID for Model to retrieve from the Collection
     * @param format       Format to return the model ["graph", "json", "jsonld"]
     * @return             Requested format of Model UUID
     * @throws Exception
    */
    @RequestMapping(
        value = "/{collection_title}/models/{model_uuid}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> getModel(
        @PathVariable("collection_title") @Pattern(regexp = BatsCollection.TITLE_REGEX)
        final String collectionTitle,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID,
        @RequestParam(name = "format", defaultValue = "json")
        final BatsModelFormats format
    ) throws Exception {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Skip authorization checking if authorization is not enabled or no user is
        // logged in.
        if (authHandler != null) {

            String user = AuthorizationUtils.getUser();

            // If the user can't read the model, return an error message
            if (user != null && !authHandler.checkPermission(user, Permissions.READ, modelUUID)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User " + user + " lacks permission to READ model " + modelUUID);
            }
        }

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
            String jsonld = graphService.getModelJsonld(collectionTitle, modelUUID);
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
     * READ A list of all UUIDs for models belonging to the given collection.
     *
     * @param collectionTitle Title of the collection to find models for.
     * @return A JSON list of all UUIDs
     */
    @RequestMapping(
        value = "/{collection_title}/models/uuids",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> getUUIDs(@PathVariable("collection_title")
        @Pattern(regexp = BatsCollection.TITLE_REGEX) final String collectionTitle
    ) {
        try {
            String uuids = graphService.getModelUUIDsForCollection(collectionTitle);
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
     * UPDATE (REPLACE) for Model w/ UUID in Collection collection.
     *
     * @param collectionTitle  Title for Collection collection that Model belonds to
     * @param modelUUID     UUID for Model to replace
     * @param jsonldPayload JSON-LD of new Model to replace current Model
     * @return              BatsModel for newly updated Model
     * @throws Exception
    */
    @RequestMapping(
        value = "/{collection_title}/models/{model_uuid}",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> updateModelReplace(
        @PathVariable("collection_title") @Pattern(regexp = BatsCollection.TITLE_REGEX)
        final String collectionTitle,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID,
        @RequestBody final String jsonldPayload
    ) throws
        Exception {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Skip authorization checking if authorization is not enabled or no user is
        // logged in.
        if (authHandler != null) {

            String user = AuthorizationUtils.getUser();

            // If the user can't read the model, return an error message
            if (user != null && !authHandler.checkPermission(user, Permissions.UPDATE, modelUUID)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User " + user + " lacks permission to UPDATE model " + modelUUID);
            }
        }

        // Cache old data for rollback
        LOGGER.info("Getting rollback json-ld for update");
        String oldJsonld = documentService.getJsonld(modelUUID);

        // Extract created timestamp
        String createdTime = graphService.getCreatedTimeForModel(collectionTitle, modelUUID);

        // Update graph database model
        LOGGER.info("Uploading model to graph database: " + modelUUID);
        graphService.uploadJsonld(collectionTitle, modelUUID, jsonldPayload, createdTime);

        // Add updated model to document store
        LOGGER.info("Uploading model to document store: " + modelUUID);
        try {
            documentService.upload(collectionTitle, modelUUID, jsonldPayload);
            LOGGER.info("Model uploaded to document store!");
        } catch (Exception e) {
            graphService.uploadJsonld(collectionTitle, modelUUID, oldJsonld, createdTime);
            LOGGER.error(UPLOAD_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Model unable to be uploaded to document store"
            );
        }

        String jsonld = graphService.getModelJsonld(collectionTitle, modelUUID);
        BatsModel batsModel = new BatsModel(modelUUID, jsonld);
        return ResponseEntity.ok(batsModel);
    }

    /**
     * UPDATE (PARTIAL) for Model w/ UUID in Collection collection.
     *
     * @param collectionTitle Title for Collection collection that Model belonds to
     * @param modelUUID   UUID for Model to partially update
     * @param jsonPayload Partial JSON-LD of new Model to update current Model
     * @return            BatsModel for newly updated Model
     * @throws Exception
    */
    @RequestMapping(
        value = "/{collection_title}/models/{model_uuid}",
        method = RequestMethod.PATCH,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> updateModelPartial(
        @PathVariable("collection_title") @Pattern(regexp = BatsCollection.TITLE_REGEX)
        final String collectionTitle,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID,
        @RequestBody final String jsonPayload
    ) throws Exception {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Skip authorization checking if authorization is not enabled or no user is
        // logged in.
        if (authHandler != null) {

            String user = AuthorizationUtils.getUser();

            // If the user can't update the model, return an error message
            if (user != null && !authHandler.checkPermission(user, Permissions.UPDATE, modelUUID)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User " + user + " lacks permission to UPDATE model " + modelUUID);
            }
        }

        // Cache old data for rollback
        LOGGER.info("Getting rollback json-ld...");
        String oldJsonld = documentService.getJsonld(modelUUID);

        // Get merged JSON-LD from Model and new JSON-LD in request
        LOGGER.info("Merging json-ld w/ graph model json-ld: " + modelUUID);
        String mergedGraphJsonld = graphService.mergeJsonldForModel(
            collectionTitle,
            modelUUID,
            jsonPayload
        );

        LOGGER.info("Merging json-ld w/ document json-ld: " + modelUUID);
        String mergedDocumentJsonld = documentService.mergeJsonldForModel(
            modelUUID,
            jsonPayload
        );

        // Extract created timestamp
        String createdTime = graphService.getCreatedTimeForModel(collectionTitle, modelUUID);

        // Update graph database model
        LOGGER.info("Uploading model to graph database: " + modelUUID);
        graphService.uploadJsonld(collectionTitle, modelUUID, mergedGraphJsonld, createdTime);

        // Add updated model to document store
        LOGGER.info("Uploading model to document store: " + modelUUID);
        try {
            documentService.upload(collectionTitle, modelUUID, mergedDocumentJsonld);
            LOGGER.info("Model uploaded to document store!");
        } catch (Exception e) {
            graphService.uploadJsonld(collectionTitle, modelUUID, oldJsonld, createdTime);
            LOGGER.error(UPLOAD_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Model unable to be uploaded to document store"
            );
        }

        String jsonld = graphService.getModelJsonld(collectionTitle, modelUUID);
        BatsModel batsModel = new BatsModel(modelUUID, jsonld);
        return ResponseEntity.ok(batsModel);
    }

    /**
     * DELETE Model w/ given UUID in Collection collection.
     *
     * @param collectionTitle Title that Model belongs to
     * @param modelUUID   UUID of Model to delete from Collection
    */
    @RequestMapping(
        value = "/{collection_title}/models/{model_uuid}",
        method = RequestMethod.DELETE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModel(
        @PathVariable("collection_title") @Pattern(regexp = BatsCollection.TITLE_REGEX)
        final String collectionTitle,
        @PathVariable("model_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String modelUUID
    ) throws IOException, NoSuchAlgorithmException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Skip authorization checking if authorization is not enabled or no user is
        // logged in.
        if (authHandler != null) {

            String user = AuthorizationUtils.getUser();

            // If the user can't read the model, return an error message
            if (user != null && !authHandler.checkPermission(user, Permissions.DELETE, modelUUID)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User " + user + " lacks permission to DELETE model " + modelUUID);
            }
        }

        // Cache old data for rollback
        LOGGER.info("Getting rollback json-ld...");
        String oldJsonld = documentService.getJsonld(modelUUID);

        // Delete model from graph DB
        LOGGER.info("Deleting model: " + modelUUID + " from graph database");
        try {
            graphService.delete(collectionTitle, modelUUID);
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

            graphService.uploadJsonld(collectionTitle, modelUUID, oldJsonld);

            LOGGER.error(DELETE_MODEL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Model unable to be deleted from document database"
            );
        }
    }
}
