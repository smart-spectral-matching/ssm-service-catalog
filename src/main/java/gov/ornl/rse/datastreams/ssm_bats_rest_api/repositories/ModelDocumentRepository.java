package gov.ornl.rse.datastreams.ssm_bats_rest_api.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.ModelDocument;

public interface ModelDocumentRepository extends MongoRepository<ModelDocument, String> {
    /**
     * Find ModelDocument from repository by ID.
     *
     * @param modelId ID of the ModelDocument
     * @return ModelDocument object for the given ID
     */
    ModelDocument findByModelId(String modelId);
}
