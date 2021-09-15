package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

public class ModelDocument {

    @Id
    public ObjectId _id;

    public String modelJson;
    public String modelJsonld;

    public ModelDocument () {}

    public ModelDocument(ObjectId _id, String modelJson, String modelJsonld) {
        this._id = _id;
        this.modelJson = modelJson;
        this.modelJsonld = modelJsonld;
    }

    public String get_id() { return _id.toHexString(); }
    public void set_id(ObjectId _id) { this._id = _id; }

    public String getModelJson() { return this.modelJson; }
    public void setModelJson(String modelJson) { this.modelJson = modelJson; }

    public String getModelJsonld() { return this.modelJsonld; }
    public void setModelJsonld(String modelJsonld) { this.modelJsonld = modelJsonld; }
    
}
