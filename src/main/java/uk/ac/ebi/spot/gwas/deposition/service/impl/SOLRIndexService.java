package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.domain.SOLRPublication;
import uk.ac.ebi.spot.gwas.deposition.solr.PublicationSOLRRepository;
import uk.ac.ebi.spot.gwas.deposition.util.SOLRPublicationAssembler;

@Service
@ConditionalOnProperty(name = "gwas-deposition.solr.enabled", havingValue = "true")
public class SOLRIndexService {

    @Autowired
    private PublicationSOLRRepository publicationSOLRRepository;

    public void indexPublication(Publication publication) {
        SOLRPublication solrPublication = SOLRPublicationAssembler.assemble(publication);
        if (solrPublication != null) {
            publicationSOLRRepository.save(solrPublication);
        }
    }

}
