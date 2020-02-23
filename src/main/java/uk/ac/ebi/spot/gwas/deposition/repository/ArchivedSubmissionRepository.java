package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.ArchivedSubmission;

import java.util.List;

public interface ArchivedSubmissionRepository extends MongoRepository<ArchivedSubmission, String> {

    List<ArchivedSubmission> findBySubmissionId(String submissionId);
}
