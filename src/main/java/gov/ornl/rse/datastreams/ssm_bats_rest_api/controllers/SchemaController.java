package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller which can serve JSON-LD schema files as needed.
 */
@RestController
@RequestMapping("/schemas")
@Validated
public class SchemaController {

    /**
     * Spring application context.
     */
    @Autowired
    private ApplicationContext appContext;

    /**
     * The metadata schema should be merged with all other schemas with a model.
     * This endpoint serves as a reference.
     *
     * @return the Metadata schema from JSONLD
     */
    @GetMapping("/metadata")
    public ResponseEntity<Resource> getMetadataSchema() {
        // TODO this can probably be cached later on
        return ResponseEntity.ok(appContext.getResource(
            "classpath:schemas/bats-metadata-schema.jsonld"
        ));
    }

}
