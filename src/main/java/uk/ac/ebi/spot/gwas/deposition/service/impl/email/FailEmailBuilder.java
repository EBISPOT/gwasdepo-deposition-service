package uk.ac.ebi.spot.gwas.deposition.service.impl.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Context;
import uk.ac.ebi.spot.gwas.deposition.constants.MailConstants;
import uk.ac.ebi.spot.gwas.deposition.messaging.email.AbstractEmailBuilder;
import uk.ac.ebi.spot.gwas.deposition.messaging.email.EmailBuilder;

import java.util.List;
import java.util.Map;

public class FailEmailBuilder extends AbstractEmailBuilder implements EmailBuilder {

    private static final Logger log = LoggerFactory.getLogger(FailEmailBuilder.class);

    private List<String> errors;

    public FailEmailBuilder(String emailFile, List<String> errors) {
        super(emailFile);
        this.errors = errors;
    }

    @Override
    public String getEmailContent(Map<String, Object> metadata) {
        log.info("Building fail email from: {}", emailFile);
        String content = super.readEmailContent();
        if (content != null) {
            Context context = new Context();
            for (String variable : metadata.keySet()) {
                Object variableValue = metadata.get(variable);
                context.setVariable(variable, variableValue);
            }
            context.setVariable(MailConstants.ERRORS, errors);

            return templateEngine.process(content, context);
        }
        return null;
    }
}
