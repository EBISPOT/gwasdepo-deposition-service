package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.Sample;

import java.util.List;

public interface SampleRepository extends MongoRepository<Sample, String> {

    Page<Sample> findBySubmissionId(String submissionId, Pageable page);

    List<Sample> findByIdIn(List<String> ids);
}
