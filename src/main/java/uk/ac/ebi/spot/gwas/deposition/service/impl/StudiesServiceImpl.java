package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.repository.StudyRepository;
import uk.ac.ebi.spot.gwas.deposition.service.StudiesService;
import uk.ac.ebi.spot.gwas.deposition.util.IdCollector;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class StudiesServiceImpl implements StudiesService {

    private static final Logger log = LoggerFactory.getLogger(StudiesService.class);

    @Autowired
    private StudyRepository studyRepository;

    @Override
    public Page<Study> getStudies(Submission submission, Pageable page) {
        log.info("Retrieving studies: {} - {} - {}", page.getPageNumber(), page.getPageSize(), page.getSort());
        return studyRepository.findBySubmissionId(submission.getId(), page);
    }

    @Override
    @Async
    public void deleteStudies(List<String> studies) {
        log.info("Removing {} studies.", studies.size());
        List<Study> studyList = studyRepository.findByIdIn(studies);
        for (Study study : studyList) {
            studyRepository.delete(study);
        }
        log.info("Successfully removed {} studies.", studies.size());
    }

    @Override
    public List<Study> retrieveStudies(List<String> studies) {
        log.info("Retrieving studies: {}", studies);
        List<Study> studyList = studyRepository.findByIdIn(studies);
        log.info("Found {} studies.", studyList.size());
        return studyList;
    }

    @Override
    public Study getStudy(String studyId) {
        log.info("Retrieving study: {}", studyId);
        Optional<Study> studyOptional = studyRepository.findById(studyId);
        if (studyOptional.isPresent()) {
            log.info("Found study: {}", studyOptional.get().getStudyTag());
            return studyOptional.get();
        }
        log.error("Unable to find study: {}", studyId);
        return null;
    }

    @Override
    public void deleteStudies(String submissionId) {
        log.info("Removing studies for submission: {}", submissionId);
        Stream<Study> studyStream = studyRepository.readBySubmissionId(submissionId);
        IdCollector idCollector = new IdCollector();
        studyStream.forEach(study -> idCollector.addId(study.getId()));
        studyStream.close();
        log.info(" - Found {} studies.", idCollector.getIds().size());
        for (String id : idCollector.getIds()) {
            studyRepository.deleteById(id);
        }
    }

}
