package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig.Fuseki;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.CustomizedBatsCollection;

@Component
public class CollectionUtils {

    /**
     * Status from checking if Fuseki Collection exists. {@link #EXISTS}
     * {@link #DOES_NOT_EXIST} {@link #BAD_URL} {@link #BAD_CONNECTION}
     */
    public enum CollectionQueryStatus {
        /**
         * Collection exists in Fuseki.
         */
        EXISTS,

        /**
         * Collection does not exist in Fuseki.
         */
        DOES_NOT_EXIST,

        /**
         * Malformed URL for Fuseki Collection.
         */
        BAD_URL,

        /**
         * Bad connection to Fuseki Collection URL.
         */
        BAD_CONNECTION
    }

    /**
     * Setup logger for CollectionUtils.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        CollectionUtils.class
    );

    /**
     * Configuration from properties.
    */
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * @return shorthand for the Fuseki configuration
     */
    private Fuseki fuseki() {
        return appConfig.getFuseki();
    }

    /**
     * Initialize Apache Jena Collection connection via BATS.
     *
     * @param collectionTitle Collection title
     * @return BATS collection with name, Fuseki host, and Fuseki port configured
     */
    public CustomizedBatsCollection initCollectionConnection(final String collectionTitle) {
        CustomizedBatsCollection collection = new CustomizedBatsCollection();
        collection.setName(collectionTitle);
        collection.setHost(fuseki().getHostname());
        collection.setPort(fuseki().getPort());
        return collection;
    }

    /**
     * Return if given Apache Jena Collection exists in Apache Fuseki / TDB database.
     *
     * @param collection      Collection to check for existence in Apache Fuseki / TDB database
     * @return CollectionQueryStatus; collection status
     */
    public CollectionQueryStatus doesCollectionExist(
        final CustomizedBatsCollection collection
    ) {
        // Construct Fuseki API URL for the specific collection
        URL url = null;

        try {
            url = new URL(
                    fuseki().getHostname()
                    + ":"
                    + fuseki().getPort()
                    + "/$/datasets/"
                    + collection.getName());
        } catch (MalformedURLException e) {
            return CollectionQueryStatus.BAD_URL;
        }

        // Get response code for collection to determine if it exists
        int code = HttpStatus.I_AM_A_TEAPOT.value();
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            code = http.getResponseCode();
        } catch (IOException e) {
            return CollectionQueryStatus.BAD_CONNECTION;
        }

        if (code == HttpStatus.OK.value()) {
            return CollectionQueryStatus.EXISTS;
        } else {
            return CollectionQueryStatus.DOES_NOT_EXIST;
        }
    }

    /**
     * Asserts / checks if Apache Jena Collection exists in Apache Fuseki / TDB database.
     *
     * @param collection Collection to check for existence in Apache Fuseki / TDB database
     */
    private void assertCollectionExists(
        final CustomizedBatsCollection collection
    ) throws ResponseStatusException {
        LOGGER.info("Checking collection: " + collection.getName());
        CollectionQueryStatus code = doesCollectionExist(collection);
        if (code == CollectionQueryStatus.DOES_NOT_EXIST) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Collection " + collection.getName() + " NOT FOUND!");
        } else if (
            code == CollectionQueryStatus.BAD_CONNECTION || code == CollectionQueryStatus.BAD_URL
        ) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error accessing collection " + collection.getName() + "!");
        }
        LOGGER.info("Collection " + collection.getName() + " exists!");
    }

    /**
     * Get Collection with given title.
     *
     * @param collectionTitle Title of collection to get
     * @return CustomizedBatsCollection object for the collection title given
     * @throws ResponseStatusException
     */
    public CustomizedBatsCollection getCollection(
        final String collectionTitle
    ) throws ResponseStatusException {
        CustomizedBatsCollection collection = initCollectionConnection(collectionTitle);
        assertCollectionExists(collection);
        return collection;
    }

}
