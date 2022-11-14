package uk.ac.ebi.spot.gwas.deposition.scheduler.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.SystemConfigProperties;
import uk.ac.ebi.spot.gwas.deposition.constants.PublicationIngestStatus;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.domain.PublicationIngestEntry;
import uk.ac.ebi.spot.gwas.deposition.repository.PublicationIngestEntryRepository;
import uk.ac.ebi.spot.gwas.deposition.service.PublicationService;
import uk.ac.ebi.spot.gwas.deposition.service.SOLRService;

import java.util.ArrayList;
import java.util.List;

@Component
public class SOLRDocumentsStatusCheckTask {

    private static final Logger log = LoggerFactory.getLogger(SOLRDocumentsStatusCheckTask.class);

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private SystemConfigProperties systemConfigProperties;

    @Autowired
    private PublicationIngestEntryRepository publicationIngestEntryRepository;

    //@Autowired(required = false)
    @Autowired
    private SOLRService solrService;

    public void checkSOLRDocuments() {
        if (solrService == null) {
            return;
        }
        log.info("Checking SOLR documents ...");
        List<PublicationIngestEntry> publicationIngestEntries = publicationIngestEntryRepository.findByEnvironment(systemConfigProperties.getActiveSpringProfile());
        List<String> idsToDelete = new ArrayList<>();
        log.info("Found {} SOLR documents to index.", publicationIngestEntries.size());
        for (PublicationIngestEntry publicationIngestEntry : publicationIngestEntries) {
            try {
                Publication publication = publicationService.retrievePublication(publicationIngestEntry.getPublicationId(), true);
                if (publicationIngestEntry.getStatus().equalsIgnoreCase(PublicationIngestStatus.CREATED.name())) {
                    if (solrService != null) {
                        solrService.addPublication(publication);
                    }
                }
                if (publicationIngestEntry.getStatus().equalsIgnoreCase(PublicationIngestStatus.UPDATED.name())) {
                    if (solrService != null) {
                        solrService.updatePublication(publication);
                    }
                }
                idsToDelete.add(publicationIngestEntry.getId());
            } catch (Exception e) {
                log.error("ERROR: {}", e.getMessage(), e);
            }
        }

        for (String id : idsToDelete) {
            publicationIngestEntryRepository.deleteById(id);
        }
        log.info("SOLR documents processed.");
    }
}
