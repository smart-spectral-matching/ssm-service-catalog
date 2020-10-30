package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import org.json.JSONObject;

import org.apache.jena.query.Dataset;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.ornl.rse.bats.DataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.UUIDGenerator;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.RdfModelWriter;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDataset;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsFusekiInfo;

@RestController
@RequestMapping("/datasets")
public class BatsDatasetController {
    private static final Logger logger = LoggerFactory.getLogger(BatsDatasetController.class);
    
    private String hostname = "http://localhost";

    // CREATE
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public BatsDataset  createDataSet() throws Exception {
        String uuid = UUIDGenerator.generateUUID();
        DataSet dataset = new DataSet();
        dataset.setName(uuid);
        dataset.setHost(hostname);
        dataset.create();

        BatsFusekiInfo info = new BatsFusekiInfo(dataset);
        return new BatsDataset(uuid, info);
    }

    // READ
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    @ResponseBody
    public BatsDataset  getDataSet(@PathVariable("uuid") String uuid) throws ResponseStatusException {
        DataSet dataset = new DataSet();
        dataset.setName(uuid);
        dataset.setHost(hostname);

        Dataset contents = dataset.getJenaDataset();
        if ( contents == null ) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DataSet Not Found");
        }

        BatsFusekiInfo info = new BatsFusekiInfo(dataset);
        return new BatsDataset(uuid, info);
    }

    //UPDATE

    //DELETE
    @RequestMapping(value = "/{uuid}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDataSet(@PathVariable("uuid") String uuid) throws Exception {
        DataSet dataset = new DataSet();
        dataset.setName(uuid);
        dataset.setHost(hostname);
        dataset.delete();
    }
}
