package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.spot.gwas.deposition.domain.Manuscript;

public interface ManuscriptService {
    Manuscript createManuscript(Manuscript manuscript);

    Manuscript retrieveManuscript(String manuscriptId, String userId);

    Page<Manuscript> retrieveManuscripts(String userId, Pageable pageable);
}
