package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;

public interface BodyOfWorkService {
    BodyOfWork createBodyOfWork(BodyOfWork bodyOfWork);

    BodyOfWork retrieveBodyOfWork(String bodyOfWork, String userId);

    Page<BodyOfWork> retrieveBodyOfWorks(String userId, Pageable pageable);

    void deleteBodyOfWork(String bodyofworkId, String userId);
}
