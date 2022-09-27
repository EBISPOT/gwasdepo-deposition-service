package uk.ac.ebi.spot.gwas.deposition.repository.ensembl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.spot.gwas.deposition.domain.ensembl.VariationSynonym;

import java.util.Collection;
import java.util.List;

@Repository
public interface VariationSynonymRepository extends JpaRepository<VariationSynonym, Long> {
    List<VariationSynonym> findByNameIn(Collection<String> names);
}
