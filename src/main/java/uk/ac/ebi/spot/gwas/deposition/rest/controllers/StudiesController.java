package uk.ac.ebi.spot.gwas.deposition.rest.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.FileObject;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.StudyDto;
import uk.ac.ebi.spot.gwas.deposition.exception.FileProcessingException;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.StudyDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.*;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;
import uk.ac.ebi.spot.gwas.deposition.util.HeadersUtil;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_SUBMISSIONS)
public class StudiesController {

    private static final Logger log = LoggerFactory.getLogger(StudiesController.class);

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private StudiesService studiesService;

    @Autowired
    private StudyDtoAssembler studyDtoAssembler;

    @Autowired
    private TemplatePrefillService templatePrefillService;

    /**
     * GET /v1/submissions/{submissionId}/studies
     */
    @GetMapping(value = "/{submissionId}" + GWASDepositionBackendConstants.API_STUDIES,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public PagedResources<StudyDto> getStudies(@PathVariable String submissionId, HttpServletRequest request,
                                               @PageableDefault(size = 20, page = 0) Pageable pageable,
                                               PagedResourcesAssembler assembler) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve studies for submission: {}", user.getName(), submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);

        Page<Study> facetedSearchStudies = studiesService.getStudies(submission, pageable);
        log.info("Returning {} studies.", facetedSearchStudies.getTotalElements());

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(StudiesController.class).getStudies(submissionId, null, pageable, assembler));
        return assembler.toResource(facetedSearchStudies, studyDtoAssembler,
                new Link(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).toUri().toString()));

    }

    /**
     * GET /v1/submissions/{submissionId}/study-envelopes
     */
    /*
    @GetMapping(value = "/{submissionId}" + GWASDepositionBackendConstants.API_STUDY_ENVELOPES,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public HttpEntity<byte[]> getStudyEnvelopes(@PathVariable String submissionId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve studies for submission: {}", user.getName(), submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);

        FileObject fileObject = templatePrefillService.prefillGCST(submissionId, user);
        if (fileObject == null) {
            throw new FileProcessingException("Unable to process request.");
        }
        log.info("Returning content for file [gcst_list.xlsx] for submission: {}", submission.getId());

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=gcst_list.xlsx");
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        responseHeaders.add(HttpHeaders.CONTENT_LENGTH, Integer.toString(fileObject.getContent().length));
        return new HttpEntity<>(fileObject.getContent(), responseHeaders);
    }

    @GetMapping(value = "/{submissionId}" + GWASDepositionBackendConstants.API_STUDY_ENVELOPES,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public PagedResources<StudyEnvelopeDto> getStudyEnvelopes(@PathVariable String submissionId, HttpServletRequest request,
                                                              @PageableDefault(size = 20, page = 0) Pageable pageable,
                                                              PagedResourcesAssembler assembler) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve studies for submission: {}", user.getName(), submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);

        Page<Study> facetedSearchStudies = studiesService.getStudies(submission, pageable);
        log.info("Returning {} studies.", facetedSearchStudies.getTotalElements());

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(StudiesController.class).getStudyEnvelopes(submissionId, null, pageable, assembler));
        return assembler.toResource(facetedSearchStudies, studyEnvelopeDtoAssembler,
                new Link(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).toUri().toString()));
    }
    */
}
