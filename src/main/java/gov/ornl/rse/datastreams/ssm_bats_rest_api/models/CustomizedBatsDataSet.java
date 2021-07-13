package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import java.util.Locale;

import gov.ornl.rse.bats.DataSet;

public class CustomizedBatsDataSet extends DataSet {

    /**
     * Overrides the setName method of DataSet to always be lower-case.
     *
     * @param name
    */
    @Override
    public void setName(final String name) {
        super.setName(name.toLowerCase(new Locale("en")));
    }
}
