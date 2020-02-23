package uk.ac.ebi.spot.gwas.deposition.util;

import org.joda.time.DateTime;
import org.joda.time.Days;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.domain.DailyStats;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;

public class StatsBuilder {

    private DateTime today;

    private int total;

    private int submitted;

    private int withNoFile;

    private int withInvalidFile;

    private int withValidFile;

    private DailyStats dailyStats;

    public StatsBuilder(DailyStats dailyStats) {
        this.dailyStats = dailyStats;
        today = DateTime.now();
        total = 0;
        submitted = 0;
        withNoFile = 0;
        withInvalidFile = 0;
        withValidFile = 0;
    }

    public void processSubmission(Submission submission) {
        DateTime timestamp = submission.getCreated().getTimestamp();
        int days = Days.daysBetween(timestamp.toLocalDate(), today.toLocalDate()).getDays();

        if (days == 1) {
            total++;
            if (submission.getOverallStatus().equals(Status.SUBMITTED.name())) {
                submitted++;
                dailyStats.addNewSubmitted(submission.getId());
            } else {
                if (submission.getFileUploads().isEmpty()) {
                    withNoFile++;
                } else {
                    dailyStats.addNewIncompleteSubmissions(submission.getId());
                    if (submission.getMetadataStatus().equals(Status.INVALID) ||
                            submission.getSummaryStatsStatus().equals(Status.INVALID)) {
                        withInvalidFile++;
                    } else {
                        withValidFile++;
                    }
                }
            }
        }
    }

    public DailyStats getDailyStats() {
        dailyStats.addStats(StatsConstants.NEW_SUBMISSIONS, total);
        dailyStats.addStats(StatsConstants.NEW_SUBMITTED, submitted);
        dailyStats.addStats(StatsConstants.NEW_WITH_INVALID_FILE, withInvalidFile);
        dailyStats.addStats(StatsConstants.NEW_WITH_NO_FILE, withNoFile);
        dailyStats.addStats(StatsConstants.NEW_WITH_VALID_FILE, withValidFile);

        return dailyStats;
    }
}
