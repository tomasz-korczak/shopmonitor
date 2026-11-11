package smon.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import smon.dto.ShopItemDto;
import smon.model.Shop;

import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Delivery per recipient: one failing recipient does not stop the others (H1 of the review). */
class MailServiceTest {

    private JavaMailSender sender;
    private MailService service;

    @BeforeEach
    void setUp() throws Exception {
        sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        service = new MailService();
        ReflectionTestUtils.setField(service, "emailSender", sender);
        ReflectionTestUtils.setField(service, "username", "raspberry@example.com");
        ReflectionTestUtils.setField(service, "to", new String[]{"ok@example.com", "bad@example.com"});
        ReflectionTestUtils.setField(service, "subject", "shopmonitor report");
        ReflectionTestUtils.setField(service, "displayName", "raspberry");
        ReflectionTestUtils.setField(service, "unsubscribeTo", "");
        ReflectionTestUtils.setField(service, "copyright", "");
    }

    private static Map<Shop, List<ShopItemDto>> items() {
        Shop shop = new Shop();
        shop.setName("alfa");
        shop.setUrl("https://alfa.pl/");
        ShopItemDto item = new ShopItemDto();
        item.setDescription("Mauser");
        item.setLink("https://alfa.pl/1");
        return Collections.singletonMap(shop, Collections.singletonList(item));
    }

    /** Refuses the message to one recipient, as an SMTP server would. */
    private void refuseRecipient(String address) throws Exception {
        doAnswer(invocation -> {
            MimeMessage message = invocation.getArgument(0);
            if (address.equals(message.getAllRecipients()[0].toString())) {
                throw new MailSendException("recipient refused");
            }
            return null;
        }).when(sender).send(any(MimeMessage.class));
    }

    @Test
    void oneRecipientFailingDoesNotStopTheOthers() throws Exception {
        refuseRecipient("bad@example.com");

        assertThat(service.sendEmail(MailKind.SCAN, items())).isTrue();
        verify(sender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void noRecipientReceivingTheMailIsAFailure() throws Exception {
        doThrow(new MailSendException("server down")).when(sender).send(any(MimeMessage.class));

        assertThat(service.sendEmail(MailKind.SCAN, items())).isFalse();
    }

    @Test
    void mailBodyIsHtml() throws Exception {
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);

        assertThat(service.sendEmail(MailKind.SCAN, items())).isTrue();
        verify(sender, atLeastOnce()).send(sent.capture());
        MimeMessage message = sent.getValue();
        message.saveChanges(); // a real send saves the headers; the mock sender does not, so do it here
        Optional<String> html = htmlPart(message);
        assertThat(html).isPresent();
        assertThat(html.get()).contains("Mauser", "https://alfa.pl/1");
    }

    /** The text/html part of the message, searching nested multiparts. */
    private static Optional<String> htmlPart(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return Optional.of((String) part.getContent());
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                Optional<String> html = htmlPart(multipart.getBodyPart(i));
                if (html.isPresent()) {
                    return html;
                }
            }
        }
        return Optional.empty();
    }
}
