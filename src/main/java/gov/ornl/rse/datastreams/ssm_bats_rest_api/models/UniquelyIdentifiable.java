package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

/**
 * Objects that have universally uniquely identifiers.
 *
 * @author Robert Smith
 *
 */
public interface UniquelyIdentifiable {

    /**
     * Getter for the UUID.
     *
     * @return The object's unique identifier.
     */
    String getUUID();
}
