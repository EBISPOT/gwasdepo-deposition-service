package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.Manuscript;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.repository.ManuscriptRepository;
import uk.ac.ebi.spot.gwas.deposition.service.ManuscriptService;

import java.util.Optional;

@Service
public class ManuscriptServiceImpl implements ManuscriptService {

    private static final Logger log = LoggerFactory.getLogger(ManuscriptService.class);

    @Autowired
    private ManuscriptRepository manuscriptRepository;

    @Override
    public Manuscript createManuscript(Manuscript manuscript) {
        log.info("Creating manuscript: {}", manuscript.getTitle());
        manuscript = manuscriptRepository.insert(manuscript);
        log.info("Manuscript created: {}", manuscript.getId());
        return manuscript;
    }

    @Override
    public Manuscript retrieveManuscript(String manuscriptId, String userId) {
        log.info("[{}] Retrieving manuscript: {}", userId, manuscriptId);
        Optional<Manuscript> optionalManuscript = manuscriptRepository.findByIdAndArchivedAndCreated_UserId(manuscriptId, false, userId);
        if (!optionalManuscript.isPresent()) {
            log.error("Unable to find manuscript with ID: {}", manuscriptId);
            throw new EntityNotFoundException("Unable to find manuscript with ID: " + manuscriptId);
        }

        log.info("Returning manuscript: {}", optionalManuscript.get().getId());
        return optionalManuscript.get();
    }

    @Override
    public Page<Manuscript> retrieveManuscripts(String userId, Pageable pageable) {
        log.info("[{}] Retrieving manuscripts.", userId);
        Page<Manuscript> manuscripts = manuscriptRepository.findByArchivedAndCreated_UserId(false, userId, pageable);
        return manuscripts;
    }
}
