package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.dto.gwas.GWASCatalogStudyDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateEntryDto;

public class SSTemplateEntryRequestDtoAssembler {

    public static SSTemplateEntryDto assemble(GWASCatalogStudyDto gwasCatalogStudyDto) {
        if (gwasCatalogStudyDto == null) {
            return null;
        }
        if (gwasCatalogStudyDto.getDiseaseTrait() == null) {
            return null;
        }
        return new SSTemplateEntryDto(gwasCatalogStudyDto.getAccessionId(),
                null,
                gwasCatalogStudyDto.getDiseaseTrait().getTrait(),
                gwasCatalogStudyDto.getInitialSampleSize(),
                false);
    }

}
