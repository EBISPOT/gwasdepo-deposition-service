package uk.ac.ebi.spot.gwas.deposition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BackendSubmissionChecksConfig {

    @Value("${gwas-deposition.stalled-submissions.first-check:10}")
    private int firstCheck;

    @Value("${gwas-deposition.stalled-submissions.second-check:7}")
    private int secondCheck;

    @Value("${gwas-deposition.stalled-submissions.last-check:13}")
    private int lastCheck;

    @Value("${gwas-deposition.stalled-submissions.first-email}")
    private String firstEmail;

    @Value("${gwas-deposition.stalled-submissions.second-email}")
    private String secondEmail;

    @Value("${gwas-deposition.stalled-submissions.last-email}")
    private String lastEmail;

    public int getFirstCheck() {
        return firstCheck;
    }

    public int getSecondCheck() {
        return secondCheck;
    }

    public int getLastCheck() {
        return lastCheck;
    }

    public String getFirstEmail() {
        return firstEmail;
    }

    public String getSecondEmail() {
        return secondEmail;
    }

    public String getLastEmail() {
        return lastEmail;
    }
}
