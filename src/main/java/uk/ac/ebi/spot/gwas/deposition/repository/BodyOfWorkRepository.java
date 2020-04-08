package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;

import java.util.Optional;

public interface BodyOfWorkRepository extends MongoRepository<BodyOfWork, String> {

    Optional<BodyOfWork> findByBowIdAndArchivedAndCreated_UserId(String bowId, boolean archived, String userId);

    Page<BodyOfWork> findByArchivedAndCreated_UserId(boolean archived, String userId, Pageable page);

}
