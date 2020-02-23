package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;

public interface SOLRService {

    void reindexPublications();

    void clearPublications();

    Page<Publication> findPublicationsByAuthor(String author, Pageable page);

    Page<Publication> findPublicationsByTitle(String title, Pageable page);

    void addPublication(Publication publication);

    void updatePublication(Publication publication);

}
