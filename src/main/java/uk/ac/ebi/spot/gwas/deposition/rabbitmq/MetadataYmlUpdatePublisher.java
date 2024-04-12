package uk.ac.ebi.spot.gwas.deposition.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.RabbitMQConfigProperties;
import uk.ac.ebi.spot.gwas.deposition.dto.curation.MetadataYmlUpdate;

@Component
public class MetadataYmlUpdatePublisher {
    private static final Logger log = LoggerFactory.getLogger(MetadataYmlUpdatePublisher.class);

    private RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitMQConfigProperties rabbitMQConfigProperties;


    public MetadataYmlUpdatePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(MetadataYmlUpdate metadataYmlUpdate) {
        log.info("Sending Message for "+metadataYmlUpdate.getArgs() != null ? metadataYmlUpdate.getArgs().get(0) : "Empty" );
        //rabbitTemplate.convertAndSend(DepositionCurationConstants.ROUTING_KEY, studyDto);

        rabbitTemplate.convertAndSend(rabbitMQConfigProperties.getSumstatsExchangeName(),rabbitMQConfigProperties.getSumstatsRoutingKey()
                , metadataYmlUpdate);
    }
}
