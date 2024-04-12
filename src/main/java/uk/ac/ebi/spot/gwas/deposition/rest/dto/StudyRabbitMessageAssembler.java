package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.dto.curation.StudyRabbitMessage;
import uk.ac.ebi.spot.gwas.deposition.repository.StudyRepository;

import java.util.Optional;

@Component
public class StudyRabbitMessageAssembler {

    @Autowired
    StudyRepository studyRepository;

    public StudyRabbitMessage assemble(Study study) {
     return  StudyRabbitMessage.builder()
                .studyId(study.getId())
                .accession(study.getAccession())
                .arrayInformation(study.getArrayInformation())
                .arrayManufacturer(study.getArrayManufacturer())
                .adjustedCovariates(study.getAdjustedCovariates())
                .agreedToCc0(study.isAgreedToCc0())
                .analysisSoftware(study.getAnalysisSoftware())
                .checksum(study.getChecksum())
                .cohort(study.getCohort())
                .cohortId(study.getCohortId())
                .coordinateSystem(study.getCoordinateSystem())
                .genotypingTechnology(study.getGenotypingTechnology())
                .gxeFlag(study.getGxeFlag())
                .imputation(study.getImputation())
                .imputationPanel(study.getImputationPanel())
                .imputationSoftware(study.getImputationSoftware())
                .initialSampleDescription(study.getInitialSampleDescription())
                .minor_allele_frequency_lower_limit(study.getEffect_allele_frequency_lower_limit())
                .pooledFlag(study.getPooledFlag())
                .rawSumstatsFile(study.getRawFilePath())
                .readmeFile(study.getReadmeFile())
                .replicateSampleDescription(study.getReplicateSampleDescription())
                .sampleDescription(study.getSampleDescription())
                .sex(study.getSex())
                .statisticalModel(study.getStatisticalModel())
                .studyDescription(study.getStudyDescription())
                .studyTag(study.getStudyTag())
                .submissionId(study.getSubmissionId())
                .summaryStatisticsAssembly(study.getSummaryStatisticsAssembly())
                .summaryStatisticsFile(study.getSummaryStatisticsFile())
                .sumstatsFlag(study.getSumstatsFlag())
                .variantCount(study.getVariantCount())
                .build();


    }

    public Study disassemble(StudyRabbitMessage studyRabbitMessage) {
       Optional<Study> optionalStudy = studyRepository.findById(studyRabbitMessage.getStudyId());
       if(optionalStudy.isPresent()) {
           return optionalStudy.get();
       } else {
           return null;
       }
    }

}
