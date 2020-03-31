package uk.ac.ebi.spot.gwas.deposition.scheduler.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.MailConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.constants.SummaryStatsEntryStatus;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsResponseDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsStatusDto;
import uk.ac.ebi.spot.gwas.deposition.repository.CallbackIdRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SummaryStatsEntryRepository;
import uk.ac.ebi.spot.gwas.deposition.service.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SSCallbackTask {

    private static final Logger log = LoggerFactory.getLogger(SSCallbackTask.class);

    @Autowired(required = false)
    private SumStatsService sumStatsService;

    @Autowired
    private SummaryStatsEntryRepository summaryStatsEntryRepository;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private CallbackIdRepository callbackIdRepository;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PublicationService publicationService;

    @Scheduled(cron = "0 */1 * * * ?")
    public void checkCallbackIds() {
        log.info("Running callback ID checks.");
        if (sumStatsService == null) {
            log.info("Nothing to run. Summary stats service not active.");
            return;
        }

        List<CallbackId> callbackIds = callbackIdRepository.findByCompleted(false);
        for (CallbackId callbackId : callbackIds) {
            SummaryStatsResponseDto summaryStatsResponseDto = sumStatsService.retrieveSummaryStatsStatus(callbackId.getCallbackId());
            List<String> errors = null;

            for (SummaryStatsStatusDto summaryStatsStatusDto : summaryStatsResponseDto.getStatusList()) {
                if (!summaryStatsStatusDto.getStatus().equalsIgnoreCase(SummaryStatsEntryStatus.VALIDATING.name()) &&
                        !summaryStatsStatusDto.getStatus().equalsIgnoreCase(SummaryStatsEntryStatus.RETRIEVING.name())) {
                    Optional<SummaryStatsEntry> summaryStatsEntryOptional = summaryStatsEntryRepository.findById(summaryStatsStatusDto.getId());
                    if (!summaryStatsEntryOptional.isPresent()) {
                        log.error("Unable to find summary stats entry: {}", summaryStatsStatusDto.getId());
                    } else {
                        SummaryStatsEntry summaryStatsEntry = summaryStatsEntryOptional.get();
                        if (summaryStatsStatusDto.getStatus().equalsIgnoreCase(SummaryStatsEntryStatus.VALID.name())) {
                            summaryStatsEntry.setStatus(SummaryStatsEntryStatus.VALID.name());
                        } else {
                            summaryStatsEntry.setStatus(SummaryStatsEntryStatus.INVALID.name());
                            summaryStatsEntry.setError(summaryStatsStatusDto.getError());

                            FileUpload fileUpload = fileUploadsService.getFileUpload(summaryStatsEntry.getFileUploadId());
                            errors = fileUpload.getErrors();
                            errors.add(summaryStatsEntry.getFilePath() + ": " + summaryStatsStatusDto.getError());
                            fileUpload.setErrors(errors);
                            fileUpload.setStatus(FileUploadStatus.INVALID.name());
                            fileUploadsService.save(fileUpload);

                            callbackId.setValid(false);
                        }
                    }
                }
            }

            if (summaryStatsResponseDto.getCompleted() != null) {
                if (summaryStatsResponseDto.getCompleted().booleanValue()) {
                    Submission submission = submissionService.getSubmission(callbackId.getSubmissionId(),
                            new User(gwasDepositionBackendConfig.getAutoCuratorServiceAccount(),
                                    gwasDepositionBackendConfig.getAutoCuratorServiceAccount()));
                    Publication publication = publicationService.retrievePublication(submission.getPublicationId(), true);
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put(MailConstants.PUBLICATION_TITLE, publication.getTitle());
                    metadata.put(MailConstants.PMID, publication.getPmid());
                    metadata.put(MailConstants.FIRST_AUTHOR, publication.getFirstAuthor());
                    metadata.put(MailConstants.SUBMISSION_ID, gwasDepositionBackendConfig.getSubmissionsBaseURL() + submission.getId());

                    String userId = submission.getLastUpdated() != null ? submission.getLastUpdated().getUserId() :
                            submission.getCreated().getUserId();
                    if (callbackId.isValid()) {
                        submission.setOverallStatus(Status.VALID.name());
                        submission.setSummaryStatsStatus(Status.VALID.name());

                        FileUpload fileUpload = fileUploadsService.getFileUploadByCallbackId(callbackId.getCallbackId());
                        if (fileUpload != null) {
                            fileUpload.setStatus(FileUploadStatus.VALID.name());
                            fileUploadsService.save(fileUpload);
                        }

                        emailService.sendSuccessEmail(userId, publication.getPmid(), metadata);
                    } else {
                        submission.setOverallStatus(Status.INVALID.name());
                        submission.setSummaryStatsStatus(Status.INVALID.name());
                        emailService.sendFailEmail(userId, publication.getPmid(), metadata, errors);
                    }
                    submissionService.saveSubmission(submission);

                    log.info("Callback ID completed: {}", callbackId.getCallbackId());
                    callbackId.setCompleted(true);
                    callbackIdRepository.save(callbackId);
                }
            }
        }
    }
}
