package uk.ac.ebi.spot.gwas.deposition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BackendMailConfig {

    @Value("${gwas-deposition.email-config.subject}")
    private String subject;

    @Value("${gwas-deposition.email-config.emails.success}")
    private String successEmail;

    @Value("${gwas-deposition.email-config.emails.fail}")
    private String failEmail;

    @Value("${gwas-deposition.email-config.base-url}")
    private String submissionsBaseURL;

    public String getSubmissionsBaseURL() {
        return submissionsBaseURL;
    }

    public String getSubject() {
        return subject;
    }

    public String getSuccessEmail() {
        return successEmail;
    }

    public String getFailEmail() {
        return failEmail;
    }
}
