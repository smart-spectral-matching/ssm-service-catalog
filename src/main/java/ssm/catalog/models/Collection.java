package ssm.catalog.models;

import javax.validation.constraints.Pattern;

/**
 * Representation of a Collection for Apache Jena Collection
 * A Collection holds a collection of Datasets.
*/

public class Collection {
    /**
     * Valid regex for the Collection title.
    */
    public static final String TITLE_REGEX = "^[A-Za-z-]+$";

    /**
     * Title for the Collection.
    */
    @Pattern(regexp = TITLE_REGEX)
    private String title;

    /**
     * Default constructor to create a Collection object.
    */
    public Collection() {
    }

    /**
     * Constructor class to create a Collection object.
     *
     * @param title Title for the new Collection
    */
    public Collection(final String title) {
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
     * @param title New Collection title to set Collection
    */

    public void setTitle(final String title) {
        this.title = title;
    }
}
