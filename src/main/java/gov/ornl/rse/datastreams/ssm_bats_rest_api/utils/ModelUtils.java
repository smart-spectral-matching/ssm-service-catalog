package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ConfigUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.CustomizedBatsCollection;

@Component
public class ModelUtils {
    /**
     * Setup logger for ModelUtils.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        ModelUtils.class
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
     * Assert / checks if Apache Jena Model exists in Fuseki / TDB database and log.
     *
     * @param model        Model to check existence for
     * @param modelUuid    UUID of Model to report on error
     */
    private void assertModelExists(
        final Model model,
        final String modelUuid
    ) throws ResponseStatusException {
        if (model == null) {
            String message = "Model " + modelUuid + " Not Found";
            LOGGER.error(message);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    /**
     * Get a Model that belongs to the given Collection.
     *
     * @param collectionTitle Title of the Collection
     * @param modelUuid    UUID of the Model to fetch
     * @return Apache Jena Model for UUID
     */
    public Model getModel(
        final String collectionTitle,
        final String modelUuid
    ) {
        CustomizedBatsCollection collection = collectionUtils.getCollection(collectionTitle);
        String modelUri = configUtils.getDatasetUri(collectionTitle, modelUuid);
        Model model = collection.getModel(modelUri);
        assertModelExists(model, modelUuid);
        return model;
    }

}
