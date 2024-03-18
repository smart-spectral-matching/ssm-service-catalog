package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import org.bson.Document;
import org.springframework.data.annotation.Id;

public class DocumentDataset {

    /**
     * ID for DocumentDataset.
     */
    @Id
    private String datasetId;

    /**
     * Abbreviated JSON document version of Dataset.
     */
    private Document json;

    /**
     * JSON-LD document version of Dataset.
     */
    private Document jsonld;

    /**
     * Constructor class to create an empty DocumentDataset object.
     */
    public DocumentDataset() { }

    /**
     * Constructor class to create a DocumentDataset object.
     * @param datasetId ID for DocumentDataset
     * @param json Abbreviated JSON document for Dataset
     * @param jsonld JSON-LD document for Dataset
     */
    public DocumentDataset(
        final String datasetId,
        final String json,
        final String jsonld
    ) {
        this.datasetId = datasetId;
        this.json = Document.parse(json);
        this.jsonld = Document.parse(jsonld);
    }


    // API

    /**
     * Getter for the DocumentDataset's ID.
     *
     * @return ID for the DocumentDataset
    */
    public String getDatasetId() {
        return this.datasetId;
    }

    /**
     * Setter for the DocumentDataset's ID.
     *
     * @param datasetId New ID for DocumentDataset
    */
    public void setDatasetId(final String datasetId) {
        this.datasetId = datasetId;
    }

    /**
     * Getter for the DocumentDataset's abbreviated json document.
     *
     * @return Abbreviated JSON document for the DocumentDataset
    */
    public String getJson() {
        return this.json.toJson();
    }

    /**
     * Setter for the DocumentDataset's abbreviated json document.
     *
     * @param json New abbreviated json document for DocumentDataset
    */
    public void setJson(final String json) {
        this.json = Document.parse(json);
    }

    /**
     * Getter for the DocumentDataset's JSON-LD document.
     *
     * @return JSON-LD document for the DocumentDataset
    */
    public String getJsonld() {
        return this.jsonld.toJson();
    }

    /**
     * Setter for the DocumentDataset's JSON-LD document.
     *
     * @param jsonld New JSON-LD document for DocumentDataset
    */
    public void setJsonld(final String jsonld) {
        this.jsonld = Document.parse(jsonld);
    }
}
