package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.repository.UserRepository;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public User findUser(User user, boolean createIfNotExistent) {
        log.info("Looking for user [{}]: {}", user.getName(), user.getEmail());
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(user.getEmail());
        if (userOpt.isPresent()) {
            User foundUser = userOpt.get();
            if (!user.getDomains().equals(foundUser.getDomains())) {
                foundUser.setDomains(user.getDomains());
                foundUser = userRepository.save(foundUser);
            }
            log.info("Returning existing user: {}", foundUser.getId());
            return userOpt.get();
        }
        if (createIfNotExistent) {
            user = userRepository.insert(user);
            log.info("Returning newly created user: {}", user.getId());
            return user;
        }

        log.error("User [{}] not found.", user.getEmail());
        throw new EntityNotFoundException("User [" + user.getEmail() + "] not found.");
    }

    @Override
    public User getUser(String userId) {
        log.info("Retrieving user: {}", userId);
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            log.error("Unable to find user: {}", userId);
            throw new EntityNotFoundException("Unable to find user: " + userId);
        }
        log.info("Returning user: {}", userOpt.get().getName());
        return userOpt.get();
    }
}
