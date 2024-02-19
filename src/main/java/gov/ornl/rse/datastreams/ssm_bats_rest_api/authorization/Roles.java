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
    COLLABORATOR(0),

    /**
     * The second highest role, for someone who can edit data and also promote other users.
     */
    MAINTAINER(2),

    /**
     * The third highest role, for someone who can edit data.
     */
    MEMBER(1),

    /**
     * The highest role, representing full permissions over the data.
     */
    OWNER(4);

    /**
     * The number of hierarchical ranks below a role.
     */
    private final int rank;

    /**
     * The default constructor.
     *
     * @param rank The Role's rank.
     */
    Roles(final int rank) {
        this.rank = rank;
    }

    /**
     * Getter for the rank.
     *
     * @return The role's rank.
     */
    public int getRank() {
        return rank;
    }
}
