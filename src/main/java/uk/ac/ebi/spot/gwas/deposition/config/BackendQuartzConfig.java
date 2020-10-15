package uk.ac.ebi.spot.gwas.deposition.config;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.spot.gwas.deposition.scheduler.config.*;

import javax.annotation.PostConstruct;

@Configuration
public class BackendQuartzConfig {

    @Autowired
    private QuartzSchedulerJobConfig quartzSchedulerJobConfig;

    @Autowired(required = false)
    private StatsConfig statsConfig;

    @Autowired(required = false)
    private SSCallbackConfig ssCallbackConfig;

    @Autowired(required = false)
    private SubmissionsCleanUpConfig submissionsCleanUpConfig;

    @Autowired(required = false)
    private SOLRDocumentStatusCheckConfigProd solrDocumentStatusCheckConfigProd;

    @Autowired(required = false)
    private SOLRDocumentStatusCheckConfigFallback solrDocumentStatusCheckConfigFallback;

    @Autowired(required = false)
    private SOLRDocumentStatusCheckConfigSandbox solrDocumentStatusCheckConfigSandbox;

    @Autowired(required = false)
    private StalledSubmissionsCheckConfig stalledSubmissionsCheckConfig;

    @PostConstruct
    private void initialize() throws SchedulerException {
        if (statsConfig != null) {
            quartzSchedulerJobConfig.addJob(statsConfig);
        }
        if (ssCallbackConfig != null) {
            quartzSchedulerJobConfig.addJob(ssCallbackConfig);
        }
        if (submissionsCleanUpConfig != null) {
            quartzSchedulerJobConfig.addJob(submissionsCleanUpConfig);
        }
        if (solrDocumentStatusCheckConfigProd != null) {
            quartzSchedulerJobConfig.addJob(solrDocumentStatusCheckConfigProd);
        }
        if (solrDocumentStatusCheckConfigFallback != null) {
            quartzSchedulerJobConfig.addJob(solrDocumentStatusCheckConfigFallback);
        }
        if (solrDocumentStatusCheckConfigSandbox != null) {
            quartzSchedulerJobConfig.addJob(solrDocumentStatusCheckConfigSandbox);
        }
        if (stalledSubmissionsCheckConfig != null) {
            quartzSchedulerJobConfig.addJob(stalledSubmissionsCheckConfig);
        }
        quartzSchedulerJobConfig.initializeJobs();
    }
}
