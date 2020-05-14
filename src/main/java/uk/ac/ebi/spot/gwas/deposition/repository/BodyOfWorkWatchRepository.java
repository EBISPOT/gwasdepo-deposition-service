package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWorkWatch;

import java.util.Optional;

public interface BodyOfWorkWatchRepository extends MongoRepository<BodyOfWorkWatch, String> {

    Optional<BodyOfWorkWatch> findByBowId(String bowId);

}
