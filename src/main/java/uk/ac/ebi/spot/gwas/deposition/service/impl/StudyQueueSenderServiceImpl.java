package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.dto.curation.MetadataYmlUpdate;
import uk.ac.ebi.spot.gwas.deposition.rabbitmq.MetadataYmlUpdatePublisher;
import uk.ac.ebi.spot.gwas.deposition.rabbitmq.StudyIngestPublisher;
import uk.ac.ebi.spot.gwas.deposition.repository.StudyRepository;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.StudyDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.StudyRabbitMessageAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.StudiesService;
import uk.ac.ebi.spot.gwas.deposition.service.StudyQueueSenderService;

import java.util.Collections;
import java.util.UUID;

@Service
public class StudyQueueSenderServiceImpl implements StudyQueueSenderService {

    private static final Logger log = LoggerFactory.getLogger(StudyQueueSenderServiceImpl.class);

    @Autowired
    StudiesService studiesService;

    @Autowired
    StudyRepository studyRepository;

    @Autowired
    StudyIngestPublisher studyIngestPublisher;

    @Autowired
    MetadataYmlUpdatePublisher metadataYmlUpdatePublisher;

    @Autowired
    StudyRabbitMessageAssembler studyRabbitMessageAssembler;

    @Override
    @Async
    public void sendStudiesToQueue(String submissionId) {
        long count = studyRepository.findBySubmissionId(submissionId, Pageable.unpaged()).stream().count();
        long bucket = count / 100;
        for (int i = 0; i <= bucket; i++) {
            log.info("Sending Studies to Queue Page running is " + i);
            Pageable pageable = new PageRequest(i, 100);
            Page<Study> studies = studyRepository.findBySubmissionId(submissionId, pageable);
            studies.forEach(study -> sendStudyChangeMessage(study));

        }
    }

    @Override
    @Async
    public void sendMetaDataMessageToQueue(String submissionId) {
        Long studiesCount =  studyRepository.findBySubmissionId(submissionId, Pageable.unpaged()).stream().count();
        log.info("Studies count for {} is {}",submissionId,  studiesCount);
        long bucket = studiesCount / 100;
        log.info("Bucket size for studies is {}",bucket);
        for (int i = 0; i <= bucket; i++) {
            log.info("Sending Studies to Queue Page running is " + i);
            Pageable pageable = new PageRequest(i, 100);
            Page<Study> studies = studyRepository.findBySubmissionId(submissionId, pageable);
            log.info("Studies size is -> {}",studies.getTotalElements());
            studies.forEach(study -> metadataYmlUpdatePublisher.send( MetadataYmlUpdate.builder()
                    .args(Collections.singletonList(study.getAccession()))
                    .task("sumstats_service.app.convert_metadata_to_yaml")
                    .id(UUID.randomUUID().toString()).build()));
        }
    }

    private void sendStudyChangeMessage(Study study){
        studyIngestPublisher.send(studyRabbitMessageAssembler.assemble(study));
    }

}
