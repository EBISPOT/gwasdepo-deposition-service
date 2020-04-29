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
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditHelper;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditProxy;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.domain.Provenance;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.BodyOfWorkDto;
import uk.ac.ebi.spot.gwas.deposition.exception.CannotDeleteBodyOfWorkException;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.BodyOfWorkDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.BodyOfWorkDtoDisassembler;
import uk.ac.ebi.spot.gwas.deposition.service.BodyOfWorkService;
import uk.ac.ebi.spot.gwas.deposition.service.JWTService;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;
import uk.ac.ebi.spot.gwas.deposition.util.HeadersUtil;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_BODY_OF_WORK)
public class BodyOfWorkController {

    private static final Logger log = LoggerFactory.getLogger(PublicationsController.class);

    @Autowired
    private BodyOfWorkService bodyOfWorkService;

    @Autowired
    private BodyOfWorkDtoAssembler bodyOfWorkDtoAssembler;

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditProxy auditProxy;

    @Autowired
    private SubmissionService submissionService;

    /**
     * POST /v1/bodyofwork
     */
    @PostMapping(produces = "application/hal+json")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public Resource<BodyOfWorkDto> createBodyOfWork(@RequestBody BodyOfWorkDto bodyOfWorkDto, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to create body of work: {}", user.getId(), bodyOfWorkDto.getTitle());
        BodyOfWork bodyOfWork = BodyOfWorkDtoDisassembler.disassemble(bodyOfWorkDto, new Provenance(DateTime.now(), user.getId()));
        bodyOfWork = bodyOfWorkService.createBodyOfWork(bodyOfWork);
        auditProxy.addAuditEntry(AuditHelper.bowCreate(user.getId(), bodyOfWork));
        log.info("Body of work  created: {}", bodyOfWork.getId());
        return bodyOfWorkDtoAssembler.toResource(bodyOfWork);
    }

    /**
     * GET /v1/bodyofwork/{bodyofworkId}
     */
    @GetMapping(value = "/{bodyofworkId}", produces = "application/hal+json")
    @ResponseStatus(HttpStatus.OK)
    public Resource<BodyOfWorkDto> getBodyOfWork(@PathVariable String bodyofworkId,
                                                 HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to get body of work: {}", user.getId(), bodyofworkId);
        BodyOfWork bodyOfWork = bodyOfWorkService.retrieveBodyOfWork(bodyofworkId, user);
        auditProxy.addAuditEntry(AuditHelper.bowRetrieve(user.getId(), bodyOfWork));
        log.info("Returning body of work: {}", bodyOfWork.getId());
        return bodyOfWorkDtoAssembler.toResource(bodyOfWork);
    }

    /**
     * GET /v1/bodyofwork
     */
    @GetMapping(produces = "application/hal+json")
    @ResponseStatus(HttpStatus.OK)
    public PagedResources<BodyOfWorkDto> getBodyOfWorks(@PageableDefault(size = 20, page = 0) Pageable pageable,
                                                        @RequestParam(value = GWASDepositionBackendConstants.PARAM_STATUS,
                                                                required = false)
                                                                String status,
                                                        PagedResourcesAssembler assembler,
                                                        HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve body of works.", user.getId());
        Page<BodyOfWork> facetedBodyOfWorks = bodyOfWorkService.retrieveBodyOfWorks(user, status, pageable);
        auditProxy.addAuditEntry(AuditHelper.bowRetrieve(user.getId(), null));

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(BodyOfWorkController.class).getBodyOfWorks(pageable, status, assembler, null));

        return assembler.toResource(facetedBodyOfWorks, bodyOfWorkDtoAssembler,
                new Link(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).toUri().toString()));

    }

    /**
     * PUT /v1/bodyofwork/{bodyofworkId}
     */
    @PutMapping(value = "/{bodyofworkId}", produces = "application/hal+json")
    @ResponseStatus(HttpStatus.OK)
    public Resource<BodyOfWorkDto> updateBodyOfWork(@PathVariable String bodyofworkId,
                                                    @RequestBody BodyOfWorkDto bodyOfWorkDto,
                                                    HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to update body of work: {}", user.getId(), bodyofworkId);
        BodyOfWork bodyOfWork = BodyOfWorkDtoDisassembler.disassemble(bodyOfWorkDto, new Provenance(DateTime.now(), user.getId()));
        BodyOfWork updated = bodyOfWorkService.updateBodyOfWork(bodyofworkId, bodyOfWork, user);
        auditProxy.addAuditEntry(AuditHelper.bowUpdate(user.getId(), updated));
        log.info("Returning body of work: {}", updated.getId());
        return bodyOfWorkDtoAssembler.toResource(updated);
    }

    /**
     * DELETE /v1/bodyofwork/{bodyofworkId}
     */
    @DeleteMapping(value = "/{bodyofworkId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteBodyOfWork(@PathVariable String bodyofworkId,
                                 HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to delete body of work: {}", user.getId(), bodyofworkId);
        BodyOfWork bodyOfWork = bodyOfWorkService.retrieveBodyOfWork(bodyofworkId, user);
        Submission submission = submissionService.findByBodyOfWork(bodyofworkId, user.getId());
        if (submission != null) {
            auditProxy.addAuditEntry(AuditHelper.bowDelete(user.getId(), bodyOfWork, false));
            log.error("Unable to delete body of work {} because a submission already exists on it: {}", bodyofworkId, submission.getId());
            throw new CannotDeleteBodyOfWorkException("Unable to delete body of work: " + bodyofworkId + ". A submission was created using this artefact.");
        }
        bodyOfWorkService.deleteBodyOfWork(bodyofworkId, user);
        auditProxy.addAuditEntry(AuditHelper.bowDelete(user.getId(), bodyOfWork, true));
    }
}
