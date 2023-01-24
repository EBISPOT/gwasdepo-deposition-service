package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.dto.CorrespondingAuthorDto;
import uk.ac.ebi.spot.gwas.deposition.dto.PublicationDto;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.PublicationsController;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;

@Component
public class PublicationDtoAssembler implements ResourceAssembler<Publication, Resource<PublicationDto>> {

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    public static PublicationDto assemble(Publication publication) {
        return new PublicationDto(publication.getId(),
                publication.getPmid(),
                publication.getTitle(),
                publication.getJournal(),
                publication.getFirstAuthor(),
                publication.getPublicationDate(),
                publication.getCorrespondingAuthor() != null ?
                        new CorrespondingAuthorDto(publication.getCorrespondingAuthor().getAuthorName(),
                                publication.getCorrespondingAuthor().getEmail()) : null,
                publication.getStatus(),
                null,
                null,
                null);
    }

    public Resource<PublicationDto> toResource(Publication publication) {

        PublicationDto publicationDto = new PublicationDto(publication.getId(),
                publication.getPmid(),
                publication.getTitle(),
                publication.getJournal(),
                publication.getFirstAuthor(),
                publication.getPublicationDate(),
                publication.getCorrespondingAuthor() != null ?
                        new CorrespondingAuthorDto(publication.getCorrespondingAuthor().getAuthorName(),
                                publication.getCorrespondingAuthor().getEmail()) : null,
                publication.getStatus(),
                null,
                null,
                null);


        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(PublicationsController.class).getPublication(publication.getId(), false));

        Resource<PublicationDto> resource = new Resource<>(publicationDto);
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withSelfRel());
        return resource;
    }
}
