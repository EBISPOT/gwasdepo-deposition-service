package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.dto.StudyDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDataDto;

import java.util.List;

public interface SampleDescriptionService {

    public void buildSampleDescription(SubmissionDataDto submissionDataDto, StudyDto studyDto);


}
