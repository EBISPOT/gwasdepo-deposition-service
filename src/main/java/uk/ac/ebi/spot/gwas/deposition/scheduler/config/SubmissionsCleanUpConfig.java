package uk.ac.ebi.spot.gwas.deposition.scheduler.config;

import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.spot.gwas.deposition.scheduler.jobs.SubmissionsCleanUpJob;

import java.util.Date;

@Configuration
@ConditionalOnProperty(name = "gwas-deposition.clean-up-task.enabled", havingValue = "true")
public class SubmissionsCleanUpConfig extends AbstractQuartzConfig {

    private static final String JK_CLEANUP = "JK_CLEANUP";

    private static final String PG_CLEANUP = "PG_CLEANUP";

    private static final String TK_CLEANUP = "TK_CLEANUP";

    public SubmissionsCleanUpConfig() {
        super(TK_CLEANUP, PG_CLEANUP);
    }

    @Value("${quartz.jobs.clean-up.schedule}")
    private String cleanUpSchedule;

    public JobDetail getjobDetail() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(new JobKey(JK_CLEANUP, PG_CLEANUP));
        jobDetail.setJobClass(SubmissionsCleanUpJob.class);
        jobDetail.setDurability(true);
        return jobDetail;
    }

    public Trigger createNewTrigger(Date startTime) {
        return TriggerBuilder.newTrigger()
                .forJob(this.getjobDetail())
                .withIdentity(TK_CLEANUP, PG_CLEANUP)
                .withPriority(50)
                .withSchedule(CronScheduleBuilder.cronSchedule(cleanUpSchedule))
                .startAt(startTime).build();
    }
}
