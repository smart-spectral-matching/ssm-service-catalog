package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import org.apache.jena.query.Dataset;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.ornl.rse.bats.DataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.UUIDGenerator;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.RdfModelWriter;

@RestController
@RequestMapping("/datasets")
public class BatsDatasetsController {
    private static final Logger logger = LoggerFactory.getLogger(BatsDatasetsController.class);
    
    private String hostname = "http://rse-nds-dev1.ornl.gov";

    // GET
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    public String  getDataSetById(@PathVariable("uuid") String uuid) {
        DataSet dataset = new DataSet();
        dataset.setName(uuid);
        dataset.setHost(hostname);

        Dataset contents = dataset.getJenaDataset();
        if ( contents == null ) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DataSet Not Found");
        }
        return "{\"UUID\" : \" " + uuid + " \"}";
    }

    // POST
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<String>  createDataset() throws Exception {
        DataSet dataset = new DataSet();
        String uuid = UUIDGenerator.generateUUID();
        dataset.setName(uuid);
        dataset.setHost(hostname);
        dataset.create();
        return new ResponseEntity<String>("{\"UUID\" : \"" + uuid + "\"}", HttpStatus.CREATED);
    }
}
