package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.repository.PublicationRepository;
import uk.ac.ebi.spot.gwas.deposition.service.SOLRService;
import uk.ac.ebi.spot.gwas.deposition.solr.PublicationSOLRRepository;
import uk.ac.ebi.spot.gwas.deposition.solr.SOLRPublication;
import uk.ac.ebi.spot.gwas.deposition.util.SOLRPublicationAssembler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name = "gwas-deposition.solr.enabled", havingValue = "true")
public class SOLRServiceImpl implements SOLRService {

    private static final Logger log = LoggerFactory.getLogger(SOLRService.class);

    @Autowired
    private PublicationSOLRRepository publicationSOLRRepository;

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired(required = false)
    private SOLRIndexService solrIndexService;

    @Autowired
    private SOLRPublicationAssembler solrPublicationAssembler;

    @Async
    public void reindexPublications() {
        log.info("Removing all publications ...");
        try {
            publicationSOLRRepository.deleteAll();

            log.info("Reindexing publications ...");
            Stream<Publication> publicationStream = publicationRepository.findAllByCustomQueryAndStream();
            publicationStream.forEach(p -> solrIndexService.indexPublication(p));
            publicationStream.close();
            log.info("Reindexing completed.");
        } catch (Exception e) {
            log.error("ERROR: {}", e.getMessage(), e);
        }
    }

    @Override
    public void clearPublications() {
        log.info("Removing all publications ...");
        publicationSOLRRepository.deleteAll();
    }

    @Override
    public Page<Publication> findPublicationsByAuthor(String author, Pageable page) {
        log.info("Searching SOLR by publication author: {}", author);
        Page<SOLRPublication> solrPublications = publicationSOLRRepository.findByFirstAuthor(author, page);
        List<Publication> publications = transform(solrPublications.getContent());
        return new PageImpl<>(publications, page, solrPublications.getTotalElements());
    }

    @Override
    public Page<Publication> findPublicationsByTitle(String title, Pageable page) {
        log.info("Searching SOLR by publication title: {}", title);
        Page<SOLRPublication> solrPublications = publicationSOLRRepository.findByTitle(title, page);
        List<Publication> publications = transform(solrPublications.getContent());
        return new PageImpl<>(publications, page, solrPublications.getTotalElements());
    }

    @Override
    public void addPublication(Publication publication) {
        log.info("Adding publication: {}", publication.getPmid());
        SOLRPublication solrPublication = solrPublicationAssembler.assemble(publication);
        if (solrPublication != null) {
            publicationSOLRRepository.save(solrPublication);
        }
    }

    @Override
    public void updatePublication(Publication publication) {
        log.info("Updating publication: {}", publication.getPmid());
        Optional<SOLRPublication> solrPublicationOptional = publicationSOLRRepository.findByPmid(publication.getPmid());
        log.info("Publication found: {}", solrPublicationOptional.isPresent());
        if (solrPublicationOptional.isPresent()) {
            publicationSOLRRepository.delete(solrPublicationOptional.get());
        }
        this.addPublication(publication);
    }

    private List<Publication> transform(List<SOLRPublication> solrPublications) {
        log.info("SOLR returned {} results.", solrPublications.size());
        List<Publication> list = new ArrayList<>();
        for (SOLRPublication solrPublication : solrPublications) {
            Publication publication = solrPublicationAssembler.disassemble(solrPublication);
            if (publication != null) {
                list.add(publication);
            }
        }
        return list;
    }
}
