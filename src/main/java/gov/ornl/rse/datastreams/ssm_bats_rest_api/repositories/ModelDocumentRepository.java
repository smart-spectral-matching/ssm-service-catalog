package ssm.catalog.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ssm.catalog.models.ModelDocument;

public interface ModelDocumentRepository extends MongoRepository<ModelDocument, String> {
    /**
     * Find ModelDocument from repository by ID.
     *
     * @param modelId ID of the ModelDocument
     * @return ModelDocument object for the given ID
     */
    ModelDocument findByModelId(String modelId);
}
