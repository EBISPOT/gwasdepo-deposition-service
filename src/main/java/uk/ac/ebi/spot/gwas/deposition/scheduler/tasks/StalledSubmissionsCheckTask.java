package uk.ac.ebi.spot.gwas.deposition.scheduler.tasks;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.BackendSubmissionChecksConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.MailConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.ReminderStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.constants.SubmissionProvenanceType;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.repository.SubmissionRepository;
import uk.ac.ebi.spot.gwas.deposition.service.BackendEmailService;
import uk.ac.ebi.spot.gwas.deposition.service.BodyOfWorkService;
import uk.ac.ebi.spot.gwas.deposition.service.PublicationService;
import uk.ac.ebi.spot.gwas.deposition.service.SumStatsService;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class StalledSubmissionsCheckTask {

    private static final Logger log = LoggerFactory.getLogger(StalledSubmissionsCheckTask.class);

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private BackendSubmissionChecksConfig backendSubmissionChecksConfig;

    @Autowired
    private BackendEmailService backendEmailService;

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private BodyOfWorkService bodyOfWorkService;

    @Autowired
    private SumStatsService sumStatsService;

    public void check() {
        log.info("Verifying stalled submissions.");
        Stream<Submission> startedSubmissionStream = submissionRepository.readByOverallStatusAndArchivedOrderByLastUpdatedDesc(Status.STARTED.name(), false);
        startedSubmissionStream.forEach(submission -> verifySubmission(submission));
        startedSubmissionStream.close();

        Stream<Submission> invalidSubmissionStream = submissionRepository.readByOverallStatusAndArchivedOrderByLastUpdatedDesc(Status.INVALID.name(), false);
        invalidSubmissionStream.forEach(submission -> verifySubmission(submission));
        invalidSubmissionStream.close();
        /**
         * USER_NAME
         * SUBMISSION_ID
         * PUBLICATION_TITLE
         */
    }

    private void verifySubmission(Submission submission) {
        log.info("Checking submission: {}", submission.getId());
        if (submission.getReminderStatus().equalsIgnoreCase(ReminderStatus.EXCLUDED.name())) {
            log.info(" - Excluded.");
            return;
        }

        DateTime now = DateTime.now();
        DateTime sLastUpdated = submission.getLastUpdated().getTimestamp();
        String title;

        if (submission.getProvenanceType().equalsIgnoreCase(SubmissionProvenanceType.PUBLICATION.name())) {
            Publication publication = publicationService.retrievePublication(submission.getPublicationId(), true);
            title = publication.getTitle();
        } else {
            BodyOfWork bodyOfWork = bodyOfWorkService.retrieveBodyOfWork(submission.getBodyOfWorks().get(0), submission.getCreated().getUserId());
            title = bodyOfWork.getTitle();
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(MailConstants.SUBMISSION_ID, submission.getId());
        metadata.put(MailConstants.PUBLICATION_TITLE, title);

        // Last check: delete
        if (now.isAfter(sLastUpdated.plusDays(backendSubmissionChecksConfig.getLastCheck()))) {
            log.info(" - Submission [{} | {}] stalled for more than {} days.", submission.getId(),
                    submission.getReminderStatus(),
                    backendSubmissionChecksConfig.getLastCheck());
            backendEmailService.sendReminderEmail(submission.getCreated().getUserId(), metadata, backendSubmissionChecksConfig.getLastEmail());
            submission.setReminderStatus(ReminderStatus.ARCHIVED.name());
            submission.setArchived(true);
            sumStatsService.deleteGlobusFolder(submission);
            submission.setDeletedOn(DateTime.now());
            submissionRepository.save(submission);
            return;
        }

        // Second check: send second reminder
        if (now.isAfter(sLastUpdated.plusDays(backendSubmissionChecksConfig.getSecondCheck()))) {
            log.info(" - Submission [{} | {}] stalled for more than {} days.", submission.getId(),
                    submission.getReminderStatus(),
                    backendSubmissionChecksConfig.getSecondCheck());
            if (!submission.getReminderStatus().equalsIgnoreCase(ReminderStatus.SECOND.name())) {
                backendEmailService.sendReminderEmail(submission.getCreated().getUserId(), metadata, backendSubmissionChecksConfig.getSecondEmail());
                submission.setReminderStatus(ReminderStatus.SECOND.name());
                submissionRepository.save(submission);
            }
            return;
        }

        // First check: send first reminder
        if (now.isAfter(sLastUpdated.plusDays(backendSubmissionChecksConfig.getFirstCheck()))) {
            log.info(" - Submission [{} | {}] stalled for more than {} days.", submission.getId(),
                    submission.getReminderStatus(),
                    backendSubmissionChecksConfig.getFirstCheck());
            if (!submission.getReminderStatus().equalsIgnoreCase(ReminderStatus.FIRST.name())) {
                backendEmailService.sendReminderEmail(submission.getCreated().getUserId(), metadata, backendSubmissionChecksConfig.getFirstEmail());
                submission.setReminderStatus(ReminderStatus.FIRST.name());
                submissionRepository.save(submission);
            }
            return;
        }
    }

}
