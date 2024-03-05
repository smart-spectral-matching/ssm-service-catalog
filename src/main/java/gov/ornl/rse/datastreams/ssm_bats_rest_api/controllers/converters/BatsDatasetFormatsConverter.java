package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers.converters;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDatasetFormats;

public class BatsDatasetFormatsConverter implements Converter<String, BatsDatasetFormats> {

    /**
     * Overrides convert of String to uppercase for BatsDatasetFormats Enum.
     *
     * @param source Input string to uppercase for conversion
     * @return BatsDatasetFormats value for source
     */
    @Override
    public BatsDatasetFormats convert(final String source) {
        return BatsDatasetFormats.valueOf(source.toUpperCase(Locale.getDefault()));
    }
}
