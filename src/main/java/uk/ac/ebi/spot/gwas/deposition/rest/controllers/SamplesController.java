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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.Sample;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.SampleDto;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SampleDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.JWTService;
import uk.ac.ebi.spot.gwas.deposition.service.SamplesService;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;
import uk.ac.ebi.spot.gwas.deposition.util.HeadersUtil;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_SUBMISSIONS)
public class SamplesController {

    private static final Logger log = LoggerFactory.getLogger(SamplesController.class);

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private SamplesService samplesService;

    @Autowired
    private SampleDtoAssembler sampleDtoAssembler;

    /**
     * GET /v1/submissions/{submissionId}/samples
     */
    @GetMapping(value = "/{submissionId}" + GWASDepositionBackendConstants.API_SAMPLES,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public PagedResources<SampleDto> getSamples(@PathVariable String submissionId, HttpServletRequest request,
                                                @PageableDefault(size = 20, page = 0) Pageable pageable,
                                                PagedResourcesAssembler assembler) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve samples for submission: {}", user.getName(), submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);

        Page<Sample> facetedSearchStudies = samplesService.getSamples(submission, pageable);
        log.info("Returning {} samples.", facetedSearchStudies.getTotalElements());

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(SamplesController.class).getSamples(submissionId, null, pageable, assembler));
        return assembler.toResource(facetedSearchStudies, sampleDtoAssembler,
                new Link(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).toUri().toString()));
    }

}
