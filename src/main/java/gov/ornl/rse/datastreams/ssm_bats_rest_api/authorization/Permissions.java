package gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization;

public enum Permissions {
    
    /**
     * Permission to associate/dissaciate datasets from collections.
     */
    ASSOCIATE,
    
    /**
     * Permission to upload new data or create a sub-collection.
     */
    CREATE,
    
    /**
     * Permission to delete data.
     */
    DELETE,
    
    /**
     * Permission to grant others the ASSOCIATE permission.
     */
    GRANT_ASSOCIATE,
    
    /**
     * Permission to grant others the CREATE permission.
     */
    GRANT_CREATE,
    
    /**
     * Permission to grant others the DELETE permission.
     */
    GRANT_DELETE,
    
    /**
     * Permission to grant others the READ permission.
     */
    GRANT_READ,
    
    /**
     * Permission to grant others the READ permission.
     */
    GRANT_UPDATE,
    
    /**
     * Permission to view the data.
     */
    READ,
    
    /**
     * Permission to update the data.
     */
    UPDATE
}
