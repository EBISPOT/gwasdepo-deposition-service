package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.spot.gwas.deposition.domain.Sample;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;

import java.util.List;

public interface SamplesService {
    Page<Sample> getSamples(Submission submission, Pageable pageable);

    void deleteSamples(List<String> samples);

    List<Sample> retrieveSamples(List<String> samples);

    Sample getSample(String sampleId);
}
