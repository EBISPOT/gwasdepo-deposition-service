package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.domain.Author;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.domain.Provenance;
import uk.ac.ebi.spot.gwas.deposition.dto.BodyOfWorkDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BodyOfWorkDtoDisassembler {

    public static BodyOfWork disassemble(BodyOfWorkDto bodyOfWorkDto, Provenance provenance) {
        List<Author> correspondingAuthorList = new ArrayList<>();
        if (bodyOfWorkDto.getCorrespondingAuthors() != null) {
            correspondingAuthorList = bodyOfWorkDto.getCorrespondingAuthors().stream()
                    .map(AuthorDtoAssembler::disassemble).collect(Collectors.toList());
        }

        BodyOfWork bodyOfWork = new BodyOfWork(bodyOfWorkDto.getTitle(),
                bodyOfWorkDto.getDescription(),
                bodyOfWorkDto.getJournal(),
                bodyOfWorkDto.getDoi(),
                bodyOfWorkDto.getUrl(),
                AuthorDtoAssembler.disassemble(bodyOfWorkDto.getFirstAuthor()),
                AuthorDtoAssembler.disassemble(bodyOfWorkDto.getLastAuthor()),
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
