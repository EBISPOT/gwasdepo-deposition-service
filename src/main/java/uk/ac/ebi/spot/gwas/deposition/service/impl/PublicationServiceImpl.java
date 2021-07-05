package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.domain.SSTemplateEntry;
import uk.ac.ebi.spot.gwas.deposition.domain.SSTemplateEntryPlaceholder;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.repository.PublicationRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SSTemplateEntryPlaceholderRepository;
import uk.ac.ebi.spot.gwas.deposition.service.PublicationService;
import uk.ac.ebi.spot.gwas.deposition.service.SOLRService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PublicationServiceImpl implements PublicationService {

    private static final Logger log = LoggerFactory.getLogger(PublicationService.class);

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private SSTemplateEntryPlaceholderRepository ssTemplateEntryPlaceholderRepository;

    @Autowired(required = false)
    private SOLRService solrService;

    @Override
    public Publication retrievePublication(String id, boolean isId) {
        log.info("Retrieving publication using id [{}]: {}", isId, id);

        Optional<Publication> optionalPublication = isId ?
                publicationRepository.findById(id) : publicationRepository.findByPmid(id);
        if (!optionalPublication.isPresent()) {
            log.error("Unable to find publication with ID / PMID: {}", id);
            throw new EntityNotFoundException("Unable to find publication with ID / PMID: " + id);
        }

        log.info("Returning publication: {}", optionalPublication.get().getPmid());
        return optionalPublication.get();
    }

    @Override
    public Page<Publication> getPublications(String author, String title, Pageable page) {
        log.info("Retrieving publications: {} - {}", author, title);

        if (author != null || title != null) {
            if (solrService != null) {
                return author != null ? solrService.findPublicationsByAuthor(author, page) :
                        solrService.findPublicationsByTitle(title, page);
            }
            return publicationRepository.findBy(TextCriteria.forDefaultLanguage().caseSensitive(false).matching(author),
                    page);
        }

        return publicationRepository.findAll(page);
    }

    @Override
    public void savePublication(Publication publication) {
        log.info("Saving publication: {}", publication.getPmid());
        publicationRepository.save(publication);
        if (solrService != null) {
            solrService.updatePublication(publication);
        }
    }

    @Override
    public List<SSTemplateEntry> retrieveSSTemplateEntries(String pmid) {
        log.info("Retrieving SS template entries for PMID: {}", pmid);
        Optional<SSTemplateEntryPlaceholder> ssTemplateEntryPlaceholderOptional = ssTemplateEntryPlaceholderRepository.findByPmid(pmid);
        if (ssTemplateEntryPlaceholderOptional.isPresent()) {
            log.info("SS Template Entries placeholder found: {}", ssTemplateEntryPlaceholderOptional.get().getId());
            return ssTemplateEntryPlaceholderOptional.get().getSsTemplateEntries();
        }

        log.info("No SS Template Entries placeholder for PMID: {}", pmid);
        return new ArrayList<>();
    }
}
