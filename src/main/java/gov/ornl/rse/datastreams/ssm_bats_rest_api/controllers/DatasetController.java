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
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDataset;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDatasetFormats;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.services.DocumentService;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.services.GraphService;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.AuthorizationUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.UUIDGenerator;

@RestController
@RequestMapping("/collections")
@Validated
public class DatasetController {

    /**
     * Setup logger for DatasetController.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        DatasetController.class
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
     * Error message for uploading dataset.
    */
    private static final String UPLOAD_DATASET_ERROR =
        "Unable to upload dataset.";

    /**
     * Error message for reading dataset.
    */
    private static final String READ_DATASET_ERROR =
        "Unable to read dataset.";

    /**
     * Error message for deleting dataset.
    */
    private static final String DELETE_DATASET_ERROR =
        "Unable to delete dataset.";

    /**
     * FETCH a certain amount of Datasets for a Collection.
     *
     * @param collectionTitle Title of the Collection this dataset belongs to
     * @param pageNumber page number to start on,
     *    must be positive (default: 1)
     * @param pageSize number of results to return,
     *    must be positive (default: 5)
     * @param returnFull boolean for returning full dataset or not
     * @return List either BatsDatasets (full) or List of Map (not full)
     */
    @RequestMapping(
        value = "/{collection_title}/datasets",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> queryDatasets(
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
        Map<String, Object> body = graphService.getDatasets(
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

                // Map of uuids to datasets the user has authorization to read
                HashMap<String, Object> authorizedDatasets = new HashMap<String, Object>();

                // Get each dataset that the user has access to and put it in the list
                for (String key : uuids) {
                    if (authHandler.checkPermission(user, Permissions.READ, key)) {
                        authorizedDatasets.put(key, body.get(key));
                    }
                }

                body = authorizedDatasets;

            }
        }

        return ResponseEntity.ok(body);
    }

    /**
     * CREATE a new Dataset in the Collection collection.
     *
     * @param collectionTitle Title for Collection collection to add the new Dataset
     * @param jsonldPayload JSON-LD of new Dataset
     * @return            BatsDataset for created Dataset in the Collection
     * @throws Exception
    */
    @RequestMapping(
        value = "/{collection_title}/datasets",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ResponseEntity<?> createDataset(
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

            // If the user doesn't have permission to create new datasets, return an error.
            if (user != null && !authHandler.checkDatasetCreationPermission(user)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User " + user + " lacks permission to create a new dataset.");
            }
        }

        String datasetUUID = UUIDGenerator.generateUUID();

        // Create in the graph database
        try {
            graphService.uploadJsonld(collectionTitle, datasetUUID, jsonldPayload);
        } catch (Exception e) {
            LOGGER.error(UPLOAD_DATASET_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Dataset unable to be uploaded to graph database"
            );
        }

        // Create in the document database w/ rollback of graph database on error
        LOGGER.info("Uploading dataset to document store: " + datasetUUID);
        try {
            documentService.upload(collectionTitle, datasetUUID, jsonldPayload);
            LOGGER.info("Dataset uploaded to document store!");
        } catch (Exception e) {
            // Rollback graph database insert of dataset
            LOGGER.error("Unable to create dataset in document store: " + datasetUUID);
            LOGGER.error("Rolling back create from graph database for dataset: " + datasetUUID);
            LOGGER.error(UPLOAD_DATASET_ERROR, e);

            graphService.delete(collectionTitle, datasetUUID);

            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Dataset unable to be uploaded to document store"
            );
        }

        String jsonld = graphService.getModelJsonld(collectionTitle, datasetUUID);
        BatsDataset batsDataset = new BatsDataset(datasetUUID, jsonld);
        return new ResponseEntity<BatsDataset>(batsDataset, HttpStatus.CREATED);
    }

    /**
     * READ Dataset w/ given UUID in Collection collection.
     *
     * @param collectionTitle Title for Collection collection that Dataset belonds to
     * @param datasetUUID    UUID for Dataset to retrieve from the Collection
     * @param format       Format to return the dataset ["graph", "json", "jsonld"]
     * @return             Requested format of Dataset UUID
     * @throws Exception
    */
    @RequestMapping(
        value = "/{collection_title}/datasets/{dataset_uuid}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> getDataset(
        @PathVariable("collection_title") @Pattern(regexp = BatsCollection.TITLE_REGEX)
        final String collectionTitle,
        @PathVariable("dataset_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String datasetUUID,
        @RequestParam(name = "format", defaultValue = "json")
        final BatsDatasetFormats format
    ) throws Exception {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Skip authorization checking if authorization is not enabled or no user is
        // logged in.
        if (authHandler != null) {

            String user = AuthorizationUtils.getUser();

            // If the user can't read the dataset, return an error message
            if (user != null && !authHandler.checkPermission(user, Permissions.READ, datasetUUID)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User " + user + " lacks permission to READ dataset " + datasetUUID);
            }
        }

        try {
            documentService.getJson(datasetUUID);
        } catch (ResourceNotFoundException e) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + datasetUUID + " Not Found"
            );
        }
        String output;
        if (format == BatsDatasetFormats.GRAPH || format == BatsDatasetFormats.FULL) {
            String jsonld = graphService.getModelJsonld(collectionTitle, datasetUUID);
            BatsDataset batsDataset = new BatsDataset(datasetUUID, jsonld);
            return ResponseEntity.ok(batsDataset);
        }

        if (format == BatsDatasetFormats.JSONLD) {
            output = documentService.getJsonld(datasetUUID);
        } else {
            output = documentService.getJson(datasetUUID);
        }
        return ResponseEntity.ok(output);
    }

    /**
     * READ A list of all UUIDs for datasets belonging to the given collection.
     *
     * @param collectionTitle Title of the collection to find datasets for.
     * @return A JSON list of all UUIDs
     */
    @RequestMapping(
        value = "/{collection_title}/datasets/uuids",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> getUUIDs(@PathVariable("collection_title")
        @Pattern(regexp = BatsCollection.TITLE_REGEX) final String collectionTitle
    ) {
        try {
            String uuids = graphService.getDatasetUUIDsForCollection(collectionTitle);
            return ResponseEntity.ok(uuids);
        } catch (JsonProcessingException e) {
            LOGGER.error(READ_DATASET_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                READ_DATASET_ERROR
            );
        }
    }

    /**
     * UPDATE (REPLACE) for Dataset w/ UUID in Collection collection.
     *
     * @param collectionTitle  Title for Collection collection that Dataset belonds to
     * @param datasetUUID     UUID for Dataset to replace
     * @param jsonldPayload JSON-LD of new Dataset to replace current Dataset
     * @return              BatsDataset for newly updated Dataset
     * @throws Exception
    */
    @RequestMapping(
        value = "/{collection_title}/datasets/{dataset_uuid}",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> updateDatasetReplace(
        @PathVariable("collection_title") @Pattern(regexp = BatsCollection.TITLE_REGEX)
        final String collectionTitle,
        @PathVariable("dataset_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String datasetUUID,
        @RequestBody final String jsonldPayload
    ) throws
        Exception {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Skip authorization checking if authorization is not enabled or no user is
        // logged in.
        if (authHandler != null) {

            String user = AuthorizationUtils.getUser();

            // If the user can't read the dataset, return an error message
            if (user != null && !authHandler.checkPermission(user, Permissions.UPDATE,
                    datasetUUID)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User " + user + " lacks permission to UPDATE dataset " + datasetUUID);
            }
        }

        // Cache old data for rollback
        LOGGER.info("Getting rollback json-ld for update");
        String oldJsonld = documentService.getJsonld(datasetUUID);

        // Extract created timestamp
        String createdTime = graphService.getCreatedTimeForModel(collectionTitle, datasetUUID);

        // Update graph database dataset
        LOGGER.info("Uploading dataset to graph database: " + datasetUUID);
        graphService.uploadJsonld(collectionTitle, datasetUUID, jsonldPayload, createdTime);

        // Add updated dataset to document store
        LOGGER.info("Uploading dataset to document store: " + datasetUUID);
        try {
            documentService.upload(collectionTitle, datasetUUID, jsonldPayload);
            LOGGER.info("Dataset uploaded to document store!");
        } catch (Exception e) {
            graphService.uploadJsonld(collectionTitle, datasetUUID, oldJsonld, createdTime);
            LOGGER.error(UPLOAD_DATASET_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Dataset unable to be uploaded to document store"
            );
        }

        String jsonld = graphService.getModelJsonld(collectionTitle, datasetUUID);
        BatsDataset batsDataset = new BatsDataset(datasetUUID, jsonld);
        return ResponseEntity.ok(batsDataset);
    }

    /**
     * UPDATE (PARTIAL) for Dataset w/ UUID in Collection collection.
     *
     * @param collectionTitle Title for Collection collection that Dataset belonds to
     * @param datasetUUID   UUID for Dataset to partially update
     * @param jsonPayload Partial JSON-LD of new Dataset to update current Dataset
     * @return            BatsDataset for newly updated Dataset
     * @throws Exception
    */
    @RequestMapping(
        value = "/{collection_title}/datasets/{dataset_uuid}",
        method = RequestMethod.PATCH,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<?> updateDatasetPartial(
        @PathVariable("collection_title") @Pattern(regexp = BatsCollection.TITLE_REGEX)
        final String collectionTitle,
        @PathVariable("dataset_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String datasetUUID,
        @RequestBody final String jsonPayload
    ) throws Exception {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Skip authorization checking if authorization is not enabled or no user is
        // logged in.
        if (authHandler != null) {

            String user = AuthorizationUtils.getUser();

            // If the user can't update the dataset, return an error message
            if (user != null && !authHandler.checkPermission(user, Permissions.UPDATE,
                    datasetUUID)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User " + user + " lacks permission to UPDATE dataset " + datasetUUID);
            }
        }

        // Cache old data for rollback
        LOGGER.info("Getting rollback json-ld...");
        String oldJsonld = documentService.getJsonld(datasetUUID);

        // Get merged JSON-LD from Dataset and new JSON-LD in request
        LOGGER.info("Merging json-ld w/ graph dataset json-ld: " + datasetUUID);
        String mergedGraphJsonld = graphService.mergeJsonldForDataset(
            collectionTitle,
            datasetUUID,
            jsonPayload
        );

        LOGGER.info("Merging json-ld w/ document json-ld: " + datasetUUID);
        String mergedDocumentJsonld = documentService.mergeJsonldForDataset(
            datasetUUID,
            jsonPayload
        );

        // Extract created timestamp
        String createdTime = graphService.getCreatedTimeForModel(collectionTitle, datasetUUID);

        // Update graph database dataset
        LOGGER.info("Uploading dataset to graph database: " + datasetUUID);
        graphService.uploadJsonld(collectionTitle, datasetUUID, mergedGraphJsonld, createdTime);

        // Add updated dataset to document store
        LOGGER.info("Uploading dataset to document store: " + datasetUUID);
        try {
            documentService.upload(collectionTitle, datasetUUID, mergedDocumentJsonld);
            LOGGER.info("Dataset uploaded to document store!");
        } catch (Exception e) {
            graphService.uploadJsonld(collectionTitle, datasetUUID, oldJsonld, createdTime);
            LOGGER.error(UPLOAD_DATASET_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Dataset unable to be uploaded to document store"
            );
        }

        String jsonld = graphService.getModelJsonld(collectionTitle, datasetUUID);
        BatsDataset batsDataset = new BatsDataset(datasetUUID, jsonld);
        return ResponseEntity.ok(batsDataset);
    }

    /**
     * DELETE Dataset w/ given UUID in Collection collection.
     *
     * @param collectionTitle Title that Dataset belongs to
     * @param datasetUUID   UUID of Dataset to delete from Collection
    */
    @RequestMapping(
        value = "/{collection_title}/datasets/{dataset_uuid}",
        method = RequestMethod.DELETE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDataset(
        @PathVariable("collection_title") @Pattern(regexp = BatsCollection.TITLE_REGEX)
        final String collectionTitle,
        @PathVariable("dataset_uuid") @Pattern(regexp = UUIDGenerator.UUID_REGEX)
        final String datasetUUID
    ) throws IOException, NoSuchAlgorithmException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Skip authorization checking if authorization is not enabled or no user is
        // logged in.
        if (authHandler != null) {

            String user = AuthorizationUtils.getUser();

            // If the user can't read the dataset, return an error message
            if (user != null && !authHandler.checkPermission(user, Permissions.DELETE,
                    datasetUUID)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "User " + user + " lacks permission to DELETE dataset " + datasetUUID);
            }
        }

        // Cache old data for rollback
        LOGGER.info("Getting rollback json-ld...");
        String oldJsonld = documentService.getJsonld(datasetUUID);

        // Delete dataset from graph DB
        LOGGER.info("Deleting dataset: " + datasetUUID + " from graph database");
        try {
            graphService.delete(collectionTitle, datasetUUID);
        } catch (Exception e) {
            LOGGER.error(DELETE_DATASET_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Dataset unable to be deleted from graph database"
            );
        }

        // Delete dataset from document store
        LOGGER.info("Deleting dataset: " + datasetUUID + " from document store");
        try {
            documentService.delete(datasetUUID);
        } catch (Exception e) {
            // Rolling back graph database deletion of dataset
            LOGGER.error("Unable to delete dataset in document store: " + datasetUUID);
            LOGGER.error("Rolling back delete from graph database for dataset: " + datasetUUID);

            graphService.uploadJsonld(collectionTitle, datasetUUID, oldJsonld);

            LOGGER.error(DELETE_DATASET_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Dataset unable to be deleted from document database"
            );
        }
    }
}
