package uk.ac.ebi.spot.gwas.deposition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestInteractionConfig {

    @Value("${gwas-sumstats-service.url}")
    private String sumStatsServiceUrl;

    @Value("${gwas-sumstats-service.endpoints.sum-stats}")
    private String sumStatsEndpoint;

    @Value("${gwas-sumstats-service.endpoints.globus-mkdir}")
    private String globusMkdirEndpoint;

    @Value("${gwas-sumstats-service.endpoints.globus}")
    private String globusEndpoint;

    @Value("${gwas-template-service.url}")
    private String templateServiceUrl;

    @Value("${gwas-template-service.endpoints.template-schema}")
    private String templateSchemaEndpoint;

    @Value("${gwas-template-service.endpoints.prefilled-template}")
    private String prefilledTemplateEndpoint;

    @Value("${gwas-catalog-service.endpoint}")
    private String gwasCatalogServiceEndpoint;

    public String getSumStatsEndpoint() {
        return sumStatsServiceUrl + sumStatsEndpoint;
    }

    public String getGlobusEndpoint() {
        return sumStatsServiceUrl + globusEndpoint;
    }

    public String getGlobusMkdirEndpoint() {
        return sumStatsServiceUrl + globusMkdirEndpoint;
    }

    public String getTemplateSchemaEndpoint() {
        return templateServiceUrl + templateSchemaEndpoint;
    }

    public String getPrefilledTemplateEndpoint() {
        return templateServiceUrl + prefilledTemplateEndpoint;
    }

    public String getGwasCatalogServiceEndpoint() {
        return gwasCatalogServiceEndpoint;
    }
}
