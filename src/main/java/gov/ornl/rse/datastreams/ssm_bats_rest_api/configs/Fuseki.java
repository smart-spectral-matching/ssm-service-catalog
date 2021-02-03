package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "fuseki")
@ConstructorBinding
@AllArgsConstructor
/*
 * Represents the Apache Jena Fuseki triple store database config.
*/
public class Fuseki {

    /**
     * Hostname for Fuseki server.
     *
     * @return Current Fuseki server hostname
    */
    @Getter private String hostname;

    /**
     * Port for Fuseki server.
    */
    @Getter private int port;
}
