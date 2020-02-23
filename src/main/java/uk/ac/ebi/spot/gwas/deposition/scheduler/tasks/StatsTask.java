package uk.ac.ebi.spot.gwas.deposition.scheduler.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.domain.DailyStats;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.repository.DailyStatsRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SubmissionRepository;
import uk.ac.ebi.spot.gwas.deposition.util.StatsBuilder;
import uk.ac.ebi.spot.gwas.deposition.util.StatsConstants;

import java.util.stream.Stream;

@Component
public class StatsTask {

    private static final Logger log = LoggerFactory.getLogger(StatsTask.class);

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private DailyStatsRepository dailyStatsRepository;

    public void buildStats() {
        log.info("Building stats ...");
        DailyStats dailyStats = new DailyStats();
        Long totalCount = submissionRepository.countByArchived(false);
        dailyStats.addStats(StatsConstants.TOTAL_SUBMISSIONS, totalCount.intValue());

        Long totalSubmitted = submissionRepository.countByOverallStatus(Status.SUBMITTED.name());
        dailyStats.addStats(StatsConstants.TOTAL_SUBMITTED, totalSubmitted.intValue());

        StatsBuilder statsBuilder = new StatsBuilder(dailyStats);
        Stream<Submission> submissionStream = submissionRepository.readByArchived(false);
        submissionStream.forEach(submission -> statsBuilder.processSubmission(submission));
        submissionStream.close();
        dailyStats = statsBuilder.getDailyStats();
        dailyStats = dailyStatsRepository.insert(dailyStats);
        log.info("Stats ready: {}", dailyStats.getId());
    }
}
