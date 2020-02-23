package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.Submission;

public interface SummaryStatsValidationService {
    void validateSummaryStatsData(Submission submission);
}
