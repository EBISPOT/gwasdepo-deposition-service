package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.DailyStats;

import java.util.List;

public interface StatsService {

    List<DailyStats> getDailyStats();
}
