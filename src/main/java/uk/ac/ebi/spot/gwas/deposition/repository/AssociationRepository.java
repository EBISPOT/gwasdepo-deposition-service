package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.Association;

import java.util.List;

public interface AssociationRepository extends MongoRepository<Association, String> {

    Page<Association> findBySubmissionId(String submissionId, Pageable page);

    List<Association> findByIdIn(List<String> ids);
}
