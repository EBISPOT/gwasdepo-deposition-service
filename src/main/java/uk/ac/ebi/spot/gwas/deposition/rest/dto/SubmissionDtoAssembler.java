package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.dto.*;

import java.util.List;

public class SubmissionDtoAssembler {

    public static SubmissionDto assemble(Submission submission,
                                         PublicationDto publication,
                                         BodyOfWorkDto bodyOfWork,
                                         List<FileUploadDto> fileUploads,
                                         ProvenanceDto created,
                                         ProvenanceDto lastUpdated) {
        return new SubmissionDto(submission.getId(),
                publication,
                bodyOfWork,
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
                submission.getProvenanceType(),
                created, lastUpdated,
                submission.isAgreedToCc0()
        );
    }
}
