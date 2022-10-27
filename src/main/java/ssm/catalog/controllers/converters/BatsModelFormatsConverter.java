package ssm.catalog.controllers.converters;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;

import ssm.catalog.models.DatasetFormats;

public class BatsModelFormatsConverter implements Converter<String, DatasetFormats> {

    /**
     * Overrides convert of String to uppercase for BatsModelFormats Enum.
     *
     * @param source Input string to uppercase for conversion
     * @return BatsModelFormats value for source
     */
    @Override
    public DatasetFormats convert(final String source) {
        return DatasetFormats.valueOf(source.toUpperCase(Locale.getDefault()));
    }
}
