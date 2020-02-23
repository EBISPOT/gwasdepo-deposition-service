package uk.ac.ebi.spot.gwas.deposition.scheduler.config;

import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.scheduler.jobs.SOLRDocumentsStatusCheckJob;

import java.util.Date;

@Configuration
@ConditionalOnProperty(name = "comms.messaging.enabled", havingValue = "true")
@Profile(GWASDepositionBackendConstants.PROFILE_SANDBOX)
public class SOLRDocumentStatusCheckConfigSandbox extends AbstractQuartzConfig {

    private static final String JK_SOLR_SANDBOX = "JK_SOLR_SANDBOX";

    private static final String PG_SOLR_SANDBOX = "PG_SOLR_SANDBOX";

    private static final String TK_SOLR_SANDBOX = "TK_SOLR_SANDBOX";

    @Value("${quartz.jobs.solr-check.schedule}")
    private String solrCheckSchedule;

    public SOLRDocumentStatusCheckConfigSandbox() {
        super(TK_SOLR_SANDBOX, PG_SOLR_SANDBOX);
    }

    public JobDetail getjobDetail() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(new JobKey(JK_SOLR_SANDBOX, PG_SOLR_SANDBOX));
        jobDetail.setJobClass(SOLRDocumentsStatusCheckJob.class);
        jobDetail.setDurability(true);
        return jobDetail;
    }

    public Trigger createNewTrigger(Date startTime) {
        return TriggerBuilder.newTrigger()
                .forJob(this.getjobDetail())
                .withIdentity(TK_SOLR_SANDBOX, PG_SOLR_SANDBOX)
                .withPriority(50)
                .withSchedule(CronScheduleBuilder.cronSchedule(solrCheckSchedule))
                .startAt(startTime).build();
    }

}
