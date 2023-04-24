package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.config.BackendMailConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.MailConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.messaging.email.EmailBuilder;
import uk.ac.ebi.spot.gwas.deposition.messaging.email.EmailService;
import uk.ac.ebi.spot.gwas.deposition.service.BackendEmailService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;
import uk.ac.ebi.spot.gwas.deposition.service.impl.email.ErrorsEmailBuilder;
import uk.ac.ebi.spot.gwas.deposition.service.impl.email.FailEmailBuilder;
import uk.ac.ebi.spot.gwas.deposition.service.impl.email.SuccessEmailBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackendEmailServiceImpl implements BackendEmailService {

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired
    private BackendMailConfig backendMailConfig;

    @Autowired
    private UserService userService;

    public void sendSuccessEmail(String userId, String pubmedId, Map<String, Object> metadata) {
        User user = userService.getUser(userId);
        metadata.put(MailConstants.USER_NAME, user.getName());

        if (emailService != null) {
            EmailBuilder successBuilder = new SuccessEmailBuilder(backendMailConfig.getSuccessEmail());
            emailService.sendMessage(user.getEmail(), getSubject(pubmedId), successBuilder.getEmailContent(metadata), false);
        }
    }

    public void sendFailEmail(String userId, String pubmedId, Map<String, Object> metadata, List<String> errors) {
        User user = userService.getUser(userId);
        metadata.put(MailConstants.USER_NAME, user.getName());

        if (emailService != null) {
            EmailBuilder failBuilder = new FailEmailBuilder(backendMailConfig.getFailEmail(), errors);
            emailService.sendMessage(user.getEmail(), getSubject(pubmedId), failBuilder.getEmailContent(metadata), false);
        }
    }

    public void sendErrorsEmail(String location, String error) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(MailConstants.ERROR_LOCATION, location);
        metadata.put(MailConstants.ERROR_CONTENT, error);

        if (emailService != null && backendMailConfig.isErrorsActive()) {
            EmailBuilder errorsBuilder = new ErrorsEmailBuilder(backendMailConfig.getErrorsEmail());
            for (String receiver : backendMailConfig.getErrorsReceiver()) {
                emailService.sendMessage(receiver, backendMailConfig.getErrorsSubject(), errorsBuilder.getEmailContent(metadata), false);
            }
        }
    }

    public void sendReminderEmail(String userId, Map<String, Object> metadata, String emailFile) {
        User user = userService.getUser(userId);
        metadata.put(MailConstants.USER_NAME, user.getName());
        metadata.put(MailConstants.SUBMISSION_ID, backendMailConfig.getSubmissionsBaseURL() + metadata.get(MailConstants.SUBMISSION_ID));
        if (emailService != null) {
            EmailBuilder successBuilder = new SuccessEmailBuilder(emailFile);
            emailService.sendMessage(user.getEmail(), "GWAS Catalog submission (" + metadata.get(MailConstants.SUBMISSION_ID) + ")", successBuilder.getEmailContent(metadata), false);
        }
    }

    public void sendGlobusFolderEmail(String userId, Map<String, Object> metadata, String emailFile, String globusId, String globusIdentity) {
        User user = userService.getUser(userId);
        metadata.put(MailConstants.USER_NAME, user.getName());
        metadata.put(MailConstants.GLOBUS_ID, globusId);
        metadata.put(MailConstants.GLOBUS_IDENTITY, globusIdentity);
        if (emailService != null) {
            EmailBuilder successBuilder = new SuccessEmailBuilder(emailFile);
            emailService.sendMessage("gwas-subs@ebi.ac.uk", "GWAS Catalog submission (" + metadata.get(MailConstants.SUBMISSION_ID) + ") needs Globus folder creation", successBuilder.getEmailContent(metadata), false);
        }
    }

    private String getSubject(String pubmedId) {
        String subject = backendMailConfig.getSubject();
        if (subject.contains("%PMID%")) {
            subject = subject.replace("%PMID%", pubmedId);
        }
        return subject;
    }
}
