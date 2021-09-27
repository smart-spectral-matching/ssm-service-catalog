package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers.converters;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsModelFormats;

public class BatsModelFormatsConverter implements Converter<String, BatsModelFormats> {

    /**
     * Overrides convert of String to uppercase for BatsModelFormats Enum.
     *
     * @param source Input string to uppercase for conversion
     * @return BatsModelFormats value for source
     */
    @Override
    public BatsModelFormats convert(final String source) {
        return BatsModelFormats.valueOf(source.toUpperCase(Locale.getDefault()));
    }
}
