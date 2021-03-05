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
        "dev", "qa", "prod"
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
            .toArray(String[]::new)
            .length > 1;
    }

    /**
     * @return host path from configuration + the base context path
     */
    public String getBasePath() {
        return config.getHost() + servletContext.getContextPath();
    }

}
