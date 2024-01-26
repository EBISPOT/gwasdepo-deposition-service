package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.*;

import java.util.List;

public interface SummaryStatsProcessingService {

    void processSummaryStats(Submission submission, String fileUploadId, List<SummaryStatsEntry> summaryStatsEntries, Publication publication, BodyOfWork bodyOfWork, String userId, String appType);

    void bypassSSValidation(String submissionId, User user);

    void callGlobusWrapUp(String submissionId);
}
