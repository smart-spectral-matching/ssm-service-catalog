package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.apache.jena.query.QueryException;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
 import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization.AuthorizationHandler;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization.Permissions;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig.Fuseki;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ConfigUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsCollection;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.CustomizedBatsCollection;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.repositories.DocumentRepository;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.AuthorizationUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.CollectionUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.sparql.DatasetSparql;

@RestController
@RequestMapping("/collections")
@Validated
public class BatsCollectionController {

    /**
     * Setup logger for BatsCollectionController.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BatsCollectionController.class);

    /**
     * Setup Application config.
     */
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Document store repository for dataset documents.
     */
    @Autowired
    private DocumentRepository repository;

    /**
     * Configuration utilities.
     */
    @Autowired
    private ConfigUtils configUtils;

    /**
     * Collection utilities.
     */
    @Autowired
    private CollectionUtils collectionUtils;

    /**
     * Class ObjectMapper.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * @return the Fuseki configuration.
     */
    private Fuseki fuseki() {
        return appConfig.getFuseki();
    }

    /**
     * Error message for reading collections.
     */
    private static final String READ_COLLECTIONS_ERROR =
            "Unable to read collection(s) on the remote Fuseki server.";

    /**
     * Error message for malformed URL.
     */
    private static final String BAD_URL_ERROR = "Error forming URL to Fuseki collection asset";

    /**
     * Error message for URL and connection IO issues to Fuseki.
     */
    private static final String URL_ACCESS_ERROR =
            "Fuseki URL / connection access error for collection";

    /**
     * CREATE a new Collection collection for Datasets.
     *
     * @param batsCollection JSON body for creating a new Collection
     * @return BatsCollection for newly created Collection in Fuseki
     */
    @RequestMapping(
      value = "",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public BatsCollection createCollection(@Valid @RequestBody final BatsCollection batsCollection)
            throws ResponseStatusException, Exception {

        // Skip authorization if not in use
        if (AuthorizationUtils.isUsingAuthorization(appConfig)) {

            // Check that the user has permission to create collectionss
            AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
            String user = AuthorizationUtils.getUser();

            if (!authHandler.checkDatasetCreationPermission(user)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User" + user + " not authorized to create collections.");
            }
        }

        String title = batsCollection.getTitle();

        // Setup the database connection
        CustomizedBatsCollection collection = collectionUtils.initCollectionConnection(title);

        // Check that a Collection with this same title doesn't already exist
        CollectionUtils.CollectionQueryStatus code =
                collectionUtils.doesCollectionExist(collection);

        if (code == CollectionUtils.CollectionQueryStatus.EXISTS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Title " + collection.getName() + " already exists!");
        }

        // Create the collection
        collection.create();
        LOGGER.info("Created collection: " + collection.getName());
        return new BatsCollection(collection.getName());
    }

    /**
     * READ A list of all collection titles.
     *
     * @return A JSON formatted list of every collection's title.
     */
    @RequestMapping(
      value = "",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String getTitles() {

        // Read the Fuseki collection list endpoint

        URL url = null;

        try {
            url = new URL(fuseki().getHostname() + ":" + fuseki().getPort() + "/$/datasets");
        } catch (MalformedURLException e) {
            LOGGER.error(BAD_URL_ERROR, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, BAD_URL_ERROR);
        }

        Scanner scanner = null;

        try {
            scanner = new Scanner(url.openStream(), "UTF-8");
        } catch (IOException e) {
            LOGGER.error(URL_ACCESS_ERROR + ": " + url, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    READ_COLLECTIONS_ERROR);
        }

        try {
            JsonNode fusekiResponse = MAPPER.readTree(scanner.useDelimiter("\\A").next());

            // Get the node containing the list of collections
            ArrayNode collectionsNode = (ArrayNode) fusekiResponse.get("datasets");
            Iterator<JsonNode> collectionIterator = collectionsNode.elements();

            // The JSON response being built
            ArrayNode response = new ArrayNode(new JsonNodeFactory(false));

            // Read out only the name field of each collection and add it to the
            // response
            while (collectionIterator.hasNext()) {
                response.add(collectionIterator.next().get("ds.name").asText().replaceAll("/", ""));
            }

            // Return the JSON representation
            return MAPPER.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            LOGGER.error(READ_COLLECTIONS_ERROR, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    READ_COLLECTIONS_ERROR);
        } finally {
            scanner.close();
        }
    }

    /**
     * READ Collection collection for given Collection title.
     *
     * @param title Title of Collection to retrieve
     * @return BatsCollection for given Collection title
     */
    @RequestMapping(
      value = "/{title}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BatsCollection getCollection(
            @PathVariable("title") @Pattern(regexp = BatsCollection.TITLE_REGEX) final String title)
            throws ResponseStatusException {
        CustomizedBatsCollection collection = collectionUtils.getCollection(title);
        return new BatsCollection(collection.getName());
    }

    /**
     * DELETE Collection collection for given Collection title.
     *
     * @param title Title of Collection to delete
     */
    @RequestMapping(
      value = "/{title}",
      method = RequestMethod.DELETE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCollection(
            @PathVariable("title") @Pattern(regexp = BatsCollection.TITLE_REGEX) final String title)
            throws Exception {
        CustomizedBatsCollection collection = collectionUtils.getCollection(title);

        // Get the Dataset UUID list for the collection
        String endpointUrl = fuseki().getHostname() + ":" + fuseki().getPort() + "/" + title;
        ArrayNode uuidArray = MAPPER.createArrayNode();
        try {
            uuidArray = DatasetSparql.getDatasetUuids(endpointUrl);
        } catch (QueryException ex) {
            LOGGER.info("No datasets to delete for datset.");
        }

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Check whether authorization is to be enforced
        boolean authorization = AuthorizationUtils.isUsingAuthorization(appConfig);

        // Skip authorization check if no authorization or no user
        if (authorization) {

            String user = AuthorizationUtils.getUser();

            // Construct a list of all UUIDs belonging to this collection which the user isn't
            // authorized to delete
            ArrayList<String> unauthorizedUUIDs = new ArrayList<String>();

            for (JsonNode datasetUuidNode : uuidArray) {
                String datasetUUID = datasetUuidNode.asText();
                if (!authHandler.checkPermission(user, Permissions.DELETE, datasetUUID)) {
                    unauthorizedUUIDs.add(datasetUUID);
                }
            }

            // If there were any undeletable datasets, do not delete the collection and instead
            // return an error.
            if (!unauthorizedUUIDs.isEmpty()) {

                String initialMessage = "User " + user
                        + " is not authorized to delete the following datasets contained in the "
                        + "collectiont: ";

                StringBuilder message = new StringBuilder(initialMessage);

                for (String uuid : unauthorizedUUIDs) {
                    String messageAppend = uuid + ",";
                    message.append(messageAppend);
                }

                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message.toString());
            }
        }

        // Loop to delete the datasets from collection
        for (JsonNode datasetUuidNode : uuidArray) {
            String datasetUUID = datasetUuidNode.asText();
            String uuid = datasetUUID.substring(datasetUUID.lastIndexOf('/') + 1);
            String datasetUri = configUtils.getDatasetUri(title, uuid);

            // Get dataset for rollback
            Model dataset = collection.getModel(datasetUri);

            // Delete dataset from graph database
            LOGGER.info("Deleting dataset: " + uuid + " from graph database");
            try {
                collection.deleteDataset(datasetUri);
            } catch (Exception ex) {
                String message = "Unable to delete dataset: " + uuid + " from graph database.";
                LOGGER.error(message);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            }

            // Delete dataset from document store (rollback graph database if fails)
            LOGGER.info("Deleting dataset: " + uuid + " from document store");
            try {
                repository.delete(repository.findById(uuid).get());

            } catch (Exception ex) {
                String message = "Unable to delete dataset: " + uuid + " from document store.";
                LOGGER.error(message);
                collection.updateModel(datasetUri, dataset);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            }

            // If authorization is off or there is no user, skip authorization check
            if (authorization) {

                String user = AuthorizationUtils.getUser();

                try {

                    // Delete all authorization information related to this object
                    authHandler.deleteObject(user, uuid);

                } catch (Exception ex) {
                    String message = "User " + user + " lost DELETE permission on object " + uuid
                            + " while it was being deleted. Authorization server is now in an "
                            + "inconsistant state.";
                    LOGGER.error(message);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
                }

            }

        }

        // Delete collection collection from graph database
        collection.delete();
        LOGGER.info("Deleted collection: " + collection.getName());
    }
}
