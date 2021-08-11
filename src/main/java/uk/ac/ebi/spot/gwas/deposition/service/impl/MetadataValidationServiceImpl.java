package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditHelper;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditProxy;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.constants.SubmissionType;
import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaResponseDto;
import uk.ac.ebi.spot.gwas.deposition.service.*;
import uk.ac.ebi.spot.gwas.deposition.util.ErrorUtil;
import uk.ac.ebi.spot.gwas.template.validator.config.ErrorType;
import uk.ac.ebi.spot.gwas.template.validator.domain.ValidationOutcome;
import uk.ac.ebi.spot.gwas.template.validator.service.TemplateValidatorService;
import uk.ac.ebi.spot.gwas.template.validator.util.ErrorMessageTemplateProcessor;
import uk.ac.ebi.spot.gwas.template.validator.util.StreamSubmissionTemplateReader;

import java.util.List;

@Service
public class MetadataValidationServiceImpl implements MetadataValidationService {

    private static final Logger log = LoggerFactory.getLogger(MetadataValidationService.class);

    @Autowired(required = false)
    private TemplateService templateService;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private TemplateValidatorService templateValidatorService;

    @Autowired
    private ErrorMessageTemplateProcessor errorMessageTemplateProcessor;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private AuditProxy auditProxy;

    @Override
    @Async
    public void validateTemplate(String submissionId, FileUpload fileUpload, byte[] fileContent, User user, List<Study> oldStudies) {
        log.info("[{}] Starting validation for file: {}", submissionId, fileUpload.getId());
        Submission submission = submissionService.getSubmission(submissionId, user);

        fileUpload.setStatus(FileUploadStatus.VALIDATING.name());
        fileUploadsService.save(fileUpload);
        submission.setMetadataStatus(Status.VALIDATING.name());
        submission.setOverallStatus(Status.VALIDATING.name());
        submissionService.saveSubmission(submission, user.getId());

        StreamSubmissionTemplateReader streamSubmissionTemplateReader = new StreamSubmissionTemplateReader(fileContent, fileUpload.getId());
        TemplateSchemaDto schema;
        log.info("[{} - {}] Template valid: {}", submissionId, fileUpload.getId(), streamSubmissionTemplateReader.isValid());
        if (streamSubmissionTemplateReader.isValid()) {
            log.info("[{} - {}] Template schema version: {}", submissionId, fileUpload.getId(),
                    streamSubmissionTemplateReader.getSchemaVersion());
            if (streamSubmissionTemplateReader.getSchemaVersion() != null) {
                log.info("Template service active: {}", (templateService != null));
                if (templateService != null) {
                    TemplateSchemaResponseDto templateSchemaResponseDto = templateService.retrieveTemplateSchemaInfo(streamSubmissionTemplateReader.getSchemaVersion());
                    if (templateSchemaResponseDto == null) {
                        log.error("Template service does not have a schema for the provided version: {}", streamSubmissionTemplateReader.getSchemaVersion());
                        materializeError(submission, fileUpload, ErrorType.NO_SCHEMA_VERSION, streamSubmissionTemplateReader.getSchemaVersion(), streamSubmissionTemplateReader, user);
                        return;
                    }
                    if (templateSchemaResponseDto.getSubmissionTypes().containsKey(SubmissionType.METADATA.name())) {
                        schema = templateService.retrieveTemplateSchema(streamSubmissionTemplateReader.getSchemaVersion(), SubmissionType.METADATA.name());
                        if (schema == null) {
                            log.error("Schema received from service is not usable.");
                            materializeError(submission, fileUpload, ErrorType.UNUSABLE_SCHEMA, null, streamSubmissionTemplateReader, user);
                            return;
                        }
                    } else {
                        log.error("Template service does not have a schema for the provided version: {}", streamSubmissionTemplateReader.getSchemaVersion());
                        materializeError(submission, fileUpload, ErrorType.NO_SCHEMA_VERSION, streamSubmissionTemplateReader.getSchemaVersion(), streamSubmissionTemplateReader, user);
                        return;
                    }
                } else {
                    materializeError(submission, fileUpload, ErrorType.NO_TEMPLATE_SERVICE, null, streamSubmissionTemplateReader, user);
                    return;
                }
            } else {
                this.materializeError(submission, fileUpload, ErrorType.NO_SCHEMA_PRESENT, null, streamSubmissionTemplateReader, user);
                return;
            }

            log.info("Validating metadata ...");
            ValidationOutcome validationOutcome = templateValidatorService.validate(streamSubmissionTemplateReader, schema, true);
            if (validationOutcome == null) {
                this.materializeError(submission, fileUpload, ErrorType.INVALID_TEMPLATE_DATA, null, streamSubmissionTemplateReader, user);
            } else {
                log.info("Validation outcome: {}", validationOutcome.getErrorMessages());
                if (validationOutcome.getErrorMessages().isEmpty()) {
                    auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), fileUpload, submission,
                            false, true, null));
                    conversionService.convertData(submission, fileUpload, streamSubmissionTemplateReader, schema, user.getId(), oldStudies);
                } else {
                    submission.setOverallStatus(Status.INVALID.name());
                    submission.setMetadataStatus(Status.INVALID.name());
                    submissionService.saveSubmission(submission, user.getId());

                    List<String> errors = ErrorUtil.transform(validationOutcome.getErrorMessages(), errorMessageTemplateProcessor);
                    fileUpload.setErrors(errors);
                    fileUpload.setStatus(FileUploadStatus.INVALID.name());
                    fileUploadsService.save(fileUpload);
                    streamSubmissionTemplateReader.close();
                    auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), fileUpload, submission, false, false, errors));
                    auditProxy.addAuditEntry(AuditHelper.submissionValidate(submission.getCreated().getUserId(), submission, false, errors));
                }
            }

            return;
        }

        this.materializeError(submission, fileUpload, ErrorType.INVALID_FILE, fileUpload.getFileName(), streamSubmissionTemplateReader, user);
    }

    private void materializeError(Submission submission, FileUpload fileUpload, String errorType, String context,
                                  StreamSubmissionTemplateReader streamSubmissionTemplateReader, User user) {
        submission.setOverallStatus(Status.INVALID.name());
        submission.setMetadataStatus(Status.INVALID.name());
        submissionService.saveSubmission(submission, user.getId());

        List<String> errors = errorMessageTemplateProcessor.processGenericError(errorType, context);
        fileUpload.setErrors(errors);
        fileUpload.setStatus(FileUploadStatus.INVALID.name());
        fileUploadsService.save(fileUpload);
        streamSubmissionTemplateReader.close();
        auditProxy.addAuditEntry(AuditHelper.submissionValidate(submission.getCreated().getUserId(), submission, false, errors));
        auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), fileUpload, submission, false, false, errors));
    }
}
