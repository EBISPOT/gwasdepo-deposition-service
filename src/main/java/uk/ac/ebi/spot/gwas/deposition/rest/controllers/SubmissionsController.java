package uk.ac.ebi.spot.gwas.deposition.rest.controllers;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditHelper;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditProxy;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.*;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionCreationDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSGlobusFolderDto;
import uk.ac.ebi.spot.gwas.deposition.exception.*;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.BodyOfWorkDtoDisassembler;
import uk.ac.ebi.spot.gwas.deposition.service.*;
import uk.ac.ebi.spot.gwas.deposition.service.impl.SubmissionAssemblyService;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;
import uk.ac.ebi.spot.gwas.deposition.util.HeadersUtil;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping(value = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_SUBMISSIONS)
public class SubmissionsController {

    private static final Logger log = LoggerFactory.getLogger(SubmissionsController.class);

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private SubmissionAssemblyService submissionAssemblyService;

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private FileHandlerService fileHandlerService;

    @Autowired
    private SumStatsService sumStatsService;

    @Autowired
    private BodyOfWorkService bodyOfWorkService;

    @Autowired
    private AuditProxy auditProxy;

    /**
     * POST /v1/submissions
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public Resource<SubmissionDto> createSubmission(@Valid @RequestBody SubmissionCreationDto submissionCreationDto,
                                                    HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to create new submission for publication: {}", user.getName(),
                submissionCreationDto.getPublication().getPmid());

        if (submissionCreationDto.getPublication() == null) {
            if (submissionCreationDto.getBodyOfWork() == null) {
                throw new InvalidSubmissionTypeException("Submission is missing body payload.");
            }
            log.info("Received submission based on body of work: {}", submissionCreationDto.getBodyOfWork().getTitle());
            BodyOfWork bodyOfWork = BodyOfWorkDtoDisassembler.disassemble(submissionCreationDto.getBodyOfWork(),
                    new Provenance(DateTime.now(), user.getId()));
            bodyOfWork = bodyOfWorkService.retrieveBodyOfWork(bodyOfWork.getId(), user.getId());
//            auditProxy.addAuditEntry(AuditHelper.manuscriptCreated(user.getId(), manuscript));

            Submission submission = new Submission(bodyOfWork.getId(),
                    SubmissionProvenanceType.BODY_OF_WORK.name(),
                    new Provenance(DateTime.now(), user.getId()));

            submission = submissionService.createSubmission(submission);
//            auditProxy.addAuditEntry(AuditHelper.submissionCreated(user.getId(), submission, manuscript));
            return submissionAssemblyService.toResource(submission);
        }

        Publication publication = publicationService.retrievePublication(submissionCreationDto.getPublication().getPmid(), false);
        if (publication.getStatus().equals(PublicationStatus.ELIGIBLE.name()) ||
                publication.getStatus().equals(PublicationStatus.PUBLISHED.name())) {

            Submission submission = new Submission(publication.getId(),
                    SubmissionProvenanceType.PUBLICATION.name(),
                    new Provenance(DateTime.now(), user.getId()));
            String globusFolder = UUID.randomUUID().toString();
            SSGlobusResponse outcome = sumStatsService.createGlobusFolder(new SSGlobusFolderDto(globusFolder,
                    submissionCreationDto.getGlobusIdentity() != null ?
                            submissionCreationDto.getGlobusIdentity() :
                            user.getEmail()));
            if (outcome != null) {
                if (!outcome.isValid()) {
                    auditProxy.addAuditEntry(AuditHelper.globusFailed(user.getId(), submissionCreationDto.getPublication(), outcome));
                    log.error("Unable to create Globus folder: {}", outcome.getOutcome());
                    throw new EmailAccountNotLinkedToGlobusException(outcome.getOutcome());
                }

                auditProxy.addAuditEntry(AuditHelper.globusSuccess(user.getEmail(), submissionCreationDto.getPublication(), outcome));
                submission.setGlobusFolderId(globusFolder);
                submission.setGlobusOriginId(outcome.getOutcome());

                if (publication.getStatus().equals(PublicationStatus.ELIGIBLE.name())) {
                    publication.setStatus(PublicationStatus.UNDER_SUBMISSION.name());
                    submission.setType(SubmissionType.METADATA.name());
                    submission = submissionService.createSubmission(submission);
                    auditProxy.addAuditEntry(AuditHelper.submissionCreated(user.getId(),
                            submission, submissionCreationDto.getPublication()));
                }
                if (publication.getStatus().equals(PublicationStatus.PUBLISHED.name())) {
                    publication.setStatus(PublicationStatus.UNDER_SUMMARY_STATS_SUBMISSION.name());
                    submission.setType(SubmissionType.SUMMARY_STATS.name());
                    submission = submissionService.createSubmission(submission);
                    fileHandlerService.handleSummaryStatsTemplate(submission, publication);
                }
                publicationService.savePublication(publication);
                log.info("Returning new submission: {}", submission.getId());
                return submissionAssemblyService.toResource(submission);
            } else {
                auditProxy.addAuditEntry(AuditHelper.globusFailed(user.getId(), submissionCreationDto.getPublication(), outcome));
                throw new SSGlobusFolderCreatioException("Sorry! There is a fault on our end. Please contact gwas-info@ebi.ac.uk for help.");
            }
        }

        throw new SubmissionOnUnacceptedPublicationTypeException("Submissions are only accepted on ELIGIBLE or PUBLISHED publications.");
    }

    /**
     * GET /v1/submissions/{submissionId}
     */
    @GetMapping(value = "/{submissionId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Resource<SubmissionDto> getSubmission(@PathVariable String submissionId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve submission: {}", user.getName(), submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);
        auditProxy.addAuditEntry(AuditHelper.submissionRetrieved(user.getId(), submission));
        log.info("Returning submission: {}", submission.getId());
        return submissionAssemblyService.toResource(submission);
    }

    /**
     * DELETE /v1/submissions/{submissionId}
     */
    @DeleteMapping(value = "/{submissionId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteSubmission(@PathVariable String submissionId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to delete submission: {}", user.getName(), submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);
        if (submission.getOverallStatus().equalsIgnoreCase(Status.SUBMITTED.name())) {
            auditProxy.addAuditEntry(AuditHelper.submissionDeleted(user.getId(), submission, false));
            log.error("Unable to DELETE submission [{}]. Submission has already been SUBMITTED.", submissionId);
            throw new DeleteOnSubmittedSubmissionNotAllowedException("Unable to DELETE submission [" + submissionId + "]. Submission has already been SUBMITTED.");
        }

        auditProxy.addAuditEntry(AuditHelper.submissionDeleted(user.getId(), submission, true));
        submissionService.deleteSubmission(submissionId, user);

        Publication publication = publicationService.retrievePublication(submission.getPublicationId(), true);
        if (publication.getStatus().equalsIgnoreCase(PublicationStatus.UNDER_SUMMARY_STATS_SUBMISSION.name())) {
            publication.setStatus(PublicationStatus.PUBLISHED.name());
        } else {
            publication.setStatus(PublicationStatus.ELIGIBLE.name());
        }
        publicationService.savePublication(publication);
        log.info("Submissions successfully deleted.");
    }

    /**
     * PUT /v1/submissions/{submissionId}/submit
     */
    @PutMapping(value = "/{submissionId}" + GWASDepositionBackendConstants.API_SUBMIT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Resource<SubmissionDto> updateSubmission(@PathVariable String submissionId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to submit submission: {}", user.getName(), submissionId);
        Submission submission = submissionService.updateSubmissionStatus(submissionId, Status.SUBMITTED.name(), user);
        auditProxy.addAuditEntry(AuditHelper.submissionSubmit(user.getId(), submission));
        log.info("Submissions successfully updated.");
        return submissionAssemblyService.toResource(submission);
    }

    /**
     * GET /v1/submissions
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public PagedResources<SubmissionDto> getSubmissions(HttpServletRequest request,
                                                        @RequestParam(value = GWASDepositionBackendConstants.PARAM_PMID,
                                                                required = false)
                                                                String pmid,
                                                        @PageableDefault(size = 20, page = 0) Pageable pageable,
                                                        PagedResourcesAssembler assembler) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve submissions: {} - {} - {} - {}", user.getName(),
                pmid, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().toString());
        Publication publication = pmid != null ? publicationService.retrievePublication(pmid, false) : null;
        Page<Submission> facetedSearchSubmissions = submissionService.getSubmissions(publication != null ?
                publication.getId() : null, pageable, user);
        log.info("Returning {} submissions.", facetedSearchSubmissions.getTotalElements());

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(SubmissionsController.class).getSubmissions(null, pmid, pageable, assembler));
        return assembler.toResource(facetedSearchSubmissions, submissionAssemblyService,
                new Link(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).toUri().toString()));
    }

}
