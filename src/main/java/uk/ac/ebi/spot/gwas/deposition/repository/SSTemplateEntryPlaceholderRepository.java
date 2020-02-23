package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.SSTemplateEntryPlaceholder;

import java.util.Optional;

public interface SSTemplateEntryPlaceholderRepository extends MongoRepository<SSTemplateEntryPlaceholder, String> {

    Optional<SSTemplateEntryPlaceholder> findByPmid(String pmid);
}
