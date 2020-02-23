package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;

import java.util.List;

public interface StudiesService {
    Page<Study> getStudies(Submission submission, Pageable pageable);

    void deleteStudies(List<String> studies);

    List<Study> retrieveStudies(List<String> studies);

    Study getStudy(String studyId);
}
