package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import org.bson.Document;
import org.springframework.data.annotation.Id;

public class ModelDocument {

    /**
     * ID for ModelDocument.
     */
    @Id
    private String id;

    /**
     * Abbreviated JSON document version of Model.
     */
    private Document modelJson;

    /**
     * JSON-LD document version of Model.
     */
    private Document modelJsonld;

    /**
     * Constructor class to create an empty ModelDocument object.
     */
    public ModelDocument() { }

    /**
     * Constructor class to create aa ModelDocument object.
     * @param id ID for ModelDocument
     * @param modelJson Abbreviated JSON document for Model
     * @param modelJsonld JSON-LD document for Model
     */
    public ModelDocument(final String id, final String modelJson, final String modelJsonld) {
        this.id = id;
        this.modelJson = Document.parse(modelJson);
        this.modelJsonld = Document.parse(modelJsonld);
    }


    // API

    /**
     * Getter for the ModelDocument's ID.
     *
     * @return ID for the ModelDocument
    */
    public String getId() {
        return id;
    }

    /**
     * Setter for the ModelDocument's ID.
     *
     * @param id New ID for ModelDocument
    */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Getter for the ModelDocument's abbreviated json document.
     *
     * @return Abbreviated JSON document for the ModelDocument
    */
    public String getModelJson() {
        return this.modelJson.toJson();
    }

    /**
     * Setter for the ModelDocument's abbreviated json document.
     *
     * @param modelJson New abbreviated json document for ModelDocument
    */
    public void setModelJson(final String modelJson) {
        this.modelJson = Document.parse(modelJson);
    }

    /**
     * Getter for the ModelDocument's JSON-LD document.
     *
     * @return JSON-LD document for the ModelDocument
    */
    public String getModelJsonld() {
        return this.modelJsonld.toJson();
    }

    /**
     * Setter for the ModelDocument's JSON-LD document.
     *
     * @param modelJsonld New JSON-LD document for ModelDocument
    */
    public void setModelJsonld(final String modelJsonld) {
        this.modelJsonld = Document.parse(modelJsonld);
    }
}
