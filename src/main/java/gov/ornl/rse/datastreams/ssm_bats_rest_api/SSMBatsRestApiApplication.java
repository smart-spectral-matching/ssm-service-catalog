package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ConfigUtils;

/**
 * Main class for Spring App - BATS REST API.
 */
@SpringBootApplication
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
        SpringApplication.run(SSMBatsRestApiApplication.class, args);
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
              + "Use only one of 'dev', 'qa', or 'prod'.");
            System.exit(1);
        }
    }

}
