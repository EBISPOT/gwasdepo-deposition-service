package uk.ac.ebi.spot.gwas.deposition.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWorkWatch;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.repository.BodyOfWorkWatchRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SubmissionRepository;

import java.util.List;
import java.util.Optional;

@Component
public class BodyOfWorkListener {

    @Autowired
    private BodyOfWorkWatchRepository bodyOfWorkWatchRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Async
    public void update(BodyOfWork bodyOfWork, String publicationId) {
        Optional<BodyOfWorkWatch> bodyOfWorkWatchOptional = bodyOfWorkWatchRepository.findByBowId(bodyOfWork.getBowId());
        if (bodyOfWorkWatchOptional.isPresent()) {
            BodyOfWorkWatch bodyOfWorkWatch = bodyOfWorkWatchOptional.get();
            bodyOfWorkWatch.setVisited(false);
            bodyOfWorkWatchRepository.save(bodyOfWorkWatch);
        }

        if (publicationId != null) {
            List<Submission> submissionList = submissionRepository.findByBodyOfWorksContainsAndArchived(bodyOfWork.getBowId(), false);
            for (Submission submission : submissionList) {
                if (submission.getPublicationId() == null) {
                    submission.setPublicationId(publicationId);
                    submissionRepository.save(submission);
                }
            }
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
