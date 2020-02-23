package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.PublicationIngestEntry;

import java.util.List;

public interface PublicationIngestEntryRepository extends MongoRepository<PublicationIngestEntry, String> {
    List<PublicationIngestEntry> findByEnvironment(String env);
}
