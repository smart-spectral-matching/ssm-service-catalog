package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles({"docker", "prod"})
public class ApplicationConfigProdEnvTest {
    /**
     * Expected port of Fuseki.
     */
    private static final int FUSEKI_PORT = 3030;

    /**
     * Application config.
     */
    @Autowired
    private ApplicationConfig config;

    @Test
    void hostSetByPropertyValue() {
        Assertions.assertEquals("https://ssm.ornl.gov",
            config.getHost());
    }

    @Test
    void fusekiHostnameSetByPropertyValue() {
        Assertions.assertEquals("http://fuseki",
            config.getFuseki().getHostname());
    }

    @Test
    void fusekiPortSetByPropertyValue() {
        Assertions.assertEquals(FUSEKI_PORT,
            config.getFuseki().getPort());
    }
}
