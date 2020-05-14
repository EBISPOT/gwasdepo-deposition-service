package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.gcst.SSTemplateGCSTStudyDto;

public class SSTemplateGCSTStudyDtoAssembler {

    public static SSTemplateGCSTStudyDto assemble(Study study) {
        return new SSTemplateGCSTStudyDto(study.getAccession(),
                study.getStudyTag(),
                study.getTrait(),
                study.getEfoTrait(),
                study.getBackgroundTrait(),
                study.getBackgroundEfoTrait());
    }

}
