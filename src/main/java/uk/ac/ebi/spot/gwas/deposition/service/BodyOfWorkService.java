package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.domain.User;

public interface BodyOfWorkService {
    BodyOfWork createBodyOfWork(BodyOfWork bodyOfWork);

    BodyOfWork retrieveBodyOfWork(String bodyOfWork, User user);

    BodyOfWork retrieveBodyOfWork(String bodyOfWork, String userId);

    Page<BodyOfWork> retrieveBodyOfWorks(User user, String status, Pageable pageable);

    void deleteBodyOfWork(String bodyofworkId, User user);

    void save(BodyOfWork bodyOfWork);

    BodyOfWork updateBodyOfWork(String bodyofworkId, BodyOfWork bodyOfWork, User user);
}
