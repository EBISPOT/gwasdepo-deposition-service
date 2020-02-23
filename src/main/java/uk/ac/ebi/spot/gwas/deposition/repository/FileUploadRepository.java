package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;

import java.util.List;
import java.util.Optional;

public interface FileUploadRepository extends MongoRepository<FileUpload, String> {
    List<FileUpload> findByIdIn(List<String> ids);

    Optional<FileUpload> findByCallbackId(String callbackId);
}
