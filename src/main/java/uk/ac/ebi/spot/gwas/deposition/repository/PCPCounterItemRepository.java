package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.PCPCounterItem;

import java.util.Optional;

public interface PCPCounterItemRepository extends MongoRepository<PCPCounterItem, String> {
    Optional<PCPCounterItem> findFirstBy();
}
