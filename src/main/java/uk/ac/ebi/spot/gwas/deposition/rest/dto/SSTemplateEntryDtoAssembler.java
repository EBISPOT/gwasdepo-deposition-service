package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.domain.SSTemplateEntry;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateEntryDto;

import java.util.List;
import java.util.stream.Collectors;

public class SSTemplateEntryDtoAssembler {

    public static SSTemplateEntryDto assemble(SSTemplateEntry ssTemplateEntry) {
        return new SSTemplateEntryDto(ssTemplateEntry.getStudyAccession(),
                ssTemplateEntry.getStudyTag(),
                ssTemplateEntry.getTrait(),
                ssTemplateEntry.getSampleDescription(),
                ssTemplateEntry.getHasSummaryStats());
    }

    public static List<SSTemplateEntryDto> assembleList(List<SSTemplateEntry> ssTemplateEntries) {
        if (ssTemplateEntries == null) {
            return null;
        }

        return ssTemplateEntries.stream().map(SSTemplateEntryDtoAssembler::assemble).collect(Collectors.toList());
    }
}
