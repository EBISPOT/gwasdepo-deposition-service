package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.dto.StudyDto;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.SubmissionsController;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;


@Component
public class StudyDtoAssembler implements ResourceAssembler<Study, Resource<StudyDto>> {

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    public Resource<StudyDto> toResource(Study study) {
        StudyDto studyDto = new StudyDto(study.getStudyTag(),
                study.getId(),
                study.getAccession(),
                study.getGenotypingTechnology(),
                study.getArrayManufacturer(),
                study.getArrayInformation(),
                study.getImputation(),
                study.getVariantCount(),
                study.getSampleDescription(),
                study.getStatisticalModel(),
                study.getStudyDescription(),
                study.getTrait(),
                study.getEfoTrait(),
                study.getBackgroundTrait(),
                study.getBackgroundEfoTrait(),
                study.getSummaryStatisticsFile(),
                study.getRawFilePath(),
                study.getChecksum(),
                study.getSummaryStatisticsAssembly(),
                study.getReadmeFile(),
                study.getCohort(),
                study.getCohortId(),
                null,
                null,
                null,
                study.isAgreedToCc0(),
                null,
                null,
                null,
                study.getInitialSampleDescription(),
                study.getReplicateSampleDescription(),
                study.getSumstatsFlag(),
                study.getPooledFlag(),
                study.getGxeFlag(),
                study.getSubmissionId(),
                study.getImputationPanel(),
                study.getImputationSoftware(),
                study.getAdjustedCovariates(),
                study.getNeg_log_p_value(),
                study.getEffect_allele_frequency_lower_limit());


        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(SubmissionsController.class).getSubmission(study.getSubmissionId(), null));

        Resource<StudyDto> resource = new Resource<>(studyDto);
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withRel(GWASDepositionBackendConstants.LINKS_PARENT));
        return resource;
    }

    public static StudyDto assemble(Study study) {
        return new StudyDto(study.getStudyTag(),
                study.getId(),
                study.getAccession(),
                study.getGenotypingTechnology(),
                study.getArrayManufacturer(),
                study.getArrayInformation(),
                study.getImputation(),
                study.getVariantCount(),
                study.getSampleDescription(),
                study.getStatisticalModel(),
                study.getStudyDescription(),
                study.getTrait(),
                study.getEfoTrait(),
                study.getBackgroundTrait(),
                study.getBackgroundEfoTrait(),
                study.getSummaryStatisticsFile(),
                study.getRawFilePath(),
                study.getChecksum(),
                study.getSummaryStatisticsAssembly(),
                study.getReadmeFile(),
                study.getCohort(),
                study.getCohortId(),
                null,
                null,
                null,
                study.isAgreedToCc0(),
                null,
                null,
                null,
                study.getInitialSampleDescription(),
                study.getReplicateSampleDescription(),
                study.getSumstatsFlag(),
                study.getPooledFlag(),
                study.getGxeFlag(),
                study.getSubmissionId(),
                study.getImputationPanel(),
                study.getImputationSoftware(),
                study.getAdjustedCovariates(),
                study.getNeg_log_p_value(),
                study.getEffect_allele_frequency_lower_limit());

    }

    public static Study disassemble(StudyDto studyDto) {
        Study study = new Study();
        study.setStudyTag(studyDto.getStudyTag());
        study.setAccession(studyDto.getAccession());
        study.setGenotypingTechnology(studyDto.getGenotypingTechnology());
        study.setArrayManufacturer(studyDto.getArrayManufacturer());
        study.setArrayInformation(studyDto.getArrayInformation());
        study.setImputation(studyDto.getImputation());
        study.setVariantCount(studyDto.getVariantCount());
        study.setStatisticalModel(studyDto.getStatisticalModel());
        study.setStudyDescription(studyDto.getStudyDescription());
        study.setTrait(studyDto.getTrait());
        study.setSampleDescription(studyDto.getSampleDescription());
        study.setEfoTrait(studyDto.getEfoTrait());
        study.setBackgroundEfoTrait(studyDto.getBackgroundEfoTrait());
        study.setBackgroundTrait(studyDto.getBackgroundTrait());
        study.setSummaryStatisticsAssembly(studyDto.getSummaryStatisticsAssembly());
        study.setSummaryStatisticsFile(studyDto.getSummaryStatisticsFile());
        study.setRawFilePath(studyDto.getRawSumstatsFile());
        study.setReadmeFile(studyDto.getReadmeFile());
        study.setChecksum(studyDto.getChecksum());
        study.setCohort(studyDto.getCohort());
        study.setCohortId(studyDto.getCohortId());
        study.setInitialSampleDescription(studyDto.getInitialSampleDescription());
        study.setReplicateSampleDescription(studyDto.getReplicateSampleDescription());
        study.setSumstatsFlag(studyDto.getSumstatsFlag());
        study.setPooledFlag(studyDto.getPooledFlag());
        study.setGxeFlag(studyDto.getGxeFlag());
        study.setImputationPanel(studyDto.getImputationPanel());
        study.setImputationSoftware(studyDto.getImputationSoftware());
        study.setAdjustedCovariates(studyDto.getAdjustedCovariates());
        study.setNeg_log_p_value(studyDto.getNeg_log_p_value());
        study.setEffect_allele_frequency_lower_limit(studyDto.getEffect_allele_frequency_lower_limit());
        return study;
    }
}
