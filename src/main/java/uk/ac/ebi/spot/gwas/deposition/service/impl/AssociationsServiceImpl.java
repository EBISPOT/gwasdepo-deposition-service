package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.Association;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.repository.AssociationRepository;
import uk.ac.ebi.spot.gwas.deposition.service.AssociationsService;
import uk.ac.ebi.spot.gwas.deposition.util.IdCollector;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class AssociationsServiceImpl implements AssociationsService {

    private static final Logger log = LoggerFactory.getLogger(AssociationsService.class);

    @Autowired
    private AssociationRepository associationRepository;

    @Override
    public Page<Association> getAssociations(Submission submission, Pageable page) {
        log.info("Retrieving associations: {} - {} - {}", page.getPageNumber(), page.getPageSize(), page.getSort().toString());
        return associationRepository.findBySubmissionId(submission.getId(), page);
    }

    @Override
    @Async
    public void deleteAssociations(List<String> associations) {
        log.info("Removing {} associations.", associations.size());
        List<Association> associationsList = associationRepository.findByIdIn(associations);
        for (Association association : associationsList) {
            associationRepository.delete(association);
        }
        log.info("Successfully removed {} associations.", associations.size());
    }

    @Override
    public List<Association> retrieveAssociations(List<String> associations) {
        log.info("Retrieving associations: {}", associations);
        List<Association> associationsList = associationRepository.findByIdIn(associations);
        log.info("Found {} associations.", associationsList.size());
        return associationsList;
    }

    @Override
    public Association getAssociation(String associationId) {
        log.info("Retrieving association: {}", associationId);
        Optional<Association> associationOptional = associationRepository.findById(associationId);
        if (associationOptional.isPresent()) {
            log.info("Found association: {}", associationOptional.get().getStudyTag());
            return associationOptional.get();
        }
        log.error("Unable to find association: {}", associationId);
        return null;
    }

    @Override
    public void deleteAssociations(String submissionId) {
        log.info("Removing associations for submission: {}", submissionId);
        Stream<Association> associationStream = associationRepository.readBySubmissionId(submissionId);
        IdCollector idCollector = new IdCollector();
        associationStream.forEach(association -> idCollector.addId(association.getId()));
        associationStream.close();
        log.info(" - Found {} associations.", idCollector.getIds().size());
        for (String id : idCollector.getIds()) {
            associationRepository.deleteById(id);
        }
    }

}
