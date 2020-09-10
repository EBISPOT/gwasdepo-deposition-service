package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.components.BodyOfWorkListener;
import uk.ac.ebi.spot.gwas.deposition.constants.PublicationStatus;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.repository.BodyOfWorkRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.PublicationRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.StudyRepository;
import uk.ac.ebi.spot.gwas.deposition.service.BodyOfWorkService;
import uk.ac.ebi.spot.gwas.deposition.service.CuratorAuthService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;
import uk.ac.ebi.spot.gwas.deposition.util.GCPCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BodyOfWorkServiceImpl implements BodyOfWorkService {

    private static final Logger log = LoggerFactory.getLogger(BodyOfWorkService.class);

    @Autowired
    private BodyOfWorkRepository bodyOfWorkRepository;

    @Autowired
    private GCPCounter gcpCounter;

    @Autowired
    private CuratorAuthService curatorAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private BodyOfWorkListener bodyOfWorkListener;

    @Autowired
    private PublicationRepository publicationRepository;

    @Override
    public BodyOfWork createBodyOfWork(BodyOfWork bodyOfWork) {
        log.info("Creating body of work: {}", bodyOfWork.getTitle());
        bodyOfWork.setBowId(gcpCounter.getNext());
        bodyOfWork = bodyOfWorkRepository.insert(bodyOfWork);
        log.info("Body of work created: {}", bodyOfWork.getId());
        return bodyOfWork;
    }

    @Override
    public BodyOfWork retrieveBodyOfWork(String bodyOfWorkId, User user) {
        log.info("[{}] Retrieving body of work: {}", user.getId(), bodyOfWorkId);
        Optional<BodyOfWork> optionalBodyOfWork;
        if (curatorAuthService.isCurator(user)) {
            optionalBodyOfWork = bodyOfWorkRepository.findByBowIdAndArchived(bodyOfWorkId, false);
        } else {
            optionalBodyOfWork = bodyOfWorkRepository.findByBowIdAndArchivedAndCreated_UserId(bodyOfWorkId, false, user.getId());
        }
        if (!optionalBodyOfWork.isPresent()) {
            log.error("Unable to find body of work with ID: {}", bodyOfWorkId);
            throw new EntityNotFoundException("Unable to find body of work with ID: " + bodyOfWorkId);
        }

        log.info("Returning body of work: {}", optionalBodyOfWork.get().getBowId());
        return optionalBodyOfWork.get();
    }

    @Override
    public BodyOfWork retrieveBodyOfWork(String bodyOfWorkId, String userId) {
        User user = userService.getUser(userId);
        return this.retrieveBodyOfWork(bodyOfWorkId, user);
    }

    @Override
    public Page<BodyOfWork> retrieveBodyOfWorks(User user, String status, Pageable pageable) {
        log.info("[{}] Retrieving body of works.", user.getId());
        if (curatorAuthService.isCurator(user)) {
            if (status != null) {
                return bodyOfWorkRepository.findByStatusAndArchived(status, false, pageable);
            }
            return bodyOfWorkRepository.findByArchived(false, pageable);
        }
        if (status != null) {
            return bodyOfWorkRepository.findByStatusAndArchivedAndCreated_UserId(status, false, user.getId(), pageable);
        }

        return bodyOfWorkRepository.findByArchivedAndCreated_UserId(false, user.getId(), pageable);
    }

    @Override
    public void deleteBodyOfWork(String bodyofworkId, User user) {
        log.info("[{}] Deleting body of work: {}", user.getId(), bodyofworkId);
        Optional<BodyOfWork> optionalBodyOfWork;
        if (curatorAuthService.isCurator(user)) {
            optionalBodyOfWork = bodyOfWorkRepository.findByBowIdAndArchived(bodyofworkId, false);
        } else {
            optionalBodyOfWork = bodyOfWorkRepository.findByBowIdAndArchivedAndCreated_UserId(bodyofworkId, false, user.getId());
        }
        if (!optionalBodyOfWork.isPresent()) {
            log.error("Unable to find body of work with ID: {}", bodyofworkId);
            throw new EntityNotFoundException("Unable to find body of work with ID: " + bodyofworkId);
        }
        BodyOfWork bodyOfWork = optionalBodyOfWork.get();
        bodyOfWork.setLastUpdated(new Provenance(DateTime.now(), user.getId()));
        bodyOfWork.setArchived(true);
        bodyOfWorkRepository.save(bodyOfWork);
        log.info("Body of work successfully deleted: {}", bodyOfWork.getBowId());
    }

    @Override
    public void save(BodyOfWork bodyOfWork) {
        log.info("Saving: {}", bodyOfWork.getBowId());
        bodyOfWorkRepository.save(bodyOfWork);
    }

    @Override
    public BodyOfWork updateBodyOfWork(String bodyofworkId, BodyOfWork bodyOfWork, User user) {
        log.info("[{}] Retrieving body of work: {}", user.getId(), bodyofworkId);
        Optional<BodyOfWork> optionalBodyOfWork;
        if (curatorAuthService.isCurator(user)) {
            optionalBodyOfWork = bodyOfWorkRepository.findByBowIdAndArchived(bodyofworkId, false);
        } else {
            optionalBodyOfWork = bodyOfWorkRepository.findByBowIdAndArchivedAndCreated_UserId(bodyofworkId, false, user.getId());
        }
        if (!optionalBodyOfWork.isPresent()) {
            log.error("Unable to find body of work with ID: {}", bodyofworkId);
            throw new EntityNotFoundException("Unable to find body of work with ID: " + bodyofworkId);
        }
        BodyOfWork existing = optionalBodyOfWork.get();
        existing.setCorrespondingAuthors(bodyOfWork.getCorrespondingAuthors());
        existing.setDescription(bodyOfWork.getDescription());
        existing.setDoi(bodyOfWork.getDoi());
        existing.setEmbargoDate(bodyOfWork.getEmbargoDate());
        existing.setEmbargoUntilPublished(bodyOfWork.getEmbargoUntilPublished());
        existing.setFirstAuthor(bodyOfWork.getFirstAuthor());
        existing.setJournal(bodyOfWork.getJournal());
        existing.setLastAuthor(bodyOfWork.getLastAuthor());
        existing.setPmids(bodyOfWork.getPmids() != null ? bodyOfWork.getPmids() : existing.getPmids());
        existing.setPrePrintServer(bodyOfWork.getPrePrintServer());
        existing.setPreprintServerDOI(bodyOfWork.getPreprintServerDOI());
        existing.setTitle(bodyOfWork.getTitle());
        existing.setUrl(bodyOfWork.getUrl());

        String publicationId = null;
        if (existing.getPmids() != null) {
            if (!existing.getPmids().isEmpty()) {
                List<Study> studyList = studyRepository.findByBodyOfWorkListContains(bodyofworkId);
                for (Study study : studyList) {
                    List<String> pmids = study.getPmids() != null ? study.getPmids() : new ArrayList<>();
                    for (String pmid : existing.getPmids()) {
                        if (!pmids.contains(pmid)) {
                            pmids.add(pmid);
                        }
                    }

                    study.setPmids(pmids);
                    studyRepository.save(study);
                }

                Optional<Publication> publicationOptional = publicationRepository.findByPmid(existing.getPmids().get(0));
                if (publicationOptional.isPresent()) {
                    Publication publication = publicationOptional.get();
                    publicationId = publication.getId();
                    publication.setStatus(PublicationStatus.UNDER_SUBMISSION.name());
                    publicationRepository.save(publication);
                }
            }
        }

        existing = bodyOfWorkRepository.save(existing);
        bodyOfWorkListener.update(bodyOfWork, publicationId);
        return existing;
    }
}
