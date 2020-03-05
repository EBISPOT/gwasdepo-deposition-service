package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.domain.CorrespondingAuthor;
import uk.ac.ebi.spot.gwas.deposition.domain.Manuscript;
import uk.ac.ebi.spot.gwas.deposition.domain.Provenance;
import uk.ac.ebi.spot.gwas.deposition.dto.PublicationDto;

public class ManuscriptDtoDisassembler {

    public static Manuscript diassemble(PublicationDto publicationDto, Provenance provenance) {
        return new Manuscript(publicationDto.getTitle(),
                publicationDto.getFirstAuthor(),
                publicationDto.getJournal(),
                publicationDto.getPublicationDate(),
                publicationDto.getPublicationDate(),
                publicationDto.getCorrespondingAuthor() != null ?
                        new CorrespondingAuthor(publicationDto.getCorrespondingAuthor().getAuthorName(),
                                publicationDto.getCorrespondingAuthor().getEmail()) : null,
                provenance);
    }

}
