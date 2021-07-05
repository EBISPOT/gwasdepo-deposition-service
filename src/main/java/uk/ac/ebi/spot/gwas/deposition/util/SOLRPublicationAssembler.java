package uk.ac.ebi.spot.gwas.deposition.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import uk.ac.ebi.spot.gwas.deposition.domain.CorrespondingAuthor;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.domain.SOLRPublication;
import java.io.IOException;

public class SOLRPublicationAssembler {

    public static SOLRPublication assemble(Publication publication) {
        try {
            String correspondingAuthor = new ObjectMapper().writeValueAsString(publication.getCorrespondingAuthor());
            return new SOLRPublication(
                    publication.getId(),
                    publication.getPmid(),
                    publication.getTitle(),
                    publication.getFirstAuthor(),
                    publication.getStatus(),
                    publication.getJournal(),
                    publication.getPublicationDate(),
                    correspondingAuthor
            );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Publication disassemble(SOLRPublication solrPublication) {
        CorrespondingAuthor correspondingAuthor = null;
        try {
            if (StringUtils.isNotBlank(solrPublication.getCorrespondingAuthor()) &&
                    !solrPublication.getCorrespondingAuthor().equalsIgnoreCase("null")) {
                correspondingAuthor = new ObjectMapper().readValue(
                        solrPublication.getCorrespondingAuthor(), CorrespondingAuthor.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Publication publication = new Publication(
                solrPublication.getPmid(),
                solrPublication.getJournal(),
                solrPublication.getTitle(),
                solrPublication.getFirstAuthor(),
                solrPublication.getPublicationDate(),
                correspondingAuthor,
                solrPublication.getStatus());
        publication.setId(solrPublication.getPublicationid());
        return publication;
    }
}
