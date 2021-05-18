package uk.ac.ebi.spot.gwas.deposition.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.Sample;

import java.util.List;
import java.util.stream.Stream;


public interface SampleRepository extends MongoRepository<Sample, String> {

    Stream<Sample> readBySubmissionId(String submissionId);

    Page<Sample> findBySubmissionId(String submissionId, Pageable page);

    List<Sample> findByIdIn(List<String> ids);
}
