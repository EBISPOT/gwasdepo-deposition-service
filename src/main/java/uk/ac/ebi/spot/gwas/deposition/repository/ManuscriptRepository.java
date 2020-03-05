package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.Manuscript;

import java.util.Optional;

public interface ManuscriptRepository extends MongoRepository<Manuscript, String> {

    Optional<Manuscript> findByIdAndArchivedAndCreated_UserId(String id, boolean archived, String userId);

    Page<Manuscript> findByArchivedAndCreated_UserId(boolean archived, String userId, Pageable page);

}
