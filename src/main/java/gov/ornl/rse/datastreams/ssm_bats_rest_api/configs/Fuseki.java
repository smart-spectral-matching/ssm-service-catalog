package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import lombok.AllArgsConstructor;

@ConfigurationProperties(prefix = "fuseki")
@ConstructorBinding
@AllArgsConstructor
/*
 * Represents the Apache Jena Fuseki triple store database config.
*/
public class Fuseki {

    /**
     * Hostname for Fuseki server.
     */
    private String hostname;

    /**
     * @return Fuseki server hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Port for Fuseki server.
    */
    private int port;

    /**
     * @return Fuseki server port
     */
    public int getPort() {
        return port;
    }
}
