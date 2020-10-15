package uk.ac.ebi.spot.gwas.deposition.scheduler.config;

import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.spot.gwas.deposition.scheduler.jobs.StalledSubmissionsCheckJob;

import java.util.Date;

@Configuration
public class StalledSubmissionsCheckConfig extends AbstractQuartzConfig {

    private static final String JK_STALLEDSUBMISSIONS = "JK_STALLEDSUBMISSIONS";

    private static final String PG_STALLEDSUBMISSIONS = "PG_STALLEDSUBMISSIONS";

    private static final String TK_STALLEDSUBMISSIONS = "TK_STALLEDSUBMISSIONS";

    public StalledSubmissionsCheckConfig() {
        super(TK_STALLEDSUBMISSIONS, PG_STALLEDSUBMISSIONS);
    }

    @Value("${quartz.jobs.stalled-submissions.schedule}")
    private String stalledSubmissionsSchedule;

    public JobDetail getjobDetail() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(new JobKey(JK_STALLEDSUBMISSIONS, PG_STALLEDSUBMISSIONS));
        jobDetail.setJobClass(StalledSubmissionsCheckJob.class);
        jobDetail.setDurability(true);
        return jobDetail;
    }

    public Trigger createNewTrigger(Date startTime) {
        return TriggerBuilder.newTrigger()
                .forJob(this.getjobDetail())
                .withIdentity(TK_STALLEDSUBMISSIONS, PG_STALLEDSUBMISSIONS)
                .withPriority(50)
                .withSchedule(CronScheduleBuilder.cronSchedule(stalledSubmissionsSchedule))
                .startAt(startTime).build();
    }
}
