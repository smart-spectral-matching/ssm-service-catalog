package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Representation of a BATS Dataset for Apache Jena Model.
*/
public class BatsDataset implements UniquelyIdentifiable {
    /**
     * UUID for the Dataset.
    */
    private String uuid;

    /**
     * JSON-LD node that holds the Dataset.
    */
    private JsonNode dataset;

    /**
     * Constructor class to create a BatsDataset object.
     *
     * @param datasetUUID   UUID for the new Dataset
     * @param datasetString Dataset as a string in JSON-LD format
    */
    public BatsDataset(final String datasetUUID, final String datasetString)
        throws
            IOException {
        super();
        this.uuid = datasetUUID;
        setDataset(datasetString);
    }

    // API

    /**
     * Getter for the Dataset's UUID.
     *
     * @return UUID for the Dataset
    */
    @Override
    public String getUUID() {
        return uuid;
    }

    /**
     * Setter for the Dataset's UUID.
     *
     * @param datasetUUID New dataset UUID to set BatsDataset
    */
    public void setUUID(final String datasetUUID) {
        this.uuid = datasetUUID;
    }

    /**
     * Getter for the Dataset.
     *
     * @return Json representation of Dataset as string
    */
    public JsonNode getDataset() {
        return dataset;
    }

    /**
     * Setter for the Dataset.
     *
     * @param datasetString New Dataset to set for BatsDataset
    */
    public final void setDataset(final String datasetString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.dataset = mapper.readTree(datasetString);
    }
}
