package uk.ac.ebi.spot.gwas.deposition.scheduler.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditHelper;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditProxy;
import uk.ac.ebi.spot.gwas.deposition.config.BackendMailConfig;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.*;
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
    private BackendEmailService backendEmailService;

    @Autowired
    private BackendMailConfig backendMailConfig;

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private BodyOfWorkService bodyOfWorkService;

    @Autowired
    private AuditProxy auditProxy;

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

                            Submission submission = submissionService.getSubmission(callbackId.getSubmissionId(),
                                    new User(gwasDepositionBackendConfig.getAutoCuratorServiceAccount(),
                                            gwasDepositionBackendConfig.getAutoCuratorServiceAccount()));

                            auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), fileUpload, submission, true, false, errors));
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

                    Map<String, Object> metadata = new HashMap<>();
                    String workId;
                    if (submission.getProvenanceType().equalsIgnoreCase(SubmissionProvenanceType.PUBLICATION.name())) {
                        Publication publication = publicationService.retrievePublication(submission.getPublicationId(), true);
                        metadata.put(MailConstants.PUBLICATION_TITLE, publication.getTitle());
                        metadata.put(MailConstants.PMID, publication.getPmid());
                        metadata.put(MailConstants.FIRST_AUTHOR, publication.getFirstAuthor());
                        workId = publication.getPmid();
                    } else {
                        BodyOfWork bodyOfWork = bodyOfWorkService.retrieveBodyOfWork(submission.getBodyOfWorks().get(0),
                                submission.getCreated().getUserId());
                        metadata.put(MailConstants.PUBLICATION_TITLE, bodyOfWork.getTitle());
                        metadata.put(MailConstants.PMID, bodyOfWork.getBowId());
                        metadata.put(MailConstants.FIRST_AUTHOR, bodyOfWork.getFirstAuthor().getLastName());
                        workId = bodyOfWork.getBowId();
                    }

                    metadata.put(MailConstants.SUBMISSION_ID, backendMailConfig.getSubmissionsBaseURL() + submission.getId());
                    metadata.put(MailConstants.SUBMISSION_STUDIES, backendMailConfig.getSubmissionsBaseURL() + submission.getId() + GWASDepositionBackendConstants.API_STUDY_ENVELOPES);

                    String userId = submission.getLastUpdated() != null ? submission.getLastUpdated().getUserId() :
                            submission.getCreated().getUserId();
                    if (callbackId.isValid()) {
                        submission.setOverallStatus(Status.VALID.name());
                        submission.setSummaryStatsStatus(Status.VALID.name());

                        FileUpload fileUpload = fileUploadsService.getFileUploadByCallbackId(callbackId.getCallbackId());
                        if (fileUpload != null) {
                            fileUpload.setStatus(FileUploadStatus.VALID.name());
                            fileUploadsService.save(fileUpload);
                            auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), fileUpload, submission, true, true, null));
                        }

                        backendEmailService.sendSuccessEmail(userId, workId, metadata);
                        auditProxy.addAuditEntry(AuditHelper.submissionValidate(submission.getCreated().getUserId(), submission, true, null));
                    } else {
                        submission.setOverallStatus(Status.INVALID.name());
                        submission.setSummaryStatsStatus(Status.INVALID.name());
                        backendEmailService.sendFailEmail(userId, workId, metadata, errors);
                        auditProxy.addAuditEntry(AuditHelper.submissionValidate(submission.getCreated().getUserId(), submission, false, errors));
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
