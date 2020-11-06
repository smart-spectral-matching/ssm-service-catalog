package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

public class BatsModel {
    private String uuid;
    private String model;

    public BatsModel(String uuid, String model) {
        super();
        this.uuid = uuid;
        this.model = model;
    }

    // API

    public String getUUID() {
        return uuid;
    }

    public void setUUID(final String uuid) {
        this.uuid = uuid;
    }

    public String getModel() {
        return model;
    }

    public void setURI(final String model) {
        this.model = model;
    }
}
