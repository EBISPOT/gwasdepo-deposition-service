package uk.ac.ebi.spot.gwas.deposition.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.RabbitMQConfigProperties;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.dto.StudyDto;

@Component
public class StudyIngestPublisher {

    private static final Logger log = LoggerFactory.getLogger(StudyIngestPublisher.class);

    private RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitMQConfigProperties rabbitMQConfigProperties;

    public StudyIngestPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(StudyDto studyDto) {
        log.info("Sending Message for"+studyDto.getSubmissionId()+":"+studyDto.getAccession());
        //rabbitTemplate.convertAndSend(DepositionCurationConstants.ROUTING_KEY, studyDto);
        log.info("Queue In Publisher "+rabbitMQConfigProperties.getQueueName());
        log.info("Exchange In Publisher "+rabbitMQConfigProperties.getExchangeName());
        log.info("Routing key In Publisher "+rabbitMQConfigProperties.getRoutingKey());
        rabbitTemplate.convertAndSend(rabbitMQConfigProperties.getExchangeName(),rabbitMQConfigProperties.getRoutingKey()
        , studyDto);
    }
}
