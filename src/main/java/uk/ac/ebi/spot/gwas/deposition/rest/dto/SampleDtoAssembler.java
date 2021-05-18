package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.Sample;
import uk.ac.ebi.spot.gwas.deposition.dto.SampleDto;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.SubmissionsController;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;

@Component
public class SampleDtoAssembler implements ResourceAssembler<Sample, Resource<SampleDto>> {

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    public Resource<SampleDto> toResource(Sample sample) {
        SampleDto sampleDto = new SampleDto(sample.getStudyTag(),
                sample.getStage(),
                sample.getSize(),
                sample.getCases(),
                sample.getControls(),
                sample.getSampleDescription(),
                sample.getAncestryCategory(),
                sample.getAncestry(),
                sample.getAncestryDescription(),
                sample.getCountryRecruitement());

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(SubmissionsController.class).getSubmission(sample.getSubmissionId(), null));

        Resource<SampleDto> resource = new Resource<>(sampleDto);
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withRel(GWASDepositionBackendConstants.LINKS_PARENT));
        return resource;
    }

    public static SampleDto assemble(Sample sample) {
        return new SampleDto(sample.getStudyTag(),
                sample.getStage(),
                sample.getSize(),
                sample.getCases(),
                sample.getControls(),
                sample.getSampleDescription(),
                sample.getAncestryCategory(),
                sample.getAncestry(),
                sample.getAncestryDescription(),
                sample.getCountryRecruitement());
    }

    public static Sample disassemble(SampleDto sampleDto) {
        Sample sample = new Sample();
        sample.setStudyTag(sampleDto.getStudyTag());
        sample.setStage(sampleDto.getStage());
        sample.setSize(sampleDto.getSize());
        sample.setCases(sampleDto.getCases());
        sample.setControls(sampleDto.getControls());
        sample.setSampleDescription(sampleDto.getSampleDescription());
        sample.setAncestryCategory(sampleDto.getAncestryCategory());
        sample.setAncestry(sampleDto.getAncestry());
        sample.setAncestryDescription(sampleDto.getAncestryDescription());
        sample.setCountryRecruitement(sampleDto.getCountryRecruitement());
        return sample;
    }
}
