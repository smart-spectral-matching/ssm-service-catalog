package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

/**
 * Formats to return for Datasets.
 */
public enum BatsDatasetFormats {
    /**
     * Abbreviated JSON format.
     */
    JSON,

    /**
     * JSON-LD format.
     */
    JSONLD,

    /**
     * Full graph format JSON-LD from Apache Jena.
     */
    FULL,

    /**
     * Full graph format JSON-LD from Apache Jena.
     */
    GRAPH
}
