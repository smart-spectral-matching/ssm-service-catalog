package gov.ornl.rse.datastreams.ssm_bats_rest_api.repositories;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.ModelDocument;

public interface ModelDocumentRepository extends MongoRepository<ModelDocument, String>{
    ModelDocument findBy_id(ObjectId _id);
}
