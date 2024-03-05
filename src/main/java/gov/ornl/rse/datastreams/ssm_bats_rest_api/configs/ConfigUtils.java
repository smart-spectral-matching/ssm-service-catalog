package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import java.util.Arrays;
import java.util.Set;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Generic configuration utilities derived from the Spring environment
 * and @Configuration Beans. Because these are derived from the environment, we
 * can't use static methods.
 */
@Component
public class ConfigUtils {

    /**
     * Immutable sets of strings which represent deployed profiles.
     */
    private static final Set<String> DEPLOYMENT_PROFILES = Set.of(
        "local", "dev", "qa", "prod"
    );

    /**
     * Spring environment.
     */
    @Autowired
    private Environment env;

    /**
     * Java servlet context.
     */
    @Autowired
    private ServletContext servletContext;

    /**
     * Application config.
     */
    @Autowired
    private ApplicationConfig config;

    /**
     * @return complete array of strings
     */
    public String[] getActiveProfiles() {
        return env.getActiveProfiles();
    }

    /**
     * @return true if one of the profiles we are using indicates a deployed
     *         instance
     */
    public boolean isDeployedInstance() {
        return Arrays.stream(getActiveProfiles())
            .anyMatch(p -> DEPLOYMENT_PROFILES.contains(p));
    }

    /**
     * @return true if there was a profile configuration error
     */
    public boolean isConfigurationError() {
        return Arrays.stream(getActiveProfiles())
            .filter(p -> DEPLOYMENT_PROFILES.contains(p))
            .count() > 1;
    }

    /**
     * @return host path from configuration + the base context path
     */
    public String getBasePath() {
        return config.getHost() + servletContext.getContextPath();
    }

    /**
     * Returns collection API URL given the collection.
     *
     * @param collectionUUID UUID for the Collection the dataset belongs to
     * @return Full URI for the Collection
     */
    public String getCollectionUri(final String collectionUUID) {
        final String uri = getBasePath() + "/colections/" + collectionUUID;
        return uri.replace("\"", "");
    }

    /**
     * Returns Dataset API URI given the Collection and Dataset UUID.
     *
     * @param collectionUUID UUID for the Collection the dataset belongs to
     * @param datasetUUID   UUID for the Dataset
     * @return Full URI for the Dataset
     */
    public String getDatasetUri(final String collectionUUID, final String datasetUUID) {
        String baseUri = getBasePath();
        String collectionUri = baseUri + "/collections/" + collectionUUID;
        String datasetUri = collectionUri + "/datasets/" + datasetUUID;
        return datasetUri.replace("\"", "");
    }

}
