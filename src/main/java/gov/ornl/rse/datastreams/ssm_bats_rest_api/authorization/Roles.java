package gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization;

/**
 * Authorization roles that represent sets of permissions.
 * 
 * @author Robert Smith
 *
 */
public enum Roles {

    /**
     * The lowest role, for someone who is only allowed to view data.
     */
    COLLABORATOR,
    
    /**
     * The second highest role, for someone who can edit data and also promote other users.
     */
    MAINTAINER,
    
    /**
     * The third highest role, for someone who can edit data.
     */
    MEMBER,
    
    /**
     * The highest role, representing full permissions over the data.
     */
    OWNER,
}
