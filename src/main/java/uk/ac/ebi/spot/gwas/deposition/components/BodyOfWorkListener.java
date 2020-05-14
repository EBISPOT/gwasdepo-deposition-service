package uk.ac.ebi.spot.gwas.deposition.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWorkWatch;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.repository.BodyOfWorkWatchRepository;

import java.util.Optional;

@Component
public class BodyOfWorkListener {

    @Autowired
    private BodyOfWorkWatchRepository bodyOfWorkWatchRepository;

    @Async
    public void update(BodyOfWork bodyOfWork) {
        Optional<BodyOfWorkWatch> bodyOfWorkWatchOptional = bodyOfWorkWatchRepository.findByBowId(bodyOfWork.getBowId());
        if (bodyOfWorkWatchOptional.isPresent()) {
            BodyOfWorkWatch bodyOfWorkWatch = bodyOfWorkWatchOptional.get();
            bodyOfWorkWatch.setVisited(false);
            bodyOfWorkWatchRepository.save(bodyOfWorkWatch);
        }
    }

    public void update(Submission submission) {
        if (submission.getBodyOfWorks() != null) {
            if (!submission.getBodyOfWorks().isEmpty()) {
                String bowId = submission.getBodyOfWorks().get(0);
                bodyOfWorkWatchRepository.insert(new BodyOfWorkWatch(bowId));
            }
        }
    }
}
