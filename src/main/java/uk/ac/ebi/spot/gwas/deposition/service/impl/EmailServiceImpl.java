package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.config.MailConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.MailConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.service.EmailService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;
import uk.ac.ebi.spot.gwas.deposition.service.impl.email.EmailBuilder;
import uk.ac.ebi.spot.gwas.deposition.service.impl.email.FailEmailBuilder;
import uk.ac.ebi.spot.gwas.deposition.service.impl.email.SuccessEmailBuilder;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private MailConfig mailConfig;

    @Autowired
    private UserService userService;

    public void sendSuccessEmail(String userId, String pubmedId, Map<String, String> metadata) {
        User user = userService.getUser(userId);
        metadata.put(MailConstants.USER_NAME, user.getName());

        EmailBuilder successBuilder = new SuccessEmailBuilder(mailConfig.getSuccessEmail());
        this.sendMessage(user.getEmail(), successBuilder.getEmailContent(metadata), pubmedId);
    }

    public void sendFailEmail(String userId, String pubmedId, Map<String, String> metadata, List<String> errors) {
        User user = userService.getUser(userId);
        metadata.put(MailConstants.USER_NAME, user.getName());

        EmailBuilder failBuilder = new FailEmailBuilder(mailConfig.getFailEmail(), errors);
        this.sendMessage(user.getEmail(), failBuilder.getEmailContent(metadata), pubmedId);
    }

    @Override
    @Async
    public void sendMessage(String emailAddress, String content, String pubmedId) {
        if (!mailConfig.isEmailEnabled()) {
            log.info("Email sending is disabled.");
            return;
        }

        if (mailSender != null) {
            if (content == null) {
                log.error("Unable to send email. Content is null.");
                return;
            }
            int retryCount = mailConfig.getRetryCount();
            for (int i = 0; i < retryCount; i++) {
                try {
                    log.info("Building the email message to be sent");
                    MimeMessage message = buildMessage(emailAddress, content, pubmedId);
                    log.info("Preparing to send the email: {}", message);
                    mailSender.send(message);
                    log.info("Successfully sent the email message.");
                    break;
                } catch (Exception e) {
                    log.error("Exception received while trying to send out email: {}", e.getMessage(), e);
                }
            }
        } else {
            log.warn("Email sender configuration not present. Cannot send emails.");
        }
    }

    private MimeMessage buildMessage(String emailAddress,
                                     String content,
                                     String pubmedId) throws MessagingException, UnsupportedEncodingException {
        log.info("Building the MimeMessage to be sent.");
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        mimeMessage.setFrom(new InternetAddress(mailConfig.getFromAddress(), mailConfig.getFromName()));

        mimeMessage.setContent(content, String.format("%s;%s",
                doesStringContainHtml(content) ? "text/html" : "text/plain", "charset=utf-8"));
        mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress));

        String subject = mailConfig.getSubject();
        if (subject.contains("%PMID%")) {
            subject = subject.replace("%PMID%", pubmedId);
        }
        mimeMessage.setSubject(subject);
        return mimeMessage;
    }

    private static boolean doesStringContainHtml(final String content) {
        return content.replaceAll("\r\n|\n", "").matches(".*\\<[a-zA-Z]{1,}\\>.*\\</[a-zA-Z]{1,}\\>.*");

    }

}
