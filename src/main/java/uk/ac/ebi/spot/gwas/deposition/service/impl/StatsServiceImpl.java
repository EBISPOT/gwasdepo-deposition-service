package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.DailyStats;
import uk.ac.ebi.spot.gwas.deposition.repository.DailyStatsRepository;
import uk.ac.ebi.spot.gwas.deposition.service.StatsService;

import java.util.List;

@Service
@ConditionalOnProperty(name = "gwas-deposition.stats-task.enabled", havingValue = "true")
public class StatsServiceImpl implements StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);

    @Autowired
    private DailyStatsRepository dailyStatsRepository;

    @Override
    public List<DailyStats> getDailyStats() {
        log.info("Retrieving stats.");
        List<DailyStats> dailyStats = dailyStatsRepository.findAllByOrderByDateDesc(PageRequest.of(0, 10));
        log.info("Retrieved latest {} stats.", dailyStats.size());
        return dailyStats;
    }

}
