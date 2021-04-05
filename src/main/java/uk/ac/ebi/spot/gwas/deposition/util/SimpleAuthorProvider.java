package uk.ac.ebi.spot.gwas.deposition.util;

import org.javers.spring.auditable.AuthorProvider;

public class SimpleAuthorProvider implements AuthorProvider {

    @Override
    public String provide() {
        return "Javers-Audit";
    }
}
