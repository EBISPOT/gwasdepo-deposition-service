package uk.ac.ebi.spot.gwas.deposition.repository.ensembl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.spot.gwas.deposition.domain.ensembl.Variation;

import java.util.Collection;
import java.util.List;

@Repository
public interface VariationRepository extends JpaRepository<Variation, Long> {
    List<Variation> findByNameIn(Collection<String> names);
}
