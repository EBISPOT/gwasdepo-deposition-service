package uk.ac.ebi.spot.gwas.deposition.rest.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditHelper;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditProxy;
import uk.ac.ebi.spot.gwas.deposition.audit.constants.PublicationEventType;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.DataType;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.constants.SubmissionType;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.domain.PgsValidationInfo;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.FileUploadDto;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.ExtendedFileUploadDto;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.ExtendedFileUploadDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.FileUploadDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.*;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;
import uk.ac.ebi.spot.gwas.deposition.util.HeadersUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_SUBMISSIONS)
public class FileUploadsController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadsController.class);

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private FileHandlerService fileHandlerService;

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private AuditProxy auditProxy;

    @Autowired
    private PublicationAuditService publicationAuditService;

    @Autowired
    private SubmissionDataCleaningService submissionDataCleaningService;

    @Autowired
    private PgsValidationInfoService pgsValidationInfoService;

    @Autowired
    private PgsValidationClient pgsValidationClient;

    /*
     * POST /v1/submissions/{submissionId}/uploads
     *
     * For GWAS → original flow.
     * For PGS  → store minimally (no template handlers), call PGS validator, persist status.
     */
    @PostMapping(
            value = "/{submissionId}" + GWASDepositionBackendConstants.API_UPLOADS,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Resource<ExtendedFileUploadDto> uploadFile(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "data-type", defaultValue = "GWAS") DataType dataType,
                                                      @PathVariable String submissionId,
                                                      HttpServletRequest request) throws IOException {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        Submission submission = submissionService.getSubmission(submissionId, user);

        FileUpload fileUpload;
        PgsValidationInfo pvi = null;

        if (dataType == DataType.PGS) {
            fileUpload = new FileUpload();
            fileUpload.setStatus("UPLOADED");
            fileUpload.setType("METADATA");
            fileUpload.setFileName(file.getOriginalFilename());
            fileUpload.setFileSize(file.getSize());
            fileUpload.setErrors(Collections.<String>emptyList());

            fileUpload = fileUploadsService.save(fileUpload); // persist

            String submissionEvent = String.format("SubmissionId-%s", submissionId);
            publicationAuditService.createAuditEvent(PublicationEventType.TEMPLATE_UPLOAD.name(),
                    submissionId, submissionEvent, false, user);

            Map<String, Object> rsp = pgsValidationClient.validate(file, request);

            @SuppressWarnings("unchecked")
            List<String> pcstIds = rsp.containsKey("pcstIds")
                    ? (List<String>) rsp.get("pcstIds")
                    : Collections.<String>emptyList();

            String newStatus = (String) rsp.get("validationStatus");
            fileUpload.setStatus(newStatus != null ? newStatus : "ERROR");
            fileUploadsService.save(fileUpload);

            pvi = new PgsValidationInfo(fileUpload.getId(), fileUpload.getStatus(), pcstIds);
            pgsValidationInfoService.save(pvi);

        } else {
            if (submission.getType().equals(SubmissionType.SUMMARY_STATS.name())) {
                fileUpload = fileHandlerService.handleSummaryStatsFile(submission, file, user);
                String submissionEvent = String.format("SubmissionId-%s prefilled", submissionId);
                publicationAuditService.createAuditEvent(PublicationEventType.TEMPLATE_UPLOAD.name(),
                        submissionId, submissionEvent, false, user);
            } else {
                fileUpload = fileHandlerService.handleMetadataFile(submission, file, user, null, null);
                String submissionEvent = String.format("SubmissionId-%s", submissionId);
                publicationAuditService.createAuditEvent(PublicationEventType.TEMPLATE_UPLOAD.name(),
                        submissionId, submissionEvent, false, user);
            }
        }

        auditProxy.addAuditEntry(AuditHelper.fileCreate(submission.getCreated().getUserId(), fileUpload, submission, true, null));

        final ControllerLinkBuilder lb = linkTo(
                methodOn(FileUploadsController.class).getFileUpload(submissionId, fileUpload.getId(), null));

        Resource<ExtendedFileUploadDto> resource = new Resource<>(
                ExtendedFileUploadDtoAssembler.assemble(fileUpload, null, pvi));
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withSelfRel());
        return resource;
    }

    /*
     * POST /v1/submissions/{submissionId}/uploads/edit
     */
    @PostMapping(
            value = "/{submissionId}" + GWASDepositionBackendConstants.API_EDIT_UPLOADS,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaTypes.HAL_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public Resource<FileUploadDto> uploadEditedFile(@RequestParam MultipartFile file,
                                                    @PathVariable String submissionId,
                                                    HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to upload a new file [{}] to submission: {}", user.getName(), file.getOriginalFilename(), submissionId);

        FileUpload fileUpload;

        List<Study> oldStudies = submissionService.getStudies(submissionId);
        submissionService.deleteSubmissionChildren(submissionId);
        Submission submission = submissionService.editFileUploadSubmissionDetails(submissionId, user);
        if (submission.getType().equals(SubmissionType.SUMMARY_STATS.name())) {
            fileUpload = fileHandlerService.handleSummaryStatsFile(submission, file, user);
        } else {
            fileUpload = fileHandlerService.handleMetadataFile(submission, file, user, oldStudies, "depo-curation");
        }
        auditProxy.addAuditEntry(AuditHelper.fileCreate(submission.getCreated().getUserId(), fileUpload, submission, true, null));

        final ControllerLinkBuilder lb = linkTo(
                methodOn(FileUploadsController.class).getFileUpload(submissionId, fileUpload.getId(), null));

        Resource<FileUploadDto> resource = new Resource<>(FileUploadDtoAssembler.assemble(fileUpload, null));
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withSelfRel());
        return resource;
    }

    /**
     * GET /v1/submissions/{submissionId}/uploads/{fileUploadId}
     */
    @GetMapping(value = "/{submissionId}" + GWASDepositionBackendConstants.API_UPLOADS + "/{fileUploadId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Resource<ExtendedFileUploadDto> getFileUpload(@PathVariable String submissionId,
                                                         @PathVariable String fileUploadId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve file [{}] from submission: {}", user.getName(), fileUploadId, submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);
        if (!submission.getFileUploads().contains(fileUploadId)) {
            log.error("Submission [{}] does not contain the file: {}", submissionId, fileUploadId);
            throw new EntityNotFoundException("Submission [" + submissionId + "] does not contain the file: " + fileUploadId);
        }
        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadId);
        log.info("Returning file [{}] for submission: {}", fileUpload.getFileName(), submission.getId());
        auditProxy.addAuditEntry(AuditHelper.fileRetrieve(submission.getCreated().getUserId(), fileUpload, submission));
        PgsValidationInfo pvi = pgsValidationInfoService
                .get(fileUploadId)
                .orElse(null);

        final ControllerLinkBuilder lb = linkTo(
                methodOn(FileUploadsController.class).getFileUpload(submissionId, fileUploadId, null));

        Resource<ExtendedFileUploadDto> resource = new Resource<>(
                ExtendedFileUploadDtoAssembler.assemble(fileUpload, null, pvi));
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withSelfRel());
        return resource;
    }

    /**
     * GET /v1/submissions/{submissionId}/uploads
     */
    @GetMapping(value = "/{submissionId}" + GWASDepositionBackendConstants.API_UPLOADS,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Resources<Resource<ExtendedFileUploadDto>> getFileUploads(@PathVariable String submissionId,
                                                                     HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve files from submission: {}", user.getName(), submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);
        List<FileUpload> fileUploads = fileUploadsService.getFileUploads(submission.getFileUploads());
        log.info("Returning {} files for submission: {}", fileUploads.size(), submission.getId());

        List<Resource<ExtendedFileUploadDto>> result = new ArrayList<>();
        for (FileUpload fu : fileUploads) {
            PgsValidationInfo pvi = pgsValidationInfoService
                    .get(fu.getId())
                    .orElse(null);

            ExtendedFileUploadDto dto =
                    ExtendedFileUploadDtoAssembler.assemble(fu, null, pvi);

            Resource<ExtendedFileUploadDto> res = new Resource<>(dto);
            res.add(BackendUtil.underBasePath(
                    linkTo(methodOn(FileUploadsController.class).getFileUpload(submissionId, fu.getId(), null)),
                    gwasDepositionBackendConfig.getProxyPrefix()).withSelfRel());
            result.add(res);
        }

        final ControllerLinkBuilder lb = linkTo(methodOn(FileUploadsController.class).getFileUploads(submissionId, null));
        return new Resources<>(result, BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withSelfRel());
    }

    /**
     * GET /v1/submissions/{submissionId}/uploads/{fileUploadId}/download
     */
    @GetMapping(value = "/{submissionId}" + GWASDepositionBackendConstants.API_UPLOADS +
            "/{fileUploadId}" + GWASDepositionBackendConstants.API_DOWNLOAD,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public HttpEntity<byte[]> downloadFile(@PathVariable String submissionId,
                                           @PathVariable String fileUploadId,
                                           HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to download file [{}] from submission: {}", user.getName(), fileUploadId, submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);
        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadId);
        auditProxy.addAuditEntry(AuditHelper.fileRetrieve(user.getId(), fileUpload, submission));
        byte[] payload = fileUploadsService.retrieveFileContent(fileUpload.getId());
        log.info("Returning content for file [{}] for submission: {}", fileUpload.getFileName(), submission.getId());

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileUpload.getFileName());
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        responseHeaders.add(HttpHeaders.CONTENT_LENGTH, Integer.toString(payload.length));
        return new HttpEntity<>(payload, responseHeaders);
    }

    /**
     * DELETE /v1/submissions/{submissionId}/uploads/{fileUploadId}
     */
    @DeleteMapping(value = "/{submissionId}" + GWASDepositionBackendConstants.API_UPLOADS + "/{fileUploadId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteFileUpload(@PathVariable String submissionId,
                                 @PathVariable String fileUploadId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to delete file [{}] from submission: {}", user.getName(), fileUploadId, submissionId);
        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadId);
        Submission submission = submissionService.getSubmission(submissionId, user);
        submissionService.deleteSubmissionFile(submission, fileUploadId, user.getId());
        auditProxy.addAuditEntry(AuditHelper.fileDelete(submission.getCreated().getUserId(), fileUpload, submission));
        submissionDataCleaningService.cleanSubmission(submission);
        log.info("File [{}] successfully removed from submission: {}", fileUploadId, submission.getId());
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = GWASDepositionBackendConstants.API_UPLOADS)
    public HttpEntity<byte[]> getTemplateForSubmissionByCallbackId(@RequestParam String callbackId) {
        FileUpload fileUpload = fileUploadsService.getFileUploadByCallbackId(callbackId);
        if (fileUpload == null) {
            throw new EntityNotFoundException("Fileupload not found for callbackId " + callbackId);
        }
        byte[] payload = fileUploadsService.retrieveFileContent(fileUpload.getId());
        log.info("Returning content for file [{}] for callbackId: {}", fileUpload.getFileName(), callbackId);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileUpload.getFileName());
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        responseHeaders.add(HttpHeaders.CONTENT_LENGTH, Integer.toString(payload.length));
        return new HttpEntity<>(payload, responseHeaders);
    }
}
