package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.domain.User;

import java.util.List;

public interface MetadataValidationService {
    void validateTemplate(String submissionId, FileUpload fileUpload, byte[] fileContent, User user, List<Study> oldStudies);
}
