package uk.ac.ebi.spot.gwas.deposition.domain.ensembl;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "variation")
@Data
public class Variation {
    @Id
    private String variation_id;
    private String name;
}
