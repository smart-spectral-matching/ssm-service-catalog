package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BatsModel {
    private String uuid;
    private JsonNode model;

    public BatsModel(String uuid, String modelString) throws IOException {
        super();
        this.uuid = uuid;
        setModel(modelString);
    }

    // API

    public String getUUID() {
        return uuid;
    }

    public void setUUID(final String uuid) {
        this.uuid = uuid;
    }

    public JsonNode getModel() {
        return model;
    }

    public void setModel(final String modelString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.model = mapper.readTree(modelString);
    }
}
