package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

/**
 * Representation of a BATS Dataset for Apache Jena Dataset
 * A Dataset holds a collection of Models.
*/

public class BatsDataset {
    /**
     * Title for the Dataset.
    */
    private String title;

    /**
     * Default constructor to create a BatsDataset object.
    */
    public BatsDataset() {
    }

    /**
     * Constructor class to create a BatsDataset object.
     *
     * @param title Title for the new Dataset
    */
    public BatsDataset(final String title) {
        super();
        this.title = title;
    }

    /**
     * Getter for the Dataset's title.
     *
     * @return Title for the Dataset
    */
    public String getTitle() {
        return title;
    }

    /**
     * Setter for the Dataset's UUID.
     *
     * @param title New dataset title to set BatsDataset
    */

    public void setTitle(final String title) {
        this.title = title;
    }
}
