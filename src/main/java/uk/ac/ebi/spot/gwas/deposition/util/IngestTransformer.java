package uk.ac.ebi.spot.gwas.deposition.util;

import uk.ac.ebi.spot.gwas.deposition.constants.PublicationStatus;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.CorrespondingAuthorDto;
import uk.ac.ebi.spot.gwas.deposition.dto.ingest.ExtendedPublicationDto;
import uk.ac.ebi.spot.gwas.deposition.dto.ingest.PublicationDto;
import uk.ac.ebi.spot.gwas.deposition.dto.ingest.SubmissionDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateEntryDto;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.ProvenanceDtoAssembler;

import java.util.ArrayList;
import java.util.List;

public class IngestTransformer {

    public static Publication fromIngest(ExtendedPublicationDto extendedPublicationDto) {
        CorrespondingAuthor correspondingAuthor = extendedPublicationDto.getCorrespondingAuthor() != null ?
                new CorrespondingAuthor(extendedPublicationDto.getCorrespondingAuthor().getAuthorName(),
                        extendedPublicationDto.getCorrespondingAuthor().getEmail()) : null;

        return new Publication(extendedPublicationDto.getPmid(),
                extendedPublicationDto.getJournal(),
                extendedPublicationDto.getTitle(),
                extendedPublicationDto.getFirstAuthor(),
                extendedPublicationDto.getPublicationDate(),
                correspondingAuthor,
                extendedPublicationDto.getStatus() != null ? extendedPublicationDto.getStatus() : PublicationStatus.ELIGIBLE.name());
    }

    public static List<SSTemplateEntry> templateEntriesFromInges(ExtendedPublicationDto extendedPublicationDto) {
        List<SSTemplateEntry> ssTemplateEntries = null;

        if (extendedPublicationDto.getSsTemplateEntries() != null) {
            ssTemplateEntries = new ArrayList<>();
            for (SSTemplateEntryDto ssTemplateEntryDto : extendedPublicationDto.getSsTemplateEntries()) {
                ssTemplateEntries.add(new SSTemplateEntry(ssTemplateEntryDto.getStudyAccession(),
                        ssTemplateEntryDto.getStudyTag(),
                        ssTemplateEntryDto.getTrait(),
                        ssTemplateEntryDto.getSampleDescription(),
                        ssTemplateEntryDto.getHasSummaryStats()));
            }
        }

        return ssTemplateEntries;
    }

    public static SubmissionDto toIngest(Submission submission, Publication publication, User user) {
        CorrespondingAuthorDto correspondingAuthor = publication.getCorrespondingAuthor() != null ?
                new CorrespondingAuthorDto(publication.getCorrespondingAuthor().getAuthorName(),
                        publication.getCorrespondingAuthor().getEmail()) : null;
        PublicationDto publicationDto = new PublicationDto(publication.getPmid(),
                publication.getTitle(),
                publication.getJournal(),
                publication.getFirstAuthor(),
                publication.getPublicationDate(),
                correspondingAuthor,
                publication.getStatus());

        return new SubmissionDto(submission.getId(),
                publicationDto,
                submission.getOverallStatus(),
                submission.getGlobusFolderId(),
                submission.getGlobusOriginId(),
                null,
                null,
                null,
                null,
                submission.getDateSubmitted(),
                ProvenanceDtoAssembler.assemble(submission.getCreated(), user));
    }
}
