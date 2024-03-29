package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ConfigUtils;

/**
 * Main class for Spring App - BATS REST API.
 */
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class SSMBatsRestApiApplication {

    /**
     * logger.
     */
    private static final Logger LOG = LoggerFactory
        .getLogger(SSMBatsRestApiApplication.class);

    /**
     * Configuration utilities.
     */
    @Autowired
    private ConfigUtils configUtils;

    /**
     * Main method.
     *
     * @param args Arguments for SprintApplication run
     */
    public static void main(final String[] args) {
        SpringApplication app = new SpringApplication(SSMBatsRestApiApplication.class);
        Map<String, Object> springDefaults = new HashMap<>();
        /*
         * activate the local profile if no other profiles are defined
         * note: this can NOT be set in application.properties
         */
        springDefaults.put("spring.profiles.default", "local");
        app.setDefaultProperties(springDefaults);
        app.run(args);
    }

    /**
     * Execute automatically after startup.
     * Determine if the profiles were configured improperly,
     * and immediately terminate the program if so.
     */
    @PostConstruct
    private void init() {
        if (configUtils.isConfigurationError()) {
            LOG.error("ERROR: improper profile configuration.\n"
              + "Use only one of 'local', 'dev', 'qa', or 'prod'.");
            System.exit(1); //NOPMD
        }
    }

}
