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
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;

import java.util.*;

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

    @Autowired
    private UserService userService;

    @Autowired
    private SummaryStatsProcessingService summaryStatsProcessingService;

    public void checkCallbackIds() {
        log.info("Running callback ID checks.");
        if (sumStatsService == null) {
            log.info("Nothing to run. Summary stats service not active.");
            return;
        }

        List<CallbackId> callbackIds = callbackIdRepository.findByCompleted(false);
        for (CallbackId callbackId : callbackIds) {
            SummaryStatsResponseDto summaryStatsResponseDto = sumStatsService.retrieveSummaryStatsStatus(callbackId.getCallbackId());
            if (summaryStatsResponseDto.getStatus().equalsIgnoreCase(SummaryStatsResponseConstants.PROCESSING)) {
                continue;
            }

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
                metadata.put(MailConstants.FIRST_AUTHOR, "N/A");
                if (bodyOfWork.getFirstAuthor() == null) {
                    if (bodyOfWork.getCorrespondingAuthors() != null) {
                        if (!bodyOfWork.getCorrespondingAuthors().isEmpty()) {
                            metadata.put(MailConstants.FIRST_AUTHOR, BackendUtil.extractName(bodyOfWork.getCorrespondingAuthors().get(0)));
                        }
                    }
                } else {
                    metadata.put(MailConstants.FIRST_AUTHOR, BackendUtil.extractName(bodyOfWork.getFirstAuthor()));
                }
                workId = bodyOfWork.getBowId();
            }

            metadata.put(MailConstants.SUBMISSION_ID, backendMailConfig.getSubmissionsBaseURL() + submission.getId());
            metadata.put(MailConstants.SUBMISSION_STUDIES, backendMailConfig.getSubmissionsBaseURL() + submission.getId());
            metadata.put(MailConstants.SUBMISSION_DOCS_URL, backendMailConfig.getSubmissionsDocsURL());

            String userId = submission.getCreated().getUserId();
            log.info("Callback ID completed: {}", callbackId.getCallbackId());
            callbackId.setCompleted(true);
            callbackIdRepository.save(callbackId);

            if (summaryStatsResponseDto.getStatus().equalsIgnoreCase(SummaryStatsResponseConstants.INVALID)) {
                List<String> allErrors = new ArrayList<>();
                if (summaryStatsResponseDto.getMetadataErrors() != null) {
                    if (!summaryStatsResponseDto.getMetadataErrors().isEmpty()) {
                        FileUpload fileUpload = fileUploadsService.getFileUploadByCallbackId(callbackId.getCallbackId());
                        List<String> fileErrors = fileUpload.getErrors() != null ? fileUpload.getErrors() : new ArrayList<>();
                        fileErrors.addAll(summaryStatsResponseDto.getMetadataErrors());
                        allErrors.addAll(summaryStatsResponseDto.getMetadataErrors());
                        fileUpload.setErrors(fileErrors);
                        fileUpload.setStatus(FileUploadStatus.INVALID.name());
                        fileUploadsService.save(fileUpload);

                        auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), fileUpload, submission, true, false, fileErrors));
                    }
                }

                for (SummaryStatsStatusDto summaryStatsStatusDto : summaryStatsResponseDto.getStatusList()) {
                    Optional<SummaryStatsEntry> summaryStatsEntryOptional = summaryStatsEntryRepository.findById(summaryStatsStatusDto.getId());
                    if (!summaryStatsEntryOptional.isPresent()) {
                        log.error("Unable to find summary stats entry: {}", summaryStatsStatusDto.getId());
                        continue;
                    }
                    SummaryStatsEntry summaryStatsEntry = summaryStatsEntryOptional.get();
                    if (summaryStatsStatusDto.getStatus().equalsIgnoreCase(SummaryStatsEntryStatus.INVALID.name())) {
                        summaryStatsEntry.setStatus(SummaryStatsEntryStatus.INVALID.name());
                        summaryStatsEntry.setError(summaryStatsStatusDto.getError());
                        summaryStatsEntryRepository.save(summaryStatsEntry);

                        FileUpload fileUpload = fileUploadsService.getFileUpload(summaryStatsEntry.getFileUploadId());
                        List<String> fileErrors = fileUpload.getErrors() != null ? fileUpload.getErrors() : new ArrayList<>();
                        fileErrors.add(summaryStatsEntry.getFilePath() + ": " + summaryStatsStatusDto.getError());
                        allErrors.add(summaryStatsEntry.getFilePath() + ": " + summaryStatsStatusDto.getError());
                        fileUpload.setErrors(fileErrors);
                        fileUpload.setStatus(FileUploadStatus.INVALID.name());
                        fileUploadsService.save(fileUpload);

                        auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), fileUpload, submission, true, false, fileErrors));
                    }
                }

                submission.setOverallStatus(Status.INVALID.name());
                submission.setSummaryStatsStatus(Status.INVALID.name());
                backendEmailService.sendFailEmail(userId, workId, metadata, allErrors);
                auditProxy.addAuditEntry(AuditHelper.submissionValidate(submission.getCreated().getUserId(), submission, false, allErrors));
                submissionService.saveSubmission(submission, userId);

                callbackId.setValid(false);
                callbackIdRepository.save(callbackId);
            }

            if (summaryStatsResponseDto.getStatus().equalsIgnoreCase(SummaryStatsResponseConstants.VALID)) {
                for (SummaryStatsStatusDto summaryStatsStatusDto : summaryStatsResponseDto.getStatusList()) {
                    Optional<SummaryStatsEntry> summaryStatsEntryOptional = summaryStatsEntryRepository.findById(summaryStatsStatusDto.getId());
                    if (!summaryStatsEntryOptional.isPresent()) {
                        log.error("Unable to find summary stats entry: {}", summaryStatsStatusDto.getId());
                        continue;
                    }
                    SummaryStatsEntry summaryStatsEntry = summaryStatsEntryOptional.get();
                    if (summaryStatsStatusDto.getStatus().equalsIgnoreCase(SummaryStatsEntryStatus.VALID.name())) {
                        summaryStatsEntry.setStatus(SummaryStatsEntryStatus.VALID.name());
                        summaryStatsEntryRepository.save(summaryStatsEntry);
                    }
                }

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
                submissionService.saveSubmission(submission, userId);

                User user = userService.getUser(submission.getCreated().getUserId());
                submission = submissionService.updateSubmissionStatus(submission.getId(), Status.DEPOSITION_COMPLETE.name(), user);
                auditProxy.addAuditEntry(AuditHelper.submissionSubmit(user.getId(), submission));
                log.info("Submission [{}] successfully submitted.", submission.getId());
                summaryStatsProcessingService.callGlobusWrapUp(submission.getId());
            }

            if (summaryStatsResponseDto.getStatus().equalsIgnoreCase(SummaryStatsResponseConstants.IGNORE)) {
                for (SummaryStatsStatusDto summaryStatsStatusDto : summaryStatsResponseDto.getStatusList()) {
                    Optional<SummaryStatsEntry> summaryStatsEntryOptional = summaryStatsEntryRepository.findById(summaryStatsStatusDto.getId());
                    if (!summaryStatsEntryOptional.isPresent()) {
                        log.error("Unable to find summary stats entry: {}", summaryStatsStatusDto.getId());
                        continue;
                    }
                    SummaryStatsEntry summaryStatsEntry = summaryStatsEntryOptional.get();
                    if (summaryStatsStatusDto.getStatus().equalsIgnoreCase(SummaryStatsEntryStatus.VALID.name())) {
                        summaryStatsEntry.setStatus(SummaryStatsEntryStatus.VALID.name());
                        summaryStatsEntryRepository.save(summaryStatsEntry);
                    }
                }

                submission.setOverallStatus(Status.VALID.name());
                submission.setSummaryStatsStatus(Status.VALID.name());

                FileUpload fileUpload = fileUploadsService.getFileUploadByCallbackId(callbackId.getCallbackId());
                if (fileUpload != null) {
                    fileUpload.setStatus(FileUploadStatus.VALID.name());
                    fileUploadsService.save(fileUpload);
                    auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), fileUpload, submission, true, true, null));
                }


                auditProxy.addAuditEntry(AuditHelper.submissionValidate(submission.getCreated().getUserId(), submission, true, null));
                submissionService.saveSubmission(submission, userId);

                User user = userService.getUser(submission.getCreated().getUserId());
                submission = submissionService.updateSubmissionStatus(submission.getId(), Status.DEPOSITION_COMPLETE.name(), user);
                auditProxy.addAuditEntry(AuditHelper.submissionSubmit(user.getId(), submission));
                log.info("Submission [{}] successfully submitted.", submission.getId());
            }
        }
    }

}
