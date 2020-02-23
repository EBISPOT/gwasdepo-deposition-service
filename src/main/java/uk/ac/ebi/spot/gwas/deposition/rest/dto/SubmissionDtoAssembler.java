package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.dto.FileUploadDto;
import uk.ac.ebi.spot.gwas.deposition.dto.ProvenanceDto;
import uk.ac.ebi.spot.gwas.deposition.dto.PublicationDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDto;

import java.util.List;

public class SubmissionDtoAssembler {

    public static SubmissionDto assemble(Submission submission,
                                         PublicationDto publication,
                                         List<FileUploadDto> fileUploads,
                                         ProvenanceDto created,
                                         ProvenanceDto lastUpdated) {
        return new SubmissionDto(submission.getId(),
                publication,
                fileUploads,
                submission.getGlobusFolderId(),
                submission.getGlobusOriginId(),
                submission.getStudies().size(),
                submission.getSamples().size(),
                submission.getAssociations().size(),
                submission.getOverallStatus(),
                submission.getMetadataStatus(),
                submission.getSummaryStatsStatus(),
                submission.getDateSubmitted(),
                created, lastUpdated
        );
    }
}
