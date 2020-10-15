package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;

public interface SummaryStatsValidationService {
    void validateSummaryStatsData(Submission submission, User user);
}
