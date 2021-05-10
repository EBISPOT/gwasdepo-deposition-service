package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.http.ResponseEntity;
import uk.ac.ebi.spot.gwas.deposition.javers.JaversChangeWrapper;

import java.util.List;

public interface SubmissionDiffService {

    public ResponseEntity<List<JaversChangeWrapper>>  diffVersionsSubmission(String submissionId, String jwtToken);
}
