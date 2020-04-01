package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.repository.BodyOfWorkRepository;
import uk.ac.ebi.spot.gwas.deposition.service.BodyOfWorkService;

import java.util.Optional;

@Service
public class BodyOfWorkServiceImpl implements BodyOfWorkService {

    private static final Logger log = LoggerFactory.getLogger(BodyOfWorkService.class);

    @Autowired
    private BodyOfWorkRepository bodyOfWorkRepository;

    @Override
    public BodyOfWork createBodyOfWork(BodyOfWork bodyOfWork) {
        log.info("Creating body of work: {}", bodyOfWork.getTitle());
        bodyOfWork = bodyOfWorkRepository.insert(bodyOfWork);
        log.info("Body of work created: {}", bodyOfWork.getId());
        return bodyOfWork;
    }

    @Override
    public BodyOfWork retrieveBodyOfWork(String bodyOfWorkId, String userId) {
        log.info("[{}] Retrieving body of work: {}", userId, bodyOfWorkId);
        Optional<BodyOfWork> optionalBodyOfWork = bodyOfWorkRepository.findByIdAndArchivedAndCreated_UserId(bodyOfWorkId, false, userId);
        if (!optionalBodyOfWork.isPresent()) {
            log.error("Unable to find body of work with ID: {}", bodyOfWorkId);
            throw new EntityNotFoundException("Unable to find body of work with ID: " + bodyOfWorkId);
        }

        log.info("Returning body of work: {}", optionalBodyOfWork.get().getId());
        return optionalBodyOfWork.get();
    }

    @Override
    public Page<BodyOfWork> retrieveBodyOfWorks(String userId, Pageable pageable) {
        log.info("[{}] Retrieving body of works.", userId);
        Page<BodyOfWork> bodyOfWorks = bodyOfWorkRepository.findByArchivedAndCreated_UserId(false, userId, pageable);
        return bodyOfWorks;
    }
}
