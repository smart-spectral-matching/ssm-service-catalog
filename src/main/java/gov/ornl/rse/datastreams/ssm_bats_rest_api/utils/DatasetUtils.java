package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import gov.ornl.rse.bats.DataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig.Fuseki;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.CustomizedBatsDataSet;

public final class DatasetUtils {

    /**
     * Status from checking if Fuseki Dataset exists. {@link #EXISTS}
     * {@link #DOES_NOT_EXIST} {@link #BAD_URL} {@link #BAD_CONNECTION}
     */
    public enum DataSetQueryStatus {
        /**
         * Dataset exists in Fuseki.
         */
        EXISTS,

        /**
         * Dataset does not exist in Fuseki.
         */
        DOES_NOT_EXIST,

        /**
         * Malformed URL for Fuseki Dataset.
         */
        BAD_URL,

        /**
         * Bad connection to Fuseki Dataset URL.
         */
        BAD_CONNECTION
    }

    /**
     * Static methods only.
     */
    private DatasetUtils() { }

    /**
     * Return if given Apache Jena Dataset exists in Fuseki database.
     *
     * @param dataset      Dataset to check for existence in Fuseki database
     * @param fusekiObject Fuseki object that holds the Fuseki database info
     * @return DataSetQueryStatus; dataset status
     */
    public static DataSetQueryStatus doesDataSetExist(
        final DataSet dataset,
        final Fuseki fusekiObject
    ) {
        // Construct Fuseki API URL for the specific dataset
        URL url = null;

        try {
            url = new URL(
                    fusekiObject.getHostname()
                    + ":"
                    + fusekiObject.getPort()
                    + "/$/datasets/"
                    + dataset.getName());
        } catch (MalformedURLException e) {
            return DataSetQueryStatus.BAD_URL;
        }

        // Get response code for dataset to determine if it exists
        int code = HttpStatus.I_AM_A_TEAPOT.value();
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            code = http.getResponseCode();
        } catch (IOException e) {
            return DataSetQueryStatus.BAD_CONNECTION;
        }

        if (code == HttpStatus.OK.value()) {
            return DataSetQueryStatus.EXISTS;
        } else {
            return DataSetQueryStatus.DOES_NOT_EXIST;
        }
    }

    /**
     * Checks if Apache Jena Dataset exists in Fuseki database and log.
     *
     * @param datasetTitle Dataset title to check for existence in Fuseki database
     * @param fusekiObject Fuseki object that holds the Fuseki database info
     * @param logger       Logger to send message to
     */
    public static void checkDataSetExists(
        final String datasetTitle,
        final Fuseki fusekiObject,
        final Logger logger
    ) throws ResponseStatusException {
        CustomizedBatsDataSet dataset = initDataset(datasetTitle, fusekiObject);

        logger.info("Checking dataset: " + dataset.getName());
        DataSetQueryStatus code = doesDataSetExist(dataset, fusekiObject);
        if (code == DataSetQueryStatus.DOES_NOT_EXIST) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Dataset " + dataset.getName() + " NOT FOUND!");
        } else if (
            code == DataSetQueryStatus.BAD_CONNECTION || code == DataSetQueryStatus.BAD_URL
        ) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error accessing dataset " + dataset.getName() + "!");
        }
        logger.info("Dataset " + dataset.getName() + " exists!");
    }

    /**
     * Initialize BATS dataset connection.
     *
     * @param datasetTitle Dataset title from user parameter
     * @param fusekiObject Fuseki object that holds the Fuseki database info
     * @return Bats dataset with name, Fuseki host, and Fuseki port configured
     */
    public static CustomizedBatsDataSet initDataset(
        final String datasetTitle,
        final Fuseki fusekiObject
    ) {
        CustomizedBatsDataSet dataset = new CustomizedBatsDataSet();
        dataset.setName(datasetTitle);
        dataset.setHost(fusekiObject.getHostname());
        dataset.setPort(fusekiObject.getPort());
        return dataset;
    }

}
