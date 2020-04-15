package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.domain.Provenance;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.repository.BodyOfWorkRepository;
import uk.ac.ebi.spot.gwas.deposition.service.BodyOfWorkService;
import uk.ac.ebi.spot.gwas.deposition.util.GCPCounter;

import java.util.Optional;

@Service
public class BodyOfWorkServiceImpl implements BodyOfWorkService {

    private static final Logger log = LoggerFactory.getLogger(BodyOfWorkService.class);

    @Autowired
    private BodyOfWorkRepository bodyOfWorkRepository;

    @Autowired
    private GCPCounter gcpCounter;

    @Override
    public BodyOfWork createBodyOfWork(BodyOfWork bodyOfWork) {
        log.info("Creating body of work: {}", bodyOfWork.getTitle());
        bodyOfWork.setBowId(gcpCounter.getNext());
        bodyOfWork = bodyOfWorkRepository.insert(bodyOfWork);
        log.info("Body of work created: {}", bodyOfWork.getId());
        return bodyOfWork;
    }

    @Override
    public BodyOfWork retrieveBodyOfWork(String bodyOfWorkId, String userId) {
        log.info("[{}] Retrieving body of work: {}", userId, bodyOfWorkId);
        Optional<BodyOfWork> optionalBodyOfWork = bodyOfWorkRepository.findByBowIdAndArchivedAndCreated_UserId(bodyOfWorkId, false, userId);
        if (!optionalBodyOfWork.isPresent()) {
            log.error("Unable to find body of work with ID: {}", bodyOfWorkId);
            throw new EntityNotFoundException("Unable to find body of work with ID: " + bodyOfWorkId);
        }

        log.info("Returning body of work: {}", optionalBodyOfWork.get().getBowId());
        return optionalBodyOfWork.get();
    }

    @Override
    public Page<BodyOfWork> retrieveBodyOfWorks(String userId, Pageable pageable) {
        log.info("[{}] Retrieving body of works.", userId);
        Page<BodyOfWork> bodyOfWorks = bodyOfWorkRepository.findByArchivedAndCreated_UserId(false, userId, pageable);
        return bodyOfWorks;
    }

    @Override
    public void deleteBodyOfWork(String bodyofworkId, String userId) {
        log.info("[{}] Deleting body of work: {}", userId, bodyofworkId);
        Optional<BodyOfWork> optionalBodyOfWork = bodyOfWorkRepository.findByBowIdAndArchivedAndCreated_UserId(bodyofworkId, false, userId);
        if (!optionalBodyOfWork.isPresent()) {
            log.error("Unable to find body of work with ID: {}", bodyofworkId);
            throw new EntityNotFoundException("Unable to find body of work with ID: " + bodyofworkId);
        }
        BodyOfWork bodyOfWork = optionalBodyOfWork.get();
        bodyOfWork.setLastUpdated(new Provenance(DateTime.now(), userId));
        bodyOfWork.setArchived(true);
        bodyOfWorkRepository.save(bodyOfWork);
        log.info("Body of work successfully deleted: {}", bodyOfWork.getBowId());
    }

    @Override
    public void save(BodyOfWork bodyOfWork) {
        log.info("Saving: {}", bodyOfWork.getBowId());
        bodyOfWorkRepository.save(bodyOfWork);
    }
}
