package uk.ac.ebi.spot.gwas.deposition.service.impl.email;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractEmailBuilder implements EmailBuilder {

    private static final Logger log = LoggerFactory.getLogger(EmailBuilder.class);

    protected TemplateEngine templateEngine;

    protected String emailFile;

    public AbstractEmailBuilder(String emailFile) {
        templateEngine = new TemplateEngine();
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(templateResolver);

        this.emailFile = emailFile;
    }

    public String readEmailContent() {
        log.info("Reading email content from: {}", emailFile);
        try {
            InputStream inputStream = new DefaultResourceLoader().getResource(emailFile).getInputStream();
            String emailContent = IOUtils.toString(inputStream, "UTF-8");
            return emailContent;
        } catch (IOException e) {
            log.error("Unable to read email content from [{}]: {}", emailFile, e.getMessage(), e);
            return null;
        }
    }

}
