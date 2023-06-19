package gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Java representation of a Zanzibar relationship.
 *
 * @author Robert Smith
 *
 */
public class Relationship {

    /**
     * Namespace the relationship exists in.
     */
    private String namespace;

    /**
     * Object the relationship describes a permission for.
     */
    private String object;
    
    private String relation;

    /**
     * Subject the relationship grants a permission to.
     */
    private String subjectId;

    /**
     * Zanzibar subject set. Subject set should always have subject_id == null.
     */
    private Relationship subjectSet;

    /**
     * The default construtor.
     */
    public Relationship() {
        namespace = null;
        object = null;
        relation = null;
        subjectId = null;
        subjectSet = null;
    }

    /**
     * Getter for namespace.
     *
     * @return The namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Setter for namespace.
     *
     * @param namespace
     */
    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    /**
     * Getter for object.
     *
     * @return The object
     */
    public String getObject() {
        return object;
    }

    /**
     * Setter for object.
     *
     * @param object
     */
    public void setObject(final String object) {
        this.object = object;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    /**
     * Getter for subject_id.
     *
     * @return The subject_id
     */
    @JsonProperty("subject_id")
    public String getSubjectId() {
        return subjectId;
    }

    /**
     * Setter for subject_id.
     *
     * @param subjectId
     */
    public void setSubjectId(final String subjectId) {
        this.subjectId = subjectId;
    }

    /**
     * Getter for subject set.
     *
     * @return The subject_set
     */
    @JsonProperty("subject_set")
    public Relationship getSubjectSet() {
        return subjectSet;
    }

    /**
     * Setter for subject set.
     *
     * @param subjectSet
     */
    public void setSubjectSet(final Relationship subjectSet) {
        this.subjectSet = subjectSet;
    }
}
