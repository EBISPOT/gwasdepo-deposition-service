package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.spot.gwas.deposition.domain.Association;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;

import java.util.List;

public interface AssociationsService {
    Page<Association> getAssociations(Submission submission, Pageable pageable);

    void deleteAssociations(List<String> associations);

    List<Association> retrieveAssociations(List<String> associations);

    Association getAssociation(String associationId);

    void deleteAssociations(String submissionId);
}
