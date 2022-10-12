package uk.ac.ebi.spot.gwas.deposition.solr;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.SolrCrudRepository;

import java.util.Optional;

public interface PublicationSOLRRepository extends SolrCrudRepository<SOLRPublication, String> {

    @Query(value = "firstAuthor:?0*")
    Page<SOLRPublication> findByFirstAuthor(String author, Pageable page);

    @Query(value = "title:*?0*")
    Page<SOLRPublication> findByTitle(String title, Pageable page);

    Optional<SOLRPublication> findByPmid(String pmid);
}
