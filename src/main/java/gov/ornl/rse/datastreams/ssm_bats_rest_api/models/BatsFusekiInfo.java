package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import gov.ornl.rse.bats.DataSet;

public class BatsFusekiInfo {
    private String hostname;
    private int port;
    private String uri;

    public BatsFusekiInfo(DataSet dataset) {
        super();
        this.hostname = dataset.getHost();
        this.port = dataset.getPort();
        this.uri = dataset.getFullURI();
    }

    // API

    public String getHost() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getURI() {
        return uri;
    }
}
