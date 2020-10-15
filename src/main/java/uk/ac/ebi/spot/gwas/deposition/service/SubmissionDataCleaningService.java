package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.Submission;

public interface SubmissionDataCleaningService {
    void deleteSubmission(Submission submission);

    void cleanSubmission(Submission submission);
}
