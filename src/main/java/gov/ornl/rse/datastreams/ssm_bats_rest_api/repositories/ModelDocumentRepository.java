package gov.ornl.rse.datastreams.ssm_bats_rest_api.repositories;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.ModelDocument;

public interface ModelDocumentRepository extends MongoRepository<ModelDocument, String> {
    /**
     * Find ModelDocument from repository by ID.
     *
     * @param id ID of the ModelDocument
     * @return ModelDocument object for the given ID
     */
    ModelDocument findById(ObjectId id);
}
