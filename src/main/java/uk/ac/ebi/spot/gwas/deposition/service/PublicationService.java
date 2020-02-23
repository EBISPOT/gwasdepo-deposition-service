package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.domain.SSTemplateEntry;

import java.util.List;

public interface PublicationService {

    Publication retrievePublication(String id, boolean isId);

    Page<Publication> getPublications(String author, String title, Pageable page);

    void savePublication(Publication publication);

    List<SSTemplateEntry> retrieveSSTemplateEntries(String pmid);
}
