package uk.ac.ebi.spot.gwas.deposition.service.impl.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Context;
import uk.ac.ebi.spot.gwas.deposition.messaging.email.AbstractEmailBuilder;
import uk.ac.ebi.spot.gwas.deposition.messaging.email.EmailBuilder;

import java.util.Map;

public class SuccessEmailBuilder extends AbstractEmailBuilder implements EmailBuilder {

    private static final Logger log = LoggerFactory.getLogger(SuccessEmailBuilder.class);

    public SuccessEmailBuilder(String emailFile) {
        super(emailFile);
    }

    @Override
    public String getEmailContent(Map<String, Object> metadata) {
        log.info("Building success email from: {}", emailFile);
        String content = super.readEmailContent();
        if (content != null) {
            Context context = new Context();
            for (String variable : metadata.keySet()) {
                log.info("Mail Key:"+variable);
                Object variableValue = metadata.get(variable);
                log.info("Mail Key Value:"+variableValue.toString());
                context.setVariable(variable, variableValue);
            }
            return templateEngine.process(content, context);
        }
        return null;
    }
}
