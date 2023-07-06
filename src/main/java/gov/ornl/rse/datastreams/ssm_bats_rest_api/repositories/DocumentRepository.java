package gov.ornl.rse.datastreams.ssm_bats_rest_api.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.DocumentModel;

public interface DocumentRepository extends MongoRepository<DocumentModel, String> {
    /**
     * Find DocumentModel from repository by ID.
     *
     * @param modelId ID of the DocumentModel
     * @return DocumentModel object for the given ID
     */
    DocumentModel findByModelId(String modelId);
}
