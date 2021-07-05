package uk.ac.ebi.spot.gwas.deposition.scheduler.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.repository.SubmissionRepository;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionDataCleaningService;
import java.util.List;

@Component
public class SubmissionsCleanUpTask {

    private static final Logger log = LoggerFactory.getLogger(SubmissionsCleanUpTask.class);

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionDataCleaningService submissionDataCleaningService;

    public void cleanUp() {
        log.info("Cleaning up deleted submissions.");
        List<Submission> submissionList = submissionRepository.findByArchived(true);
        log.info("Found {} submissions to be deleted.", submissionList.size());
        for (Submission submission : submissionList) {
            submissionDataCleaningService.deleteSubmission(submission);
        }
    }

}
