package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditHelper;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditProxy;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadType;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.constants.SubmissionType;
import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaResponseDto;
import uk.ac.ebi.spot.gwas.deposition.service.*;
import uk.ac.ebi.spot.gwas.deposition.util.ErrorUtil;
import uk.ac.ebi.spot.gwas.template.validator.config.ErrorType;
import uk.ac.ebi.spot.gwas.template.validator.config.ValidatorConfig;
import uk.ac.ebi.spot.gwas.template.validator.domain.ValidationOutcome;
import uk.ac.ebi.spot.gwas.template.validator.service.TemplateContentValidatorService;
import uk.ac.ebi.spot.gwas.template.validator.service.TemplateValidatorService;
import uk.ac.ebi.spot.gwas.template.validator.util.ErrorMessageTemplateProcessor;
import uk.ac.ebi.spot.gwas.template.validator.util.StreamSubmissionTemplateReader;

import java.util.List;

@Service
public class SummaryStatsValidationServiceImpl implements SummaryStatsValidationService {

    private static final Logger log = LoggerFactory.getLogger(SummaryStatsValidationService.class);

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired(required = false)
    private TemplateService templateService;

    @Autowired
    private TemplateValidatorService templateValidatorService;

    @Autowired
    private ErrorMessageTemplateProcessor errorMessageTemplateProcessor;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private TemplateContentValidatorService templateContentValidatorService;

    @Autowired
    private ValidatorConfig validatorConfig;

    @Autowired
    private AuditProxy auditProxy;

    @Async
    @Override
    public void validateSummaryStatsData(Submission submission, User user) {
        log.info("Validating summary stats [{}] using files: {}", submission.getId(), submission.getFileUploads());

        submission.setSummaryStatsStatus(Status.VALIDATING.name());
        submission.setOverallStatus(Status.VALIDATING.name());
        submissionService.saveSubmission(submission, user.getId());

        List<FileUpload> fileUploads = fileUploadsService.getFileUploads(submission.getFileUploads());
        log.info("Retrieved {} file uploads.", fileUploads.size());

        FileUpload templateFile = null;
        FileUpload dataFile = null;
        for (FileUpload fileUpload : fileUploads) {
            if (fileUpload.getType().equals(FileUploadType.SUMMARY_STATS_TEMPLATE.name())) {
                templateFile = fileUpload;
            }
            if (fileUpload.getType().equals(FileUploadType.SUMMARY_STATS.name())) {
                dataFile = fileUpload;
            }
        }

        if (templateFile == null || dataFile == null) {
            log.error("Unable to find summary stats template or data file for submission: {}", submission.getId());
            markInvalidFile(dataFile, submission, ErrorType.NO_TEMPLATE, null, null, null, user);
            return;
        }

        byte[] dataFileContent = fileUploadsService.retrieveFileContent(dataFile.getId());
        byte[] schemaFileContent = fileUploadsService.retrieveFileContent(templateFile.getId());
        StreamSubmissionTemplateReader dataReader = new StreamSubmissionTemplateReader(dataFileContent, dataFile.getId());
        StreamSubmissionTemplateReader schemaReader = new StreamSubmissionTemplateReader(schemaFileContent, templateFile.getId());
        log.info("Pre-filled data template valid: {}", dataReader.isValid());
        if (dataReader.isValid()) {
            log.info("Pre-filled data template schema version and type: {} | {}",
                    dataReader.getSchemaVersion(),
                    dataReader.getSubmissionType());

            log.info("Pre-filled schema template schema version and type: {} | {}",
                    schemaReader.getSchemaVersion(),
                    schemaReader.getSubmissionType());

            if (dataReader.getSchemaVersion() == null) {
                this.markInvalidFile(dataFile, submission, ErrorType.NO_SCHEMA_PRESENT, null, dataReader, schemaReader, user);
                return;
            }

            if (!schemaReader.getSchemaVersion().equalsIgnoreCase(dataReader.getSchemaVersion()) ||
                    !schemaReader.getSubmissionType().equalsIgnoreCase(dataReader.getSubmissionType())) {
                log.error("Schema and data templates do not match!");
                markInvalidFile(dataFile, submission, ErrorType.TEMPLATE_MISMATCH, null, dataReader, schemaReader, user);
                return;
            }

            log.info("Template service active: {}", (templateService != null));
            TemplateSchemaDto schema;
            if (templateService != null) {
                TemplateSchemaResponseDto templateSchemaResponseDto = templateService.retrieveTemplateSchemaInfo(dataReader.getSchemaVersion());
                if (templateSchemaResponseDto == null) {
                    log.error("Template service does not have a schema for the provided version: {}", dataReader.getSchemaVersion());
                    markInvalidFile(dataFile, submission, ErrorType.NO_SCHEMA_VERSION, dataReader.getSchemaVersion(), dataReader, schemaReader, user);
                    return;
                }
                if (templateSchemaResponseDto.getSubmissionTypes().containsKey(SubmissionType.SUMMARY_STATS.name())) {
                    schema = templateService.retrieveTemplateSchema(dataReader.getSchemaVersion(), SubmissionType.SUMMARY_STATS.name());
                    if (schema == null) {
                        log.error("Schema received from the template service is not usable.");
                        markInvalidFile(dataFile, submission, ErrorType.UNUSABLE_SCHEMA, null, dataReader, schemaReader, user);
                        return;
                    }
                } else {
                    log.error("Template service does not have a schema for the provided version: {}", dataReader.getSchemaVersion());
                    markInvalidFile(dataFile, submission, ErrorType.NO_SCHEMA_VERSION, dataReader.getSchemaVersion(), dataReader, schemaReader, user);
                    return;
                }
            } else {
                log.error("Template service is not active. Cannot validate data.");
                markInvalidFile(dataFile, submission, ErrorType.NO_TEMPLATE_SERVICE, null, dataReader, schemaReader, user);
                return;
            }

            ValidationOutcome validationOutcome = templateValidatorService.validate(dataReader, schema, false);
            if (validationOutcome == null) {
                this.markInvalidFile(dataFile, submission, ErrorType.INVALID_FILE, dataFile.getFileName(), dataReader, schemaReader, user);
            } else {
                log.info("Validation outcome: {}", validationOutcome.getErrorMessages());
                if (validationOutcome.getErrorMessages().isEmpty()) {
                    log.info("SS content validation enabled: {}", validatorConfig.isSsContentValidationEnabled());
                    if (validatorConfig.isSsContentValidationEnabled()) {
                        validationOutcome = templateContentValidatorService.validate(schemaReader, dataReader,
                                validatorConfig.getSsContentValidationSheet(),
                                validatorConfig.getSsContentValidationColumn(), schema);
                        if (validationOutcome == null) {
                            this.markInvalidFile(dataFile, submission, ErrorType.INVALID_FILE, dataFile.getFileName(), dataReader, schemaReader, user);
                        }
                        if (validationOutcome.getErrorMessages().isEmpty()) {
                            auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), dataFile, submission, true, true, null));
                            conversionService.convertData(submission, dataFile, dataReader, schema, user.getId());
                        } else {
                            submission.setOverallStatus(Status.INVALID.name());
                            submission.setSummaryStatsStatus(Status.INVALID.name());
                            submissionService.saveSubmission(submission, user.getId());

                            List<String> errors = errorMessageTemplateProcessor.processGenericError(ErrorType.STUDY_DATA_MISMATCH,
                                    StringUtils.join(validationOutcome.getErrorMessages().get(validatorConfig.getSsContentValidationSheet()), ","));
                            dataFile.setErrors(errors);
                            dataFile.setStatus(FileUploadStatus.INVALID.name());
                            fileUploadsService.save(dataFile);
                            auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), dataFile, submission, true, false, errors));
                        }
                    } else {
                        auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), dataFile, submission, true, true, null));
                        conversionService.convertData(submission, dataFile, dataReader, schema, user.getId());
                    }
                } else {
                    submission.setOverallStatus(Status.INVALID.name());
                    submission.setSummaryStatsStatus(Status.INVALID.name());
                    submissionService.saveSubmission(submission, user.getId());

                    List<String> errors = ErrorUtil.transform(validationOutcome.getErrorMessages(), errorMessageTemplateProcessor);
                    dataFile.setErrors(errors);
                    dataFile.setStatus(FileUploadStatus.INVALID.name());
                    fileUploadsService.save(dataFile);
                    dataReader.close();
                    schemaReader.close();
                    auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), dataFile, submission, true, false, errors));
                    auditProxy.addAuditEntry(AuditHelper.submissionValidate(submission.getCreated().getUserId(), submission, false, errors));
                }
            }

            return;
        }

        markInvalidFile(dataFile, submission, ErrorType.INVALID_FILE, dataFile.getFileName(), dataReader, schemaReader, user);
    }

    private void markInvalidFile(FileUpload fileUpload, Submission submission, String errorType, String context,
                                 StreamSubmissionTemplateReader dataReader,
                                 StreamSubmissionTemplateReader schemaReader, User user) {
        List<String> errors = errorMessageTemplateProcessor.processGenericError(errorType, context);
        fileUpload.setErrors(errors);
        fileUpload.setStatus(FileUploadStatus.INVALID.name());
        fileUploadsService.save(fileUpload);
        auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), fileUpload, submission, true, false, errors));

        submission.setSummaryStatsStatus(Status.INVALID.name());
        submission.setOverallStatus(Status.INVALID.name());
        auditProxy.addAuditEntry(AuditHelper.submissionValidate(submission.getCreated().getUserId(), submission, false, errors));
        submissionService.saveSubmission(submission, user.getId());
        if (dataReader != null) {
            dataReader.close();
        }
        if (schemaReader != null) {
            schemaReader.close();
        }
    }
}
