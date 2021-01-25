package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

/**
 * Representation of a BATS Dataset for Apache Jena Dataset
 * A Dataset holds a collection of Models.
*/

public class BatsDataset {
    /**
     * UUID for the Dataset.
    */
    private String uuid;

    /**
     * URI for the Dataset in Fuseki server.
     * TODO: Remove this field
    */
    private String uri;

    /**
     * Constructor class to create a BatsDataset object.
     *
     * @param datasetUUID UUID for the new Dataset
     * @param datasetURI  URI for the new Dataset in Fuseki database
    */
    public BatsDataset(final String datasetUUID, final String datasetURI) {
        super();
        this.uuid = datasetUUID;
        this.uri = datasetURI;
    }

    // API

    /**
     * Getter for the Dataset's UUID.
     *
     * @return UUID for the Dataset
    */
    public String getUUID() {
        return uuid;
    }

    /**
     * Setter for the Dataset's UUID.
     *
     * @param datasetUUID New dataset UUID to set BatsDataset
    */

    public void setUUID(final String datasetUUID) {
        this.uuid = datasetUUID;
    }

    /**
     * Getter for the Dataset URI.
     *
     * @return URI for the dataset in Fuseki database
    */
    public String getURI() {
        return uri;
    }

    /**
     * Setter for the Dataset URI.
     *
     * @param datasetURI New URI for Dataset in Fuseki database
    */
    public void setURI(final String datasetURI) {
        this.uri = datasetURI;
    }
}
