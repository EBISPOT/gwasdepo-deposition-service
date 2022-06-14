package uk.ac.ebi.spot.gwas.deposition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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


    @Value("${gwas-deposition.email-config.docs-url}")
    private String submissionsDocsURL;

    @Value("${gwas-deposition.email-config.errors.subject}")
    private String errorsSubject;

    @Value("${gwas-deposition.email-config.errors.email}")
    private String errorsEmail;

    @Value("${gwas-deposition.email-config.errors.receiver}")
    private String errorsReceiver;

    @Value("${gwas-deposition.email-config.errors.active}")
    private boolean errorsActive;

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

    public boolean isErrorsActive() {
        return errorsActive;
    }

    public String getErrorsEmail() {
        return errorsEmail;
    }

    public String getErrorsSubject() {
        return errorsSubject;
    }

    public String getSubmissionsDocsURL() {
        return submissionsDocsURL;
    }

    public List<String> getErrorsReceiver() {
        List<String> result = new ArrayList<>();
        String[] parts = errorsReceiver.split(",");
        for (String part : parts) {
            result.add(part.trim());
        }
        return result;
    }
}
