package smon.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import smon.dto.ShopItemDto;
import smon.model.Shop;

import javax.mail.MessagingException;
import javax.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
public class MailService {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${email.sendTo}")
    private String[] to;

    @Value("${email.subject}")
    private String subject;

    @Value("${email.display-name}")
    private String displayName;

    @Value("${email.footer.unsubscribe-to:}")
    private String unsubscribeTo;

    @Value("${email.footer.copyright:}")
    private String copyright;

    /**
     * Sends the report to every recipient separately; a failure for one recipient does not stop the others.
     *
     * @return true when the email reached at least one recipient
     */
    public boolean sendEmail(MailKind mailKind, Map<Shop, List<ShopItemDto>> items, String... params) {
        MailUtil util = new MailUtil(items, subject, unsubscribeTo, copyright);
        String mailSubject = util.formatSubject();
        String message = util.formatHTMLMessage(mailKind, params);
        int sent = 0;
        for (String recipient : to) {
            try {
                log.info("Sending email to {}", recipient);
                sendMimeMessage(message, mailSubject, recipient);
                sent++;
            } catch (Exception e) {
                log.error("Sending email to {} failed: {}", recipient, e.toString());
                log.debug("Sending email failed", e);
            }
        }
        log.info("Email sent to {} of {} recipients", sent, to.length);
        return sent > 0;
    }

    /**
     * multipart=true stays as it was: the mail has always been sent this way, and a single-part variant was not
     * checked against a real send. The message has no inline parts any more.
     */
    private void sendMimeMessage(String text, String subject, String to) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(new InternetAddress(username, displayName));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, true);
        emailSender.send(message);
    }

}
