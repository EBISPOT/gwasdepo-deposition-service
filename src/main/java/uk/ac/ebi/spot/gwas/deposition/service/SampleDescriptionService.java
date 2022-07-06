package uk.ac.ebi.spot.gwas.deposition.service;

import org.apache.commons.lang3.tuple.Pair;
import uk.ac.ebi.spot.gwas.deposition.dto.StudyDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDataDto;

import java.util.List;

public interface SampleDescriptionService {

    public Pair<String, String> buildSampleDescription(SubmissionDataDto submissionDataDto, StudyDto studyDto);


}
