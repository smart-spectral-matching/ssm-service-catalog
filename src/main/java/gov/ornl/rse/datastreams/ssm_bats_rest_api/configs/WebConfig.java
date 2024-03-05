package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers.converters.BatsDatasetFormatsConverter;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Overrides addFormatters to add the BatsDatasetFormatsConverter.
     *
     * @param registry Formatter registry to add converters to
     */
    @Override
    public void addFormatters(final FormatterRegistry registry) {
        registry.addConverter(new BatsDatasetFormatsConverter());
    }
}
