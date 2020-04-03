package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.domain.Author;
import uk.ac.ebi.spot.gwas.deposition.dto.AuthorDto;

public class AuthorDtoAssembler {

    public static AuthorDto assemble(Author author) {
        if (author == null) {
            return null;
        }
        return new AuthorDto(author.getFirstName(),
                author.getLastName(),
                author.getGroup(),
                author.getEmail());
    }

    public static Author disassemble(AuthorDto author) {
        if (author == null) {
            return null;
        }
        return new Author(author.getFirstName(),
                author.getLastName(),
                author.getGroup(),
                author.getEmail());
    }
}
