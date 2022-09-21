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
import org.springframework.hateoas.MediaTypes;
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
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SubmissionPatchDto;
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
    private SummaryStatsProcessingService summaryStatsProcessingService;

    @Autowired
    private AuditProxy auditProxy;

    @Autowired
    private CuratorAuthService curatorAuthService;


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
        log.info("[{}] Request to create new submission.", user.getName());
        if (submissionCreationDto.getPublication() == null && submissionCreationDto.getBodyOfWork() == null) {
            log.error("Submission is missing body payload.");
            throw new InvalidSubmissionTypeException("Submission is missing body payload.");
        }

        String globusFolder = UUID.randomUUID().toString();
        SSGlobusResponse outcome = sumStatsService.createGlobusFolder(new SSGlobusFolderDto(globusFolder,
                submissionCreationDto.getGlobusIdentity() != null ?
                        submissionCreationDto.getGlobusIdentity() :
                        user.getEmail()));
        if (outcome != null) {
            if (!outcome.isValid()) {
                auditProxy.addAuditEntry(AuditHelper.globusCreate(user.getId(), false, outcome));
                log.error("Unable to create Globus folder: {}", outcome.getOutcome());
                throw new EmailAccountNotLinkedToGlobusException(outcome.getOutcome());
            }
        } else {
            auditProxy.addAuditEntry(AuditHelper.globusCreate(user.getId(), false, null));
            throw new SSGlobusFolderCreatioException("Sorry! There is a fault on our end. Please contact gwas-subs@ebi.ac.uk for help.");
        }

        if (submissionCreationDto.getBodyOfWork() != null) {
            log.info("Received submission based on body of work: {}", submissionCreationDto.getBodyOfWork().getTitle());
            BodyOfWork bodyOfWork = BodyOfWorkDtoDisassembler.disassemble(submissionCreationDto.getBodyOfWork(),
                    new Provenance(DateTime.now(), user.getId()));
            bodyOfWork = bodyOfWorkService.retrieveBodyOfWork(bodyOfWork.getBowId(), user);

            Submission submission = new Submission(bodyOfWork.getBowId(),
                    SubmissionProvenanceType.BODY_OF_WORK.name(),
                    new Provenance(DateTime.now(), user.getId()));
            submission.setGlobusFolderId(globusFolder);
            submission.setGlobusOriginId(outcome.getOutcome());
            submission.setType(SubmissionType.METADATA.name());
            submission.setAgreedToCc0(submissionCreationDto.isAgreedToCc0());
            auditProxy.addAuditEntry(AuditHelper.submissionCreateBOW(user.getId(), submission, bodyOfWork, true, true));

            Submission existing = submissionService.findByBodyOfWork(bodyOfWork.getBowId(), user.getId());
            if (existing != null) {
                log.error("Unable to create submission using: {}. A submission already exists.", bodyOfWork.getBowId());
                auditProxy.addAuditEntry(AuditHelper.submissionCreateBOW(user.getId(), submission, bodyOfWork, false, false));
                throw new CannotCreateSubmissionOnExistingBodyOfWork("Unable to create submission using: " + bodyOfWork.getBowId() + ". A submission already exists.");
            }

            submission = submissionService.createSubmission(submission);
            bodyOfWork.setStatus(BodyOfWorkStatus.UNDER_SUBMISSION.name());
            bodyOfWorkService.save(bodyOfWork);
            auditProxy.addAuditEntry(AuditHelper.submissionCreateBOW(user.getId(), submission, bodyOfWork, false, true));
            return submissionAssemblyService.toResource(submission);
        }

        Publication publication = publicationService.retrievePublication(submissionCreationDto.getPublication().getPmid(), false);
        if (publication.getStatus().equals(PublicationStatus.ELIGIBLE.name()) ||
                publication.getStatus().equals(PublicationStatus.PUBLISHED.name())) {

            Submission submission = new Submission(publication.getId(),
                    SubmissionProvenanceType.PUBLICATION.name(),
                    new Provenance(DateTime.now(), user.getId()));

            submission.setGlobusFolderId(globusFolder);
            submission.setGlobusOriginId(outcome.getOutcome());
            submission.setAgreedToCc0(submissionCreationDto.isAgreedToCc0());
            auditProxy.addAuditEntry(AuditHelper.submissionCreatePub(user.getId(),
                    submission, publication, true, true, null));

            if (publication.getStatus().equals(PublicationStatus.ELIGIBLE.name())) {
                publication.setStatus(PublicationStatus.UNDER_SUBMISSION.name());
                submission.setType(SubmissionType.METADATA.name());
                submission = submissionService.createSubmission(submission);
                auditProxy.addAuditEntry(AuditHelper.submissionCreatePub(user.getId(),
                        submission, publication, false, true, null));
                publicationService.savePublication(publication);
            }
            if (publication.getStatus().equals(PublicationStatus.PUBLISHED.name())) {
                publication.setStatus(PublicationStatus.UNDER_SUMMARY_STATS_SUBMISSION.name());
                submission.setType(SubmissionType.SUMMARY_STATS.name());
                submission = submissionService.createSubmission(submission);
                auditProxy.addAuditEntry(AuditHelper.submissionCreatePub(user.getId(),
                        submission, publication, false, true, null));
                publicationService.savePublication(publication);
                fileHandlerService.handleSummaryStatsTemplate(submission, publication, user);
            }
            log.info("Returning new submission: {}", submission.getId());
            return submissionAssemblyService.toResource(submission);
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
        auditProxy.addAuditEntry(AuditHelper.submissionRetrieve(user.getId(), submission));
        log.info("Returning submission: {}", submission.getId());
        return submissionAssemblyService.toResource(submission);
    }

    /**
     * GET /v1/submissions/{submissionId}/prefill
     */
    /*
    @GetMapping(value = "/{submissionId}/prefill",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void prefill(@PathVariable String submissionId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to prefill submission: {}", user.getName(), submissionId);
        templatePrefillService.prefill(submissionId, user);
    }
    */

    /**
     * DELETE /v1/submissions/{submissionId}
     */
    @DeleteMapping(value = "/{submissionId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteSubmission(@PathVariable String submissionId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to delete submission: {}", user.getName(), submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);
        if (submission.getOverallStatus().equalsIgnoreCase(Status.DEPOSITION_COMPLETE.name())) {
            auditProxy.addAuditEntry(AuditHelper.submissionDelete(user.getId(), submission, false));
            log.error("Unable to DELETE submission [{}]. Submission has already been SUBMITTED.", submissionId);
            throw new DeleteOnSubmittedSubmissionNotAllowedException("Unable to DELETE submission [" + submissionId + "]. Submission has already been SUBMITTED.");
        }

        auditProxy.addAuditEntry(AuditHelper.submissionDelete(user.getId(), submission, true));
        submissionService.deleteSubmission(submissionId, user);

        if (submission.getPublicationId() != null) {
            Publication publication = publicationService.retrievePublication(submission.getPublicationId(), true);
            if (publication.getStatus().equalsIgnoreCase(PublicationStatus.UNDER_SUMMARY_STATS_SUBMISSION.name())) {
                publication.setStatus(PublicationStatus.PUBLISHED.name());
            } else {
                publication.setStatus(PublicationStatus.ELIGIBLE.name());
            }
            publicationService.savePublication(publication);
        }
        if (submission.getBodyOfWorks() != null) {
            for (String bowId : submission.getBodyOfWorks()) {
                BodyOfWork bodyOfWork = bodyOfWorkService.retrieveBodyOfWork(bowId, user);
                bodyOfWork.setStatus(BodyOfWorkStatus.NEW.name());
                bodyOfWorkService.save(bodyOfWork);
            }
        }
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
        Submission submission = submissionService.updateSubmissionStatus(submissionId, Status.DEPOSITION_COMPLETE.name(), user);
        auditProxy.addAuditEntry(AuditHelper.submissionSubmit(user.getId(), submission));
        log.info("Submissions successfully updated.");
        summaryStatsProcessingService.callGlobusWrapUp(submissionId);
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
                                                        @RequestParam(value = GWASDepositionBackendConstants.PARAM_BOWID,
                                                                required = false)
                                                                String bowId,
                                                        @PageableDefault(size = 20, page = 0) Pageable pageable,
                                                        PagedResourcesAssembler assembler) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve submissions: {} - {} - {} - {} - {}", user.getName(),
                pmid, bowId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().toString());
        Publication publication = pmid != null ? publicationService.retrievePublication(pmid, false) : null;
        Page<Submission> facetedSearchSubmissions = submissionService.getSubmissions(publication != null ?
                publication.getId() : null, bowId, pageable, user);
        log.info("Returning {} submissions.", facetedSearchSubmissions.getTotalElements());

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(SubmissionsController.class).getSubmissions(null, pmid, bowId, pageable, assembler));
        return assembler.toResource(facetedSearchSubmissions, submissionAssemblyService,
                new Link(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).toUri().toString()));
    }

    /**
     * PUT /v1/submissions/{submissionId}/lock?lockStatus=lock|unlock
     */
    @PutMapping(value = "/{submissionId}" + GWASDepositionBackendConstants.API_SUBMISSIONS_LOCK,
            produces = MediaTypes.HAL_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Resource<SubmissionDto> lockSubmission(@RequestParam String lockStatus,@PathVariable String submissionId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to submit submission: {}", user.getName(), submissionId);
        log.info("LockStatus is ->"+lockStatus+"!!");
        Submission submission = submissionService.getSubmission(submissionId, user);
        Submission lockSubmission = submissionService.lockSubmission(submission, user, lockStatus);
        return submissionAssemblyService.toResource(lockSubmission);
    }

    /**
     * PATCH /v1/submissions/{submissionId}
     * used to patch a submission (extend for fields as needed) and to create Globus folder if a user requests to reopen
     * a stale submission that was archived
     */
    @PatchMapping(value = "/{submissionId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Resource<SubmissionDto> patchSubmission(@PathVariable String submissionId,
                                                   HttpServletRequest request,
                                                   @RequestBody SubmissionPatchDto patchDto) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        Submission submission = new Submission();
        if (patchDto.getGlobusEmail() != null) {
            submission = submissionService
                    .createGlobusFolderForReopenedSubmission(submissionId, user, patchDto.getGlobusEmail());
        }
        return submissionAssemblyService.toResource(submission);
    }

    @GetMapping(value = "/{submissionId}/validate-snps")
    public void validateSnps(@PathVariable  String submissionId, HttpServletRequest request) {

        userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        submissionService.validateSnps(submissionId);
    }

}
