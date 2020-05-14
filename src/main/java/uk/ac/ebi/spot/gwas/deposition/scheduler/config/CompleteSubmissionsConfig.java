package uk.ac.ebi.spot.gwas.deposition.scheduler.config;

import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.spot.gwas.deposition.scheduler.jobs.CompleteSubmissionsJob;

import java.util.Date;

@Configuration
@ConditionalOnProperty(name = "gwas-sumstats-service.callback-schedule.enabled", havingValue = "true")
public class CompleteSubmissionsConfig extends AbstractQuartzConfig {

    private static final String JK_COMPLETESUB = "JK_COMPLETESUB";

    private static final String PG_COMPLETESUB = "PG_COMPLETESUB";

    private static final String TK_COMPLETESUB = "TK_COMPLETESUB";

    @Value("${quartz.jobs.complete-submissions.schedule}")
    private String completeSubmissionsSchedule;

    public CompleteSubmissionsConfig() {
        super(TK_COMPLETESUB, PG_COMPLETESUB);
    }

    public JobDetail getjobDetail() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(new JobKey(JK_COMPLETESUB, PG_COMPLETESUB));
        jobDetail.setJobClass(CompleteSubmissionsJob.class);
        jobDetail.setDurability(true);
        return jobDetail;
    }

    public Trigger createNewTrigger(Date startTime) {
        return TriggerBuilder.newTrigger()
                .forJob(this.getjobDetail())
                .withIdentity(TK_COMPLETESUB, PG_COMPLETESUB)
                .withPriority(50)
                .withSchedule(CronScheduleBuilder.cronSchedule(completeSubmissionsSchedule))
                .startAt(startTime).build();
    }
}
