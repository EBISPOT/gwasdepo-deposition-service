package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.domain.Manuscript;
import uk.ac.ebi.spot.gwas.deposition.dto.CorrespondingAuthorDto;
import uk.ac.ebi.spot.gwas.deposition.dto.ManuscriptDto;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.ManuscriptController;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;

@Component
public class ManuscriptDtoAssembler implements ResourceAssembler<Manuscript, Resource<ManuscriptDto>> {

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    public static ManuscriptDto assemble(Manuscript manuscript) {
        return new ManuscriptDto(manuscript.getId(),
                manuscript.getTitle(),
                manuscript.getJournal(),
                manuscript.getFirstAuthor(),
                manuscript.getSubmissionDate(),
                manuscript.getAcceptanceDate(),
                manuscript.getCorrespondingAuthor() != null ?
                        new CorrespondingAuthorDto(manuscript.getCorrespondingAuthor().getAuthorName(),
                                manuscript.getCorrespondingAuthor().getEmail()) : null,
                manuscript.getStatus());
    }

    @Override
    public Resource<ManuscriptDto> toResource(Manuscript manuscript) {
        ManuscriptDto manuscriptDto = new ManuscriptDto(manuscript.getId(),
                manuscript.getTitle(),
                manuscript.getJournal(),
                manuscript.getFirstAuthor(),
                manuscript.getSubmissionDate(),
                manuscript.getAcceptanceDate(),
                manuscript.getCorrespondingAuthor() != null ?
                        new CorrespondingAuthorDto(manuscript.getCorrespondingAuthor().getAuthorName(),
                                manuscript.getCorrespondingAuthor().getEmail()) : null,
                manuscript.getStatus());

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(ManuscriptController.class).getManuscript(manuscript.getId(), null));

        Resource<ManuscriptDto> resource = new Resource<>(manuscriptDto);
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withSelfRel());
        return resource;
    }
}
