package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.User;

public interface UserService {

    User findUser(User user, boolean createIfNotExistent);

    User getUser(String userId);
}
