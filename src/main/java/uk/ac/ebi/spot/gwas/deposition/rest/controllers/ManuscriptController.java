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
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.Manuscript;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.ManuscriptDto;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.ManuscriptDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.JWTService;
import uk.ac.ebi.spot.gwas.deposition.service.ManuscriptService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;
import uk.ac.ebi.spot.gwas.deposition.util.HeadersUtil;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = GWASDepositionBackendConstants.API_V1 + GWASDepositionBackendConstants.API_MANUSCRIPTS)
public class ManuscriptController {

    private static final Logger log = LoggerFactory.getLogger(PublicationsController.class);

    @Autowired
    private ManuscriptService manuscriptService;

    @Autowired
    private ManuscriptDtoAssembler manuscriptDtoAssembler;

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserService userService;

    /**
     * GET /v1/manuscripts/{manuscriptId}
     */
    @GetMapping(value = "/{manuscriptId}", produces = "application/hal+json")
    @ResponseStatus(HttpStatus.OK)
    public Resource<ManuscriptDto> getManuscript(@PathVariable String manuscriptId,
                                                 HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to get manuscript: {}", user.getId(), manuscriptId);
        Manuscript manuscript = manuscriptService.retrieveManuscript(manuscriptId, user.getId());
        log.info("Returning manuscript: {}", manuscript.getId());
        return manuscriptDtoAssembler.toResource(manuscript);
    }

    /**
     * GET /v1/manuscripts
     */
    @GetMapping(produces = "application/hal+json")
    @ResponseStatus(HttpStatus.OK)
    public PagedResources<ManuscriptDto> getManuscripts(@PageableDefault(size = 20, page = 0) Pageable pageable,
                                                        PagedResourcesAssembler assembler,
                                                        HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to retrieve manuscripts.", user.getId());
        Page<Manuscript> facetedManuscripts = manuscriptService.retrieveManuscripts(user.getId(), pageable);

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(ManuscriptController.class).getManuscripts(pageable, assembler, null));

        return assembler.toResource(facetedManuscripts, manuscriptDtoAssembler,
                new Link(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).toUri().toString()));

    }

}
