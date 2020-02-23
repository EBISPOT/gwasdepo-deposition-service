package uk.ac.ebi.spot.gwas.deposition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MailConfig {

    @Value("${gwas-deposition.email-config.from-address}")
    private String fromAddress;

    @Value("${gwas-deposition.email-config.from-name}")
    private String fromName;

    @Value("${gwas-deposition.email-config.retries:3}")
    private int retryCount;

    @Value("${gwas-deposition.email-config.subject}")
    private String subject;

    @Value("${gwas-deposition.email-config.emails.success}")
    private String successEmail;

    @Value("${gwas-deposition.email-config.emails.fail}")
    private String failEmail;

    @Value("${gwas-deposition.email-config.email-enabled}")
    private boolean emailEnabled;

    public String getFromAddress() {
        return fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public String getSubject() {
        return subject;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getSuccessEmail() {
        return successEmail;
    }

    public String getFailEmail() {
        return failEmail;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }
}
