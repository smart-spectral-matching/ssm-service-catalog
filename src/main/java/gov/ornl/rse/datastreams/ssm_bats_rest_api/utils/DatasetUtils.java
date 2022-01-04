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
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.CustomizedBatsDataSet;

@Component
public class DatasetUtils {

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
     * Setup logger for DatasetUtils.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        DatasetUtils.class
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
     * Initialize Apache Jena Dataset connection via BATS.
     *
     * @param datasetTitle Dataset title
     * @return BATS dataset with name, Fuseki host, and Fuseki port configured
     */
    public CustomizedBatsDataSet initDatasetConnection(final String datasetTitle) {
        CustomizedBatsDataSet dataset = new CustomizedBatsDataSet();
        dataset.setName(datasetTitle);
        dataset.setHost(fuseki().getHostname());
        dataset.setPort(fuseki().getPort());
        return dataset;
    }

    /**
     * Return if given Apache Jena Dataset exists in Apache Fuseki / TDB database.
     *
     * @param dataset      Dataset to check for existence in Apache Fuseki / TDB database
     * @return DataSetQueryStatus; dataset status
     */
    public DataSetQueryStatus doesDataSetExist(
        final CustomizedBatsDataSet dataset
    ) {
        // Construct Fuseki API URL for the specific dataset
        URL url = null;

        try {
            url = new URL(
                    fuseki().getHostname()
                    + ":"
                    + fuseki().getPort()
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
     * Asserts / checks if Apache Jena Dataset exists in Apache Fuseki / TDB database.
     *
     * @param dataset Dataset to check for existence in Apache Fuseki / TDB database
     */
    private void assertDataSetExists(
        final CustomizedBatsDataSet dataset
    ) throws ResponseStatusException {
        LOGGER.info("Checking dataset: " + dataset.getName());
        DataSetQueryStatus code = doesDataSetExist(dataset);
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
        LOGGER.info("Dataset " + dataset.getName() + " exists!");
    }

    /**
     * Get Dataset with given title.
     *
     * @param datasetTitle Title of dataset to get
     * @return CustomizedBatsDataset object for the dataset title given
     * @throws ResponseStatusException
     */
    public CustomizedBatsDataSet getDataset(
        final String datasetTitle
    ) throws ResponseStatusException {
        CustomizedBatsDataSet dataset = initDatasetConnection(datasetTitle);
        assertDataSetExists(dataset);
        return dataset;
    }

}
