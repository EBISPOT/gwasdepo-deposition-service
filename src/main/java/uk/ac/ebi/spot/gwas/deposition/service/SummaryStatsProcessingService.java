package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.SummaryStatsEntry;

import java.util.List;

public interface SummaryStatsProcessingService {

    void processSummaryStats(Submission submission, String fileUploadId, List<SummaryStatsEntry> summaryStatsEntries, Publication publication, BodyOfWork bodyOfWork);

    void callGlobusWrapUp(String submissionId);
}
