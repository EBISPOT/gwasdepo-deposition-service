package uk.ac.ebi.spot.gwas.deposition.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.dto.StudyDto;

@Component
public class StudyIngestPublisher {

    private static final Logger log = LoggerFactory.getLogger(StudyIngestPublisher.class);

    private RabbitTemplate rabbitTemplate;

    public StudyIngestPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(StudyDto studyDto) {
        log.info("Sending Message for"+studyDto.getSubmissionId()+":"+studyDto.getAccession());
        //rabbitTemplate.convertAndSend(DepositionCurationConstants.ROUTING_KEY, studyDto);

        rabbitTemplate.convertAndSend(GWASDepositionBackendConstants.EXCHANGE_NAME,GWASDepositionBackendConstants.ROUTING_KEY
        , studyDto);
    }
}
