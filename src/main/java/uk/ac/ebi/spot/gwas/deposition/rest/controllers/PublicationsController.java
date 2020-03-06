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
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.dto.PublicationDto;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.PublicationDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.PublicationService;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;

@RestController
@RequestMapping(value = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_PUBLICATIONS)
public class PublicationsController {

    private static final Logger log = LoggerFactory.getLogger(PublicationsController.class);

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private PublicationDtoAssembler publicationDtoAssembler;

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    /**
     * GET /v1/publications/{publicationId}?pmid=true | false
     */
    @GetMapping(value = "/{publicationId}", produces = "application/hal+json")
    @ResponseStatus(HttpStatus.OK)
    public Resource<PublicationDto> getPublication(@PathVariable String publicationId,
                                                   @RequestParam(value = GWASDepositionBackendConstants.PARAM_PMID,
                                                           required = false)
                                                           Boolean pmid) {
        log.info("Request to get publication: {} - {}.", publicationId, pmid);
        Publication publication;
        if (pmid != null) {
            publication = publicationService.retrievePublication(publicationId, !pmid.booleanValue());
        } else {
            publication = publicationService.retrievePublication(publicationId, true);
        }
        log.info("Returning publication: {}", publication.getPmid());
        return publicationDtoAssembler.toResource(publication);
    }

    /**
     * GET /v1/publications
     */
    @GetMapping(produces = "application/hal+json")
    @ResponseStatus(HttpStatus.OK)
    public PagedResources<PublicationDto> getPublications(
            @RequestParam(value = GWASDepositionBackendConstants.PARAM_AUTHOR,
                    required = false) String author,
            @RequestParam(value = GWASDepositionBackendConstants.PARAM_TITLE,
                    required = false) String title,
            @PageableDefault(size = 20, page = 0) Pageable pageable, PagedResourcesAssembler assembler) {
        log.info("Request to retrieve publications: {} - {}", author, title);
        Page<Publication> facetedSearchPublications = publicationService.getPublications(author, title, pageable);

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(PublicationsController.class).getPublications(author, title, pageable, assembler));

        return assembler.toResource(facetedSearchPublications, publicationDtoAssembler,
                new Link(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).toUri().toString()));

    }
}
