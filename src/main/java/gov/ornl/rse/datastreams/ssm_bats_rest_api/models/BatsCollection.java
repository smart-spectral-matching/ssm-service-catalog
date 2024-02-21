package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import javax.validation.constraints.Pattern;

/**
 * Representation of a BATS Collection for Apache Jena Collection
 * A Collection holds a collection of Models.
*/

public class BatsCollection {
    /**
     * Valid regex for the BatsCollection title.
    */
    public static final String TITLE_REGEX = "^[A-Za-z-]+$";

    /**
     * Title for the Collection.
    */
    @Pattern(regexp = TITLE_REGEX)
    private String title;

    /**
     * Default constructor to create a BatsCollection object.
    */
    public BatsCollection() {
    }

    /**
     * Constructor class to create a BatsCollection object.
     *
     * @param title Title for the new Collection
    */
    public BatsCollection(final String title) {
        super();
        this.title = title;
    }

    /**
     * Getter for the Collection's title.
     *
     * @return Title for the Collection
    */
    public String getTitle() {
        return title;
    }

    /**
     * Setter for the Collection's UUID.
     *
     * @param title New collection title to set BatsCollection
    */

    public void setTitle(final String title) {
        this.title = title;
    }
}
