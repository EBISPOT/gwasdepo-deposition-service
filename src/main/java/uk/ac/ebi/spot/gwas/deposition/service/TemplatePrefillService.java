package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.FileObject;
import uk.ac.ebi.spot.gwas.deposition.domain.User;

public interface TemplatePrefillService {
    void prefill(String submissionId, User user);

    FileObject prefillGCST(String submissionId, User user);
}
