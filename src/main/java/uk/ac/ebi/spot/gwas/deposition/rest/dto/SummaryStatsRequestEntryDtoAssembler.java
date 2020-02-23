package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.domain.SummaryStatsEntry;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsRequestEntryDto;

public class SummaryStatsRequestEntryDtoAssembler {

    public static SummaryStatsRequestEntryDto assemble(SummaryStatsEntry summaryStatsEntry) {
        return new SummaryStatsRequestEntryDto(summaryStatsEntry.getId(),
                summaryStatsEntry.getFilePath(),
                summaryStatsEntry.getMd5(),
                summaryStatsEntry.getAssembly(),
                summaryStatsEntry.getReadme(),
                summaryStatsEntry.getGlobusFolder());
    }
}
