package uk.ac.ebi.spot.gwas.deposition.rest.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.SubmissionType;
import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.FileUploadDto;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.FileUploadDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.*;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;
import uk.ac.ebi.spot.gwas.deposition.util.HeadersUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = GWASDepositionBackendConstants.API_V1 + GWASDepositionBackendConstants.API_SUBMISSIONS)
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

    /*
     * POST /v1/submissions/{submissionId}/uploads
     */
    @PostMapping(
            value = "/{submissionId}" + GWASDepositionBackendConstants.API_UPLOADS,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public Resource<FileUploadDto> uploadFile(@RequestParam MultipartFile file,
                                              @PathVariable String submissionId,
                                              HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to upload a new file [{}] to submission: {}", user.getName(), file.getOriginalFilename(), submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);
        FileUpload fileUpload;
        if (submission.getType().equals(SubmissionType.SUMMARY_STATS.name())) {
            fileUpload = fileHandlerService.handleSummaryStatsFile(submission, file);
        } else {
            fileUpload = fileHandlerService.handleMetadataFile(submission, file, user);
        }

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
    public Resource<FileUploadDto> getFileUpload(@PathVariable String submissionId,
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

        final ControllerLinkBuilder lb = linkTo(
                methodOn(FileUploadsController.class).getFileUpload(submissionId, fileUploadId, null));

        Resource<FileUploadDto> resource = new Resource<>(FileUploadDtoAssembler.assemble(fileUpload, null));
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withSelfRel());
        return resource;
    }

    /**
     * GET /v1/submissions/{submissionId}/uploads
     */
    @GetMapping(value = "/{submissionId}" + GWASDepositionBackendConstants.API_UPLOADS,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Resources<FileUploadDto> getFileUploads(@PathVariable String submissionId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve files from submission: {}", user.getName(), submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);
        List<FileUpload> fileUploads = fileUploadsService.getFileUploads(submission.getFileUploads());
        log.info("Returning {} files for submission: {}", fileUploads.size(), submission.getId());

        List<FileUploadDto> result = new ArrayList<>();
        for (FileUpload fileUpload : fileUploads) {
            FileUploadDto fileUploadDto = FileUploadDtoAssembler.assemble(fileUpload, null);
            final ControllerLinkBuilder lb = linkTo(methodOn(FileUploadsController.class).getFileUpload(submissionId, fileUpload.getId(), null));
            fileUploadDto.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withSelfRel());
            result.add(fileUploadDto);
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
        Submission submission = submissionService.getSubmission(submissionId, user);
        submissionService.deleteSubmissionFile(submission, fileUploadId, user.getId());
//        submissionDataCleaningService.cleanSubmission(submission, fileUploadId);
        log.info("File [{}] successfully removed from submission: {}", fileUploadId, submission.getId());
    }
}
