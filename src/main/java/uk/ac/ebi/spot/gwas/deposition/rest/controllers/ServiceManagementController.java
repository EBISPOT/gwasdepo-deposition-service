package uk.ac.ebi.spot.gwas.deposition.rest.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.service.*;

@RestController
@RequestMapping(value = GeneralCommon.API_V1)
public class ServiceManagementController {

    private static final Logger log = LoggerFactory.getLogger(ServiceManagementController.class);

    @Autowired(required = false)
    private SOLRService solrService;

    @Autowired
    private FileHandlerService fileHandlerService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private UserService userService;

    /**
     * GET /v1/recreate-ss-template
     */
    @GetMapping(value = GWASDepositionBackendConstants.API_RECREATE_TEMPLATE + "/{pmid}")
    @ResponseStatus(HttpStatus.OK)
    public void reindexPublications(@PathVariable String pmid) {
        log.info("Request to recreate SS template for submission on publication: {}", pmid);
        Publication publication = publicationService.retrievePublication(pmid, false);
        Submission submission = submissionService.getSubmission(publication.getId());
        fileHandlerService.handleSummaryStatsTemplate(submission, publication, userService.getUser(submission.getCreated().getUserId()));
    }

    /**
     * GET /v1/reindex-publications
     */
    @GetMapping(value = GWASDepositionBackendConstants.API_REINDEX_PUBLICATIONS)
    @ResponseStatus(HttpStatus.OK)
    public void reindexPublications() {
        log.info("Request to reindex publications. SOLR active: {}", (solrService != null));
        if (solrService != null) {
            solrService.reindexPublications();
        }
    }

    /**
     * GET /v1/clear-publications
     */
    @GetMapping(value = GWASDepositionBackendConstants.API_CLEAR_PUBLICATIONS)
    @ResponseStatus(HttpStatus.OK)
    public void clearPublications() {
        log.info("Request to clear publications. SOLR active: {}", (solrService != null));
        if (solrService != null) {
            solrService.clearPublications();
        }
    }

}
