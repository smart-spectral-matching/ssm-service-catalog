package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

/**
 * The type of conversion to use for JSON-LD -> SSM JSON.
 */
public enum JsonConversionType {

    /**
     * EMBEDDED - Use the internal AbbreviatedJson class to conver.
     * FILE_CONVERTER_SERVICE - Use the external file converter service REST API.
    */
    EMBEDDED, FILE_CONVERTER_SERVICE
}
