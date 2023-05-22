package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletContext;

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
     * Returns dataset API URL given the dataset.
     *
     * @param datasetUUID UUID for the Dataset the model belongs to
     * @return Full URI for the Dataset
     */
    public String getDatasetUri(final String datasetUUID) {
        final String uri = getBasePath() + "/datasets/" + datasetUUID;
        return uri.replace("\"", "");
    }

    /**
     * Returns Model API URI given the Dataset and Model UUID.
     *
     * @param datasetUUID UUID for the Dataset the model belongs to
     * @param modelUUID   UUID for the Model
     * @return Full URI for the Model
     */
    public String getModelUri(final String datasetUUID, final String modelUUID) {
        String baseUri = getBasePath();
        String datasetUri = baseUri + "/datasets/" + datasetUUID;
        String modelUri = datasetUri + "/models/" + modelUUID;
        return modelUri.replace("\"", "");
    }

}
