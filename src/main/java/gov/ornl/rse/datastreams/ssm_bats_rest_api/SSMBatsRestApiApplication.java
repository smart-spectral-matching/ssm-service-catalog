package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main class for Spring App - BATS REST API.
*/
@SpringBootApplication
public class SSMBatsRestApiApplication {
    /**
     * Main method.
     *
     * @param args Arguments for SprintApplication run
    */
    public static void main(final String[] args) {
        SpringApplication.run(SSMBatsRestApiApplication.class, args);
    }
}
