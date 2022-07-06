package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditHelper;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditProxy;
import uk.ac.ebi.spot.gwas.deposition.config.BackendMailConfig;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.MailConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.constants.SubmissionProvenanceType;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSWrapUpRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSWrapUpRequestEntryDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsRequestEntryDto;
import uk.ac.ebi.spot.gwas.deposition.repository.CallbackIdRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.StudyRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SummaryStatsEntryRepository;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SummaryStatsRequestEntryDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.*;
import uk.ac.ebi.spot.gwas.deposition.util.AccessionMapUtil;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;

import java.util.*;

@Service
public class SummaryStatsProcessingServiceImpl implements SummaryStatsProcessingService {

    private static final Logger log = LoggerFactory.getLogger(SummaryStatsProcessingService.class);

    @Autowired(required = false)
    private SumStatsService sumStatsService;

    @Autowired
    private SummaryStatsEntryRepository summaryStatsEntryRepository;

    @Autowired
    private CallbackIdRepository callbackIdRepository;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private AuditProxy auditProxy;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private BackendMailConfig backendMailConfig;

    @Autowired
    private BackendEmailService backendEmailService;

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private UserService userService;

    @Override
    @Async
    public void processSummaryStats(Submission submission, String fileUploadId, List<SummaryStatsEntry> summaryStatsEntries,
                                    Publication publication, BodyOfWork bodyOfWork, String userId, String appType) {
        log.info("Processing {} summary stats.", summaryStatsEntries.size());
        log.info("Summary stats service enabled: {}", (sumStatsService != null));

        if (summaryStatsEntries.isEmpty() || sumStatsService == null) {
            if (sumStatsService == null) {
                log.info("Summary Stats Service is not active.");
            }
            submission.setOverallStatus(Status.VALID.name());
            submission.setSummaryStatsStatus(Status.NA.name());
            submissionService.saveSubmission(submission, userId);

            Map<String, Object> metadata = new HashMap<>();
            String workId = null;
            if (publication != null) {
                metadata.put(MailConstants.PUBLICATION_TITLE, publication.getTitle());
                metadata.put(MailConstants.PMID, publication.getPmid());
                metadata.put(MailConstants.FIRST_AUTHOR, publication.getFirstAuthor());
                workId = publication.getPmid();
            } else {
                if (bodyOfWork != null) {
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
            }

            metadata.put(MailConstants.SUBMISSION_ID, backendMailConfig.getSubmissionsBaseURL() + submission.getId());
            metadata.put(MailConstants.SUBMISSION_STUDIES, backendMailConfig.getSubmissionsBaseURL() + submission.getId());
            metadata.put(MailConstants.SUBMISSION_DOCS_URL, backendMailConfig.getSubmissionsDocsURL());

            if (workId != null && !(appType != null && appType.equals("depo-curation"))) {
                backendEmailService.sendSuccessEmail(submission.getCreated().getUserId(), workId, metadata);
            }
            auditProxy.addAuditEntry(AuditHelper.submissionValidate(submission.getCreated().getUserId(), submission, true, null));
            User user = userService.getUser(submission.getCreated().getUserId());
            submission = submissionService.updateSubmissionStatus(submission.getId(), Status.DEPOSITION_COMPLETE.name(), user);
            auditProxy.addAuditEntry(AuditHelper.submissionSubmit(user.getId(), submission));
            log.info("Submission [{}] successfully submitted.", submission.getId());
            return;
        }
        submission.setSummaryStatsStatus(Status.VALIDATING.name());
        submissionService.saveSubmission(submission, userId);

        List<SummaryStatsRequestEntryDto> list = new ArrayList<>();
        for (SummaryStatsEntry summaryStatsEntry : summaryStatsEntries) {
            summaryStatsEntry = summaryStatsEntryRepository.insert(summaryStatsEntry);
            list.add(SummaryStatsRequestEntryDtoAssembler.assemble(summaryStatsEntry));
        }

        Boolean skipValidation = false;

        if(appType != null && appType.equals("depo-curation"))
            skipValidation = true;

        String callbackId = sumStatsService.registerStatsForProcessing(new SummaryStatsRequestDto(list, skipValidation));
        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadId);

        if (callbackId == null) {
            log.error("Unable to register summary stats for processing. Received NULL callback ID.");

            submission.setOverallStatus(Status.INVALID.name());
            submission.setSummaryStatsStatus(Status.INVALID.name());
            submissionService.saveSubmission(submission, userId);

            List<String> errors = Arrays.asList(new String[]{"Sorry! There is a fault on our end. Please contact gwas-subs@ebi.ac.uk for help."});
            fileUpload.setStatus(FileUploadStatus.INVALID.name());
            fileUpload.setErrors(errors);
            fileUploadsService.save(fileUpload);

            auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), fileUpload, submission, true, false, errors));
            auditProxy.addAuditEntry(AuditHelper.submissionValidate(submission.getCreated().getUserId(), submission, false, errors));
        } else {
            log.info("Successfully registered {} summary stats with callback ID: {}",
                    summaryStatsEntries.size(), callbackId);
            callbackIdRepository.insert(new CallbackId(callbackId, submission.getId()));

            for (SummaryStatsEntry summaryStatsEntry : summaryStatsEntries) {
                summaryStatsEntry.setCallbackId(callbackId);
                summaryStatsEntryRepository.save(summaryStatsEntry);
            }
            fileUpload.setStatus(FileUploadStatus.VALIDATING.name());
            fileUpload.setCallbackId(callbackId);
            fileUploadsService.save(fileUpload);

            submission.setSummaryStatsStatus(Status.VALIDATING.name());
            submissionService.saveSubmission(submission, userId);
        }
    }

    @Override
    @Async
    public void callGlobusWrapUp(String submissionId) {
        log.info("Wrapping up Globus folder for submission: {}", submissionId);
        AccessionMapUtil accessionMapUtil = new AccessionMapUtil();
        User user = new User("Auto Curator", gwasDepositionBackendConfig.getAutoCuratorServiceAccount());
        user.setDomains(gwasDepositionBackendConfig.getCuratorDomains());
        Submission submission = submissionService.getSubmission(submissionId, user);
        for (String studyId : submission.getStudies()) {
            Optional<Study> studyOptional = studyRepository.findById(studyId);
            if (!studyOptional.isPresent()) {
                log.error("Unable to find study [{}] for submission: {}", studyId, submissionId);
            } else {
                accessionMapUtil.addStudy(studyOptional.get().getStudyTag(), studyOptional.get().getAccession());
            }
        }
        Map<String, String> accessionMap = accessionMapUtil.getAccessionMap();

        Optional<CallbackId> callbackIdOptional = callbackIdRepository.findBySubmissionIdAndCompleted(submissionId, true);
        if (!callbackIdOptional.isPresent()) {
            log.error("Unable to perform Globus wrap up operation. Cannot find callback ID for submission: {}", submissionId);
            backendEmailService.sendErrorsEmail("Pre-Globus wrap up",
                    "Unable to perform Globus wrap up operation. Cannot find callback ID for submission: " + submissionId);
            return;
        }

        log.info("Accession map [{}]: {}", callbackIdOptional.get().getCallbackId(), accessionMap);

        List<SSWrapUpRequestEntryDto> ssWrapUpRequestEntryDtos = new ArrayList<>();
        List<SummaryStatsEntry> summaryStatsEntries = summaryStatsEntryRepository.findByCallbackId(callbackIdOptional.get().getCallbackId());
        List<String> errors = new ArrayList<>();
        for (SummaryStatsEntry summaryStatsEntry : summaryStatsEntries) {
            log.info("Verifying: {}", summaryStatsEntry.getStudyTag());
            String gcst = accessionMap.get(summaryStatsEntry.getStudyTag());
            if (gcst == null) {
                log.error("Unable to add ss data for [{}]. GCST is missing.", summaryStatsEntry.getStudyTag());
                errors.add("Unable to add ss data for [" + summaryStatsEntry.getStudyTag() + "]. GCST is missing.");
                continue;
            }
            ssWrapUpRequestEntryDtos.add(new SSWrapUpRequestEntryDto(summaryStatsEntry.getId(), gcst));
        }

        if (!errors.isEmpty()) {
            backendEmailService.sendErrorsEmail("Pre-Globus wrap up", StringUtils.join(errors, "; "));
        }
        if (ssWrapUpRequestEntryDtos.isEmpty()) {
            backendEmailService.sendErrorsEmail("Pre-Globus wrap up",
                    "Unable to perform Globus wrap up operation. No data entry items were populated.");
            log.error("Unable to perform Globus wrap up operation. No data entry items were populated.");
            return;
        }

        SSWrapUpRequestDto ssWrapUpRequestDto = new SSWrapUpRequestDto(ssWrapUpRequestEntryDtos);
        sumStatsService.wrapUpGlobusSubmission(callbackIdOptional.get().getCallbackId(), ssWrapUpRequestDto);
    }
}
