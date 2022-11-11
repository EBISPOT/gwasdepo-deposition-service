package uk.ac.ebi.spot.gwas.deposition.domain.ensembl;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "variation_synonym")
@Data
public class VariationSynonym {
    @Id
    private String variation_synonym_id;
    private String name;
}
