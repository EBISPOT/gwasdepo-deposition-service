package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditHelper;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditProxy;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadType;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateEntryDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateRequestStudyDto;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SSTemplateEntryDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.*;
import uk.ac.ebi.spot.gwas.template.validator.config.ErrorType;
import uk.ac.ebi.spot.gwas.template.validator.util.ErrorMessageTemplateProcessor;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
public class FileHandlerServiceImpl implements FileHandlerService {

    private static final Logger log = LoggerFactory.getLogger(FileHandlerService.class);

    @Autowired(required = false)
    private GWASCatalogRESTService gwasCatalogRESTService;

    @Autowired(required = false)
    private TemplateService templateService;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private MetadataValidationService metadataValidationService;

    @Autowired
    private SummaryStatsValidationService summaryStatsValidationService;

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private ErrorMessageTemplateProcessor errorMessageTemplateProcessor;

    @Autowired
    private AuditProxy auditProxy;

    @Autowired
    private BackendEmailService backendEmailService;

    @Autowired
    private CuratorAuthService curatorAuthService;

    @Override
    @Async
    public void handleSummaryStatsTemplate(Submission submission, Publication publication, User user) {
        log.info("Started summary stats template handling for PMID: {} - {}", submission.getId(),
                submission.getPublicationId());

        List<SSTemplateEntryDto> summaryStatsEntries;
        if (gwasCatalogRESTService == null) {
            log.info("GWAS Catalog service is not enabled. Retrieving ss data from local DB");
            List<SSTemplateEntry> ssTemplateEntries = publicationService.retrieveSSTemplateEntries(publication.getPmid());
            summaryStatsEntries = SSTemplateEntryDtoAssembler.assembleList(ssTemplateEntries);
        } else {
            summaryStatsEntries = gwasCatalogRESTService.getSSTemplateEntries(publication.getPmid());
        }
        log.info("[{}] Retrieved {} summary stats entries.", submission.getPublicationId(), summaryStatsEntries.size());

        FileUpload fileUpload;
        if (summaryStatsEntries.isEmpty()) {
            log.error("No summary stats data available for publication: {}", publication.getPmid());
            backendEmailService.sendErrorsEmail("SS File Handler", "No summary stats data available for publication: " + publication.getPmid());
            fileUpload = fileUploadsService.storeFile(null, null, null, 0, FileUploadType.SUMMARY_STATS_TEMPLATE.name());
            submission.addFileUpload(fileUpload.getId());
            auditProxy.addAuditEntry(AuditHelper.fileCreate(submission.getCreated().getUserId(), fileUpload, submission, false,
                    "No summary stats data available for publication: {}" + publication.getPmid()));
            submissionService.saveSubmission(submission, user.getId());
            markInvalidFile(fileUpload, submission, ErrorType.NO_SS_DATA, publication.getPmid(), user);
            return;
        }

        if (templateService == null) {
            log.error("Template service not available.");
            fileUpload = fileUploadsService.storeFile(null, null, null, 0, FileUploadType.SUMMARY_STATS_TEMPLATE.name());
            submission.addFileUpload(fileUpload.getId());
            auditProxy.addAuditEntry(AuditHelper.fileCreate(submission.getCreated().getUserId(), fileUpload, submission, false, "Template service not available."));
            submissionService.saveSubmission(submission, user.getId());
            markInvalidFile(fileUpload, submission, ErrorType.NO_TEMPLATE_SERVICE, null, user);
        } else {
            FileObject fileObject = templateService.retrievePrefilledTemplate(new SSTemplateRequestDto(true,
                    false /*curatorAuthService.isCurator(user)*/, new SSTemplateRequestStudyDto(summaryStatsEntries)));
            if (fileObject == null) {
                log.error("No file object received from the template service!");
                backendEmailService.sendErrorsEmail("SS File Handler", "[" + publication.getPmid() + "] No file object received from the template service!");
                fileUpload = fileUploadsService.storeFile(null, null, null, 0, FileUploadType.SUMMARY_STATS_TEMPLATE.name());
                submission.addFileUpload(fileUpload.getId());
                auditProxy.addAuditEntry(AuditHelper.fileCreate(submission.getCreated().getUserId(), fileUpload, submission, false, "No file object received from the template service!"));
                submissionService.saveSubmission(submission, user.getId());
                markInvalidFile(fileUpload, submission, ErrorType.UNUSABLE_DATA, null, user);
                return;
            }

            fileUpload = fileUploadsService.storeFile(new ByteArrayInputStream(fileObject.getContent()),
                    fileObject.getFileName(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fileObject.getContent().length, FileUploadType.SUMMARY_STATS_TEMPLATE.name());
            fileUpload.setStatus(FileUploadStatus.VALID.name());
            fileUploadsService.save(fileUpload);
            auditProxy.addAuditEntry(AuditHelper.fileCreate(submission.getCreated().getUserId(), fileUpload, submission, true, null));

            log.info("File upload created: {}", fileUpload.getId());
            submission.addFileUpload(fileUpload.getId());
            submissionService.saveSubmission(submission, user.getId());
        }
    }

    @Override
    public FileUpload handleMetadataFile(Submission submission, MultipartFile file, User user, List<Study> oldStudies) {
        log.info("Started metadata submission [{}] handling for PMID: {}", submission.getId(),
                submission.getPublicationId());

        FileUpload fileUpload = fileUploadsService.storeFile(file, FileUploadType.METADATA.name());
        fileUploadsService.setNotLatest(submission.getFileUploads());
        submission.addFileUpload(fileUpload.getId());
        submissionService.saveSubmission(submission, user.getId());
        metadataValidationService.validateTemplate(submission.getId(), fileUpload,
                fileUploadsService.retrieveFileContent(fileUpload.getId()), user, oldStudies);
        return fileUpload;
    }

    @Override
    public FileUpload handleSummaryStatsFile(Submission submission, MultipartFile file, User user) {
        log.info("Started summary stats data handling for PMID: {} - {} - {}", submission.getId(),
                submission.getPublicationId(), file.getName());

        FileUpload fileUpload = fileUploadsService.storeFile(file, FileUploadType.SUMMARY_STATS.name());
        submission.addFileUpload(fileUpload.getId());
        submissionService.saveSubmission(submission, user.getId());
        summaryStatsValidationService.validateSummaryStatsData(submission, user);
        return fileUpload;
    }

    private void markInvalidFile(FileUpload fileUpload, Submission submission, String errorType, String context, User user) {
        fileUpload.setErrors(errorMessageTemplateProcessor.processGenericError(errorType, context));
        fileUpload.setStatus(FileUploadStatus.INVALID.name());
        fileUploadsService.save(fileUpload);

        submission.setSummaryStatsStatus(Status.INVALID.name());
        submission.setOverallStatus(Status.INVALID.name());
        submissionService.saveSubmission(submission, user.getId());
    }

}
