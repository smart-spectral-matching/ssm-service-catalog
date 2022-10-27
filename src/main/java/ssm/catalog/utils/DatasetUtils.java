package ssm.catalog.utils;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import ssm.catalog.configs.ConfigUtils;
import ssm.catalog.models.CustomizedCollection;

@Component
public class DatasetUtils {
    /**
     * Setup logger for DatasetUtils.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        DatasetUtils.class
    );

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
     * Assert / checks if Dataset exists.
     *
     * @param dataset        Dataset to check existence for
     * @param datasetUuid    UUID of Dataset to report on error
     */
    private void assertDatasetExists(
        final Model dataset,
        final String datasetUuid
    ) throws ResponseStatusException {
        if (dataset == null) {
            String message = "Dataset " + datasetUuid + " Not Found";
            LOGGER.error(message);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    /**
     * Get a Dataset that belongs to the given Collection.
     *
     * @param collectionTitle Title of the Collection
     * @param datasetUuid    UUID of the Dataset to fetch
     * @return Dataset for UUID
     */
    public Model getDataset(
        final String collectionTitle,
        final String datasetUuid
    ) {
        CustomizedCollection collection = collectionUtils.getCollection(collectionTitle);
        String datasetUri = configUtils.getDatasetUri(collectionTitle, datasetUuid);
        Model dataset = collection.getDataset(datasetUri);
        assertDatasetExists(dataset, datasetUuid);
        return dataset;
    }

}
