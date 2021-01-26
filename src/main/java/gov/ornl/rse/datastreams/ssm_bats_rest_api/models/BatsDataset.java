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
     * Constructor class to create a BatsDataset object.
     *
     * @param datasetUUID UUID for the new Dataset
    */
    public BatsDataset(final String datasetUUID) {
        super();
        this.uuid = datasetUUID;
    }

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
}
