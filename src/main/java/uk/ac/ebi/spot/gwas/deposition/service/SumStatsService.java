package uk.ac.ebi.spot.gwas.deposition.service;


import uk.ac.ebi.spot.gwas.deposition.domain.SSGlobusResponse;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSGlobusFolderDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSWrapUpRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsResponseDto;

public interface SumStatsService {

    SummaryStatsResponseDto retrieveSummaryStatsStatus(String callbackId);

    void wrapUpGlobusSubmission(String callbackId, SSWrapUpRequestDto ssWrapUpRequestDto);

    String registerStatsForProcessing(SummaryStatsRequestDto summaryStatsRequestDto);

    void cleanUp(String callbackId);

    void deleteGlobusFolder(Submission submission);

    SSGlobusResponse createGlobusFolder(SSGlobusFolderDto ssGlobusFolderDto);
}
