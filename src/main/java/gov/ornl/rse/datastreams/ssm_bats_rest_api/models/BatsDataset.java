package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsFusekiInfo;

public class BatsDataset {
    private String uuid;
    private String location;
    private BatsFusekiInfo info;

    public BatsDataset(String uuid, BatsFusekiInfo info) {
        super();
        this.uuid = uuid;
        this.info = info;
    }

    // API

    public String getUUID() {
        return uuid;
    }

    public void setUUID(final String uuid) {
        this.uuid = uuid;
    }

    public BatsFusekiInfo getFusekiInfo() {
        return info;
    }

    public void setFusekiInfo(final BatsFusekiInfo info) {
        this.info = info;
    }
}