package ssm.catalog.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ssm.catalog.models.DatasetDocument;

public interface DatasetDocumentRepository extends MongoRepository<DatasetDocument, String> {
    /**
     * Find DatasetDocument from repository by ID.
     *
     * @param datasetId ID of the DatasetDocument
     * @return DatasetDocument object for the given ID
     */
    DatasetDocument findByDatasetId(String datasetId);
}
