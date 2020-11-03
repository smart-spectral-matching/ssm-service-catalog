package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

public class BatsDataset {
    private String uuid;
    private String uri;

    public BatsDataset(String uuid, String uri) {
        super();
        this.uuid = uuid;
        this.uri = uri;
    }

    // API

    public String getUUID() {
        return uuid;
    }

    public void setUUID(final String uuid) {
        this.uuid = uuid;
    }

    public String getURI() {
        return uri;
    }

    public void setURI(final String uri) {
        this.uri = uri;
    }
}
