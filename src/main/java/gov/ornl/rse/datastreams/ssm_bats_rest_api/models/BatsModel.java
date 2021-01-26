package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Representation of a BATS Model for Apache Jena Model.
*/
public class BatsModel {
    /**
     * UUID for the Model.
    */
    private String uuid;

    /**
     * JSON-LD node that holds the Model.
    */
    private JsonNode model;

    /**
     * Constructor class to create a BatsModel object.
     *
     * @param modelUUID   UUID for the new Model
     * @param modelString Model as a string in JSON-LD format
    */
    public BatsModel(final String modelUUID, final String modelString)
        throws
            IOException {
        super();
        this.uuid = modelUUID;
        setModel(modelString);
    }

    // API

    /**
     * Getter for the Model's UUID.
     *
     * @return UUID for the Model
    */
    public String getUUID() {
        return uuid;
    }

    /**
     * Setter for the Model's UUID.
     *
     * @param modelUUID New model UUID to set BatsModel
    */
    public void setUUID(final String modelUUID) {
        this.uuid = modelUUID;
    }

    /**
     * Getter for the Model.
     *
     * @return Json representation of Model as string
    */
    public JsonNode getModel() {
        return model;
    }

    /**
     * Setter for the Model.
     *
     * @param modelString New Model to set for BatsModel
    */
    public void setModel(final String modelString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.model = mapper.readTree(modelString);
    }
}
