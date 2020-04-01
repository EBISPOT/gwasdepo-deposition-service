package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.domain.CorrespondingAuthor;
import uk.ac.ebi.spot.gwas.deposition.domain.Provenance;
import uk.ac.ebi.spot.gwas.deposition.dto.BodyOfWorkDto;
import uk.ac.ebi.spot.gwas.deposition.dto.CorrespondingAuthorDto;

import java.util.ArrayList;
import java.util.List;

public class BodyOfWorkDtoDisassembler {

    public static BodyOfWork disassemble(BodyOfWorkDto bodyOfWorkDto, Provenance provenance) {
        List<CorrespondingAuthor> correspondingAuthorList = new ArrayList<>();
        if (bodyOfWorkDto.getCorrespondingAuthors() != null) {
            for (CorrespondingAuthorDto correspondingAuthor : bodyOfWorkDto.getCorrespondingAuthors()) {
                correspondingAuthorList.add(new CorrespondingAuthor(correspondingAuthor.getAuthorName(),
                        correspondingAuthor.getEmail()));
            }
        }

        BodyOfWork bodyOfWork = new BodyOfWork(bodyOfWorkDto.getTitle(),
                bodyOfWorkDto.getDescription(),
                bodyOfWorkDto.getJournal(),
                bodyOfWorkDto.getDoi(),
                bodyOfWorkDto.getUrl(),
                bodyOfWorkDto.getFirstAuthorFirstName(),
                bodyOfWorkDto.getFirstAuthorLastName(),
                correspondingAuthorList,
                bodyOfWorkDto.getPmids(),
                bodyOfWorkDto.getPrePrintServer(),
                bodyOfWorkDto.getPreprintServerDOI(),
                bodyOfWorkDto.getEmbargoDate(),
                bodyOfWorkDto.getEmbargoUntilPublished(),
                provenance);
        if (bodyOfWorkDto.getBodyOfWorkId() != null) {
            bodyOfWork.setId(bodyOfWorkDto.getBodyOfWorkId());
        }
        return bodyOfWork;
    }

}
