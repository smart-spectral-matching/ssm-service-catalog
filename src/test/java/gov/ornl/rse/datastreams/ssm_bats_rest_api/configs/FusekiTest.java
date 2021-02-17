package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FusekiTest {

    /**
     * Target port for test results.
    */
    private static final int PORT = 8888;

    /**
     * Target hostname for test results.
    */
    private static final String HOSTNAME = "localhost";

    /**
     * Application context runner.
    */
    private ApplicationContextRunner applicationContextRunner;


    @BeforeEach
    void setup() {
        applicationContextRunner =
                new ApplicationContextRunner()
                        .withConfiguration(
                            AutoConfigurations.of(FusekiConfig.class));
    }

    @Test
    void serverBeanExists() {
        applicationContextRunner
            .run(
                context -> Assertions.assertThat(context)
                                     .hasSingleBean(Fuseki.class));
    }

    @Test
    void hostnameSetByPropertyValue() {
        applicationContextRunner
            .withPropertyValues("fuseki.hostname=" + HOSTNAME)
            .run(
                context ->
                    Assertions.assertThat(
                            context.getBean(Fuseki.class).getHostname()
                        ).isEqualTo(HOSTNAME));
    }

    @Test
    void portSetByPropertyValue() {
        applicationContextRunner
            .withPropertyValues("fuseki.port=" + PORT)
            .run(
                context -> Assertions.assertThat(
                    context.getBean(Fuseki.class)
                           .getPort()
                ).isEqualTo(PORT));
    }

}
