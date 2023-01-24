package uk.ac.ebi.spot.gwas.deposition.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.Curator;

import java.util.Optional;

public interface CuratorRepository extends MongoRepository<Curator, String> {

    public Optional<Curator> findById(String id);
    public Optional<Curator> findByFirstNameAndLastName(String firstName, String lastName);
}
