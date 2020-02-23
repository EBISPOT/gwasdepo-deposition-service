package uk.ac.ebi.spot.gwas.deposition.scheduler.config;

import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.spot.gwas.deposition.scheduler.jobs.StatsJob;

import java.util.Date;

@Configuration
@ConditionalOnProperty(name = "gwas-deposition.stats-task.enabled", havingValue = "true")
public class StatsConfig extends AbstractQuartzConfig {

    private static final String JK_STATS = "JK_STATS";

    private static final String PG_STATS = "PG_STATS";

    private static final String TK_STATS = "TK_STATS";

    @Value("${quartz.jobs.stats.schedule}")
    private String statsSchedule;

    public StatsConfig() {
        super(TK_STATS, PG_STATS);
    }

    public JobDetail getjobDetail() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(new JobKey(JK_STATS, PG_STATS));
        jobDetail.setJobClass(StatsJob.class);
        jobDetail.setDurability(true);
        return jobDetail;
    }

    public Trigger createNewTrigger(Date startTime) {
        return TriggerBuilder.newTrigger()
                .forJob(this.getjobDetail())
                .withIdentity(TK_STATS, PG_STATS)
                .withPriority(50)
                .withSchedule(CronScheduleBuilder.cronSchedule(statsSchedule))
                .startAt(startTime).build();
    }
}
