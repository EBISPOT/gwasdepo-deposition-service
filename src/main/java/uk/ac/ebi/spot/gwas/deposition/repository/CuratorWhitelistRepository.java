package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.CuratorWhitelist;

import java.util.Optional;

public interface CuratorWhitelistRepository extends MongoRepository<CuratorWhitelist, String> {
    Optional<CuratorWhitelist> findByEmailIgnoreCase(String enail);
}
