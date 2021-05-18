package uk.ac.ebi.spot.gwas.deposition.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.Association;

import java.util.List;
import java.util.stream.Stream;


public interface AssociationRepository extends MongoRepository<Association, String> {

    Stream<Association> readBySubmissionId(String submissionId);

    Page<Association> findBySubmissionId(String submissionId, Pageable page);

    List<Association> findByIdIn(List<String> ids);
}
