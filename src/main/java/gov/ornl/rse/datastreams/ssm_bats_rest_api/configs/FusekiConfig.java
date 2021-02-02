package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Configuration
@ConfigurationProperties(prefix = "fuseki")
/*
 * Represents the Apache Jena Fuseki triple store database config.
*/
public class FusekiConfig {

    /**
     * Hostname for Fuseki server.
    */
    private String hostname;

    /**
     * Port for Fuseki server.
    */
    private int port;

    /**
     * Get host for Fuseki server set by properties.
     *
     * @return Hostname for the Fuseki server
    */
    public final String getHost() {
        return hostname;
    }

    /**
     * Get port for Fuseki server set by properties.
     *
     * @return Port number for the Fuseki server
    */
    public final int getPort() {
        return port;
    }
}
