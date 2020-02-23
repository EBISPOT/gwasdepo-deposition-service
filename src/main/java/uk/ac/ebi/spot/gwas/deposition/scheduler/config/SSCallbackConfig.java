package uk.ac.ebi.spot.gwas.deposition.scheduler.config;

import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.spot.gwas.deposition.scheduler.jobs.SSCallbackJob;

import java.util.Date;

@Configuration
@ConditionalOnProperty(name = "gwas-sumstats-service.callback-schedule.enabled", havingValue = "true")
public class SSCallbackConfig extends AbstractQuartzConfig {

    private static final String JK_SSCALLBACK = "JK_SSCALLBACK";

    private static final String PG_SSCALLBACK = "PG_SSCALLBACK";

    private static final String TK_SSCALLBACK = "TK_SSCALLBACK";

    @Value("${quartz.jobs.ss-callback.schedule}")
    private String ssCallbackSchedule;

    public SSCallbackConfig() {
        super(TK_SSCALLBACK, PG_SSCALLBACK);
    }

    public JobDetail getjobDetail() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(new JobKey(JK_SSCALLBACK, PG_SSCALLBACK));
        jobDetail.setJobClass(SSCallbackJob.class);
        jobDetail.setDurability(true);
        return jobDetail;
    }

    public Trigger createNewTrigger(Date startTime) {
        return TriggerBuilder.newTrigger()
                .forJob(this.getjobDetail())
                .withIdentity(TK_SSCALLBACK, PG_SSCALLBACK)
                .withPriority(50)
                .withSchedule(CronScheduleBuilder.cronSchedule(ssCallbackSchedule))
                .startAt(startTime).build();
    }
}
