package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    /**
     * Custom configuration of the API selector.
     *
     * @return Docket for custom API selector
    */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
          .select()
          .apis(
            RequestHandlerSelectors.basePackage(
                "gov.ornl.rse.datastreams.ssm_bats_rest_api"
            )
          )
          .paths(PathSelectors.any())
          .build()
          .apiInfo(apiInfo());
    }

    /**
     * Custom configuration of the API docs header page.
     *
     * @return ApiInfo object for API docs page
    */
    private ApiInfo apiInfo() {
        return new ApiInfo(
            "Smart Spectral Matching Storage REST API",
            "API for storage of Spectroscopy Datasets for Materials Research",
            "0.0.1",
            "Terms of Service URL",
            new Contact(
                "Marshall McDonnell",
                "https://www.ornl.gov/staff-profile/marshall-t-mcdonnell",
                "mcdonnellmt@ornl.gov"),
            "License of API",
            "API license URL",
             Collections.emptyList());
    }
}
