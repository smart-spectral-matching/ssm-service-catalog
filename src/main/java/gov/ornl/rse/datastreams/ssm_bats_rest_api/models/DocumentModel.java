package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import org.bson.Document;
import org.springframework.data.annotation.Id;

public class DocumentModel {

    /**
     * ID for DocumentModel.
     */
    @Id
    private String modelId;

    /**
     * Abbreviated JSON document version of Model.
     */
    private Document json;

    /**
     * JSON-LD document version of Model.
     */
    private Document jsonld;

    /**
     * Constructor class to create an empty DocumentModel object.
     */
    public DocumentModel() { }

    /**
     * Constructor class to create aa DocumentModel object.
     * @param modelId ID for DocumentModel
     * @param json Abbreviated JSON document for Model
     * @param jsonld JSON-LD document for Model
     */
    public DocumentModel(
        final String modelId,
        final String json,
        final String jsonld
    ) {
        this.modelId = modelId;
        this.json = Document.parse(json);
        this.jsonld = Document.parse(jsonld);
    }


    // API

    /**
     * Getter for the DocumentModel's ID.
     *
     * @return ID for the DocumentModel
    */
    public String getModelId() {
        return this.modelId;
    }

    /**
     * Setter for the DocumentModel's ID.
     *
     * @param modelId New ID for DocumentModel
    */
    public void setModelId(final String modelId) {
        this.modelId = modelId;
    }

    /**
     * Getter for the DocumentModel's abbreviated json document.
     *
     * @return Abbreviated JSON document for the DocumentModel
    */
    public String getJson() {
        return this.json.toJson();
    }

    /**
     * Setter for the DocumentModel's abbreviated json document.
     *
     * @param json New abbreviated json document for DocumentModel
    */
    public void setJson(final String json) {
        this.json = Document.parse(json);
    }

    /**
     * Getter for the DocumentModel's JSON-LD document.
     *
     * @return JSON-LD document for the DocumentModel
    */
    public String getJsonld() {
        return this.jsonld.toJson();
    }

    /**
     * Setter for the DocumentModel's JSON-LD document.
     *
     * @param jsonld New JSON-LD document for DocumentModel
    */
    public void setJsonld(final String jsonld) {
        this.jsonld = Document.parse(jsonld);
    }
}
