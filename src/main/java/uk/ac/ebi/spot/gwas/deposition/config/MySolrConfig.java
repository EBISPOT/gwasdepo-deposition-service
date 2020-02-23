package uk.ac.ebi.spot.gwas.deposition.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

@Configuration
@ConditionalOnProperty(name = "gwas-deposition.solr.enabled", havingValue = "true")
@EnableSolrRepositories(value = "uk.ac.ebi.spot.gwas.deposition.solr", schemaCreationSupport = true)
@ComponentScan
public class MySolrConfig {

    private static final Logger log = LoggerFactory.getLogger(MySolrConfig.class);

    @Value("${spring.data.solr.host:#{NULL}}")
    private String solrURL;

    @Bean
    public SolrClient solrClient() {
        log.info("Using SOLR: {}", solrURL);
        SolrClient solrClient = new HttpSolrClient.Builder(solrURL).build();
        log.info("Initialized SOLR client: {}", solrClient);
        return solrClient;
    }

    @Bean
    public SolrTemplate solrTemplate(SolrClient client) {
        return new SolrTemplate(client);
    }
}
