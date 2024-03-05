package gov.ornl.rse.datastreams.ssm_bats_rest_api.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.DocumentDataset;

public interface DocumentRepository extends MongoRepository<DocumentDataset, String> {
    /**
     * Find DocumentDataset from repository by ID.
     *
     * @param datasetId ID of the DocumentDataset
     * @return DocumentDataset object for the given ID
     */
    DocumentDataset findByDatasetId(String datasetId);
}
