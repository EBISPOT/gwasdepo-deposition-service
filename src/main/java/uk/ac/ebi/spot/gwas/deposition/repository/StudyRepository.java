package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;

import java.util.List;

public interface StudyRepository extends MongoRepository<Study, String> {

    Page<Study> findBySubmissionId(String submissionId, Pageable page);

    List<Study> findByIdIn(List<String> ids);
}
