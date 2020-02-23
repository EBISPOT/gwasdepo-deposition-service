package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.User;

public interface CuratorAuthService {

    boolean isCurator(User user);
}
