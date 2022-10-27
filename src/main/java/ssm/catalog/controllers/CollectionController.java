package ssm.catalog.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import ssm.catalog.configs.ApplicationConfig;
import ssm.catalog.configs.ApplicationConfig.Fuseki;
import ssm.catalog.configs.ConfigUtils;
import ssm.catalog.models.Collection;
import ssm.catalog.models.CustomizedCollection;
import ssm.catalog.repositories.DatasetDocumentRepository;
import ssm.catalog.utils.CollectionUtils;
import ssm.catalog.utils.sparql.DatasetSparql;

@RestController
@RequestMapping("/collections")
@Validated
public class CollectionController {

    /**
     * Setup logger for CollectionController.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        CollectionController.class
    );

    /**
     * Setup Application config.
     */
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Document store repository for dataset documents.
     */
    @Autowired
    private DatasetDocumentRepository repository;

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
    private static final String BAD_URL_ERROR =
        "Error forming URL to Fuseki collection asset";

    /**
     * Error message for URL and connection IO issues to Fuseki.
    */
    private static final String URL_ACCESS_ERROR =
        "Fuseki URL / connection access error for collection";

    /**
     * CREATE a new Collection collection for Datasets.
     *
     * @param collection JSON body for creating a new Collection
     * @return collection for newly created Collection in Fuseki
    */
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public Collection  createCollection(
        @Valid @RequestBody final Collection collectionJson
    ) throws ResponseStatusException, Exception {
        String title = collectionJson.getTitle();

        // Setup the database connection
        CustomizedCollection collection = collectionUtils.initCollectionConnection(title);

        // Check that a Collection with this same title doesn't already exist
        CollectionUtils.CollectionQueryStatus code = collectionUtils.doesCollectionExist(collection);

        if (code == CollectionUtils.CollectionQueryStatus.EXISTS) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Title " + collection.getName() + " already exists!"
            );
        }

        // Create the collection
        collection.create();
        LOGGER.info("Created collection: " + collection.getName());
        return new Collection(collection.getName());
    }

    /**
     * READ A list of all collection titles.
     *
     * @return A JSON formatted list of every collection's title.
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String getTitles() {

        //Read the Fuseki dataset list endpoint

        URL url = null;

        try {
            url = new URL(fuseki().getHostname()
                    + ":"
                    + fuseki().getPort()
                    + "/$/datasets");
        } catch (MalformedURLException e) {
            LOGGER.error(BAD_URL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                BAD_URL_ERROR
            );
        }

        Scanner scanner = null;

        try {
            scanner = new Scanner(url.openStream(), "UTF-8");
        } catch (IOException e) {
            LOGGER.error(URL_ACCESS_ERROR + ": " + url, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                READ_COLLECTIONS_ERROR
            );
        }

        try {
            JsonNode fusekiResponse = MAPPER.readTree(
                    scanner.useDelimiter("\\A").next());

            //Get the node containing the list of collections
            ArrayNode collectionsNode = (ArrayNode) fusekiResponse.get("datasets");
            Iterator<JsonNode> collectionIterator = collectionsNode.elements();

            //The JSON response being built
            ArrayNode response = new ArrayNode(new JsonNodeFactory(false));

            //Read out only the name field of each collection and add it to the
            //response
            while (collectionIterator.hasNext()) {
                response.add(
                    collectionIterator.next()
                                   .get("ds.name")
                                   .asText()
                                   .replaceAll("/", "")
                );
            }

            //Return the JSON representation
            return MAPPER.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            LOGGER.error(READ_COLLECTIONS_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                READ_COLLECTIONS_ERROR
            );
        } finally {
            scanner.close();
        }
    }

    /**
     * READ Collection collection for given Collection title.
     *
     * @param title Title of Collection to retrieve
     * @return collection for given Collection title
    */
    @RequestMapping(value = "/{title}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Collection getCollection(@PathVariable("title")
        @Pattern(regexp = Collection.TITLE_REGEX) final String title)
        throws
            ResponseStatusException {
        CustomizedCollection collection = collectionUtils.getCollection(title);
        return new Collection(collection.getName());
    }

    /**
     * DELETE Collection collection for given Collection title.
     *
     * @param title Title of Collection to delete
    */
    @RequestMapping(value = "/{title}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCollection(@PathVariable("title")
        @Pattern(regexp = Collection.TITLE_REGEX) final String title)
        throws Exception {
        CustomizedCollection collection = collectionUtils.getCollection(title);

        // Get the Dataset UUID list for the collection
        String endpointUrl = fuseki().getHostname() + ":" + fuseki().getPort() + "/" + title;
        ArrayNode uuidArray = MAPPER.createArrayNode();
        try {
            uuidArray = DatasetSparql.getDatasetUuids(endpointUrl);
        } catch (QueryException ex) {
            LOGGER.info("No datasets to delete for collection.");
        }

        // Loop to delete the datasets from collection
        for (JsonNode datasetUuidNode: uuidArray) {
            String datasetUUID = datasetUuidNode.asText();
            String uuid = datasetUUID.substring(datasetUUID.lastIndexOf('/') + 1);
            String datasetUri = configUtils.getDatasetUri(title, uuid);

            // Get dataset for rollback
            Model dataset = collection.getDataset(datasetUri);

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
                collection.updateDataset(datasetUri, dataset);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            }

        }


        // Delete collection collection from graph database
        collection.delete();
        LOGGER.info("Deleted collection: " + collection.getName());
    }
}
