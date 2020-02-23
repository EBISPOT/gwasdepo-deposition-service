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
@Profile(GWASDepositionBackendConstants.PROFILE_FALLBACK)
public class SOLRDocumentStatusCheckConfigFallback extends AbstractQuartzConfig {

    private static final String JK_SOLR_FB = "JK_SOLR_FB";

    private static final String PG_SOLR_FB = "PG_SOLR_FB";

    private static final String TK_SOLR_FB = "TK_SOLR_FB";

    @Value("${quartz.jobs.solr-check.schedule}")
    private String solrCheckSchedule;

    public SOLRDocumentStatusCheckConfigFallback() {
        super(TK_SOLR_FB, PG_SOLR_FB);
    }

    public JobDetail getjobDetail() {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(new JobKey(JK_SOLR_FB, PG_SOLR_FB));
        jobDetail.setJobClass(SOLRDocumentsStatusCheckJob.class);
        jobDetail.setDurability(true);
        return jobDetail;
    }

    public Trigger createNewTrigger(Date startTime) {
        return TriggerBuilder.newTrigger()
                .forJob(this.getjobDetail())
                .withIdentity(TK_SOLR_FB, PG_SOLR_FB)
                .withPriority(50)
                .withSchedule(CronScheduleBuilder.cronSchedule(solrCheckSchedule))
                .startAt(startTime).build();
    }

}
