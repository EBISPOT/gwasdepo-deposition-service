package uk.ac.ebi.spot.gwas.deposition.scheduler.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.domain.CompletedSubmission;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.repository.CompletedSubmissionRepository;
import uk.ac.ebi.spot.gwas.deposition.service.PublicationService;
import uk.ac.ebi.spot.gwas.deposition.service.SummaryStatsProcessingService;

import java.util.List;

@Component
public class CompleteSubmissionsTask {

    private static final Logger log = LoggerFactory.getLogger(CompleteSubmissionsTask.class);

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private CompletedSubmissionRepository completedSubmissionRepository;

    @Autowired
    private SummaryStatsProcessingService summaryStatsProcessingService;

    public void checkCompletedSubmissions() {
        log.info("Verifying for completed submissions ...");
        List<CompletedSubmission> completedSubmissionList = completedSubmissionRepository.findAll();
        log.info("Found {} completed submissions.", completedSubmissionList.size());
        for (CompletedSubmission completedSubmission : completedSubmissionList) {
            Publication publication = publicationService.retrievePublication(completedSubmission.getPublicationId(), true);
            if (publication != null) {
                log.info("Calling SS Service to wrap up Globus for publication: {}", publication.getPmid());
                summaryStatsProcessingService.callGlobusWrapUp(publication);
            }
            completedSubmissionRepository.delete(completedSubmission);
        }
        log.info("Completed submissions check done.");
    }
}
