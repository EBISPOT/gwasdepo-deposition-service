package uk.ac.ebi.spot.gwas.deposition.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;

@Configuration
public class MyRabbitConfiguration {

    String QUEUE_NAME = GWASDepositionBackendConstants.QUEUE_NAME;
    String EXCHANGE_NAME = GWASDepositionBackendConstants.EXCHANGE_NAME;
    String ROUTING_KEY = GWASDepositionBackendConstants.ROUTING_KEY;

    @Bean
    Queue queue(){
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    DirectExchange exchange(){
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }




}
