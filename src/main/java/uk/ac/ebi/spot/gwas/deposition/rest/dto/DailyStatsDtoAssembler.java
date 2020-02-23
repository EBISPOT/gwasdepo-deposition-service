package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.domain.DailyStats;
import uk.ac.ebi.spot.gwas.deposition.dto.DailyStatsDto;

public class DailyStatsDtoAssembler {

    public static DailyStatsDto assemble(DailyStats dailyStats) {
        return new DailyStatsDto(dailyStats.getStats(),
                dailyStats.getNewIncompleteSubmissions(),
                dailyStats.getNewSubmitted(),
                dailyStats.getDate());
    }

}
