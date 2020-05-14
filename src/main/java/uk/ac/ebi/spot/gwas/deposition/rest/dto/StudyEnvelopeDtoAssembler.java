package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.dto.StudyEnvelopeDto;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.SubmissionsController;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;


@Component
public class StudyEnvelopeDtoAssembler implements ResourceAssembler<Study, Resource<StudyEnvelopeDto>> {

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    public Resource<StudyEnvelopeDto> toResource(Study study) {
        StudyEnvelopeDto studyEnvelopeDto = new StudyEnvelopeDto(study.getAccession());

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(SubmissionsController.class).getSubmission(study.getSubmissionId(), null));

        Resource<StudyEnvelopeDto> resource = new Resource<>(studyEnvelopeDto);
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withRel(GWASDepositionBackendConstants.LINKS_PARENT));
        return resource;
    }
}
