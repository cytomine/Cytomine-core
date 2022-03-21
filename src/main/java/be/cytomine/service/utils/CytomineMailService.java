package be.cytomine.service.utils;

import be.cytomine.config.ApplicationConfiguration;
import be.cytomine.config.NotificationConfiguration;
import be.cytomine.exceptions.MiddlewareException;
import be.cytomine.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.*;

@Slf4j
@Service
public class CytomineMailService {

    static final String NO_REPLY_EMAIL = "no-reply@cytomine.com";

    @Autowired
    JavaMailSender sender;

    @Autowired
    ApplicationConfiguration applicationConfiguration;


    public void send(String from, String[] to, String[] cc, String[] bcc, String subject, String message) throws MessagingException {
        send(from, to, cc, bcc, subject, message, new HashMap<>());
    }

    public void send(String from, String[] to, String[] cc, String[] bcc, String subject, String message, Map<String,Byte[]> attachment) throws MessagingException {
        NotificationConfiguration notificationConfiguration = applicationConfiguration.getNotification();
        String defaultEmail = notificationConfiguration.getEmail();

        if (StringUtils.isBlank(from)) {
            from = defaultEmail;
        }

        if (notificationConfiguration.getSmtpHost().equals("disabled")) {
            return;
        }

//        Properties props = new Properties();
//        props.put("mail.smtp.host",notificationConfiguration.getSmtpHost());
//        props.put("mail.smtp.port",notificationConfiguration.getSmtpHost());
//        props.put("mail.transport.protocol", notificationConfiguration.getSmtpProtocol());
//        props.put("mail.smtp.starttls.enable",notificationConfiguration.getSmtpStarttlsEnable());
//        props.put("mail.smtp.starttls.required",notificationConfiguration.getSmtpStarttlsRequired());
//        props.put("mail.smtp.ssl.protocols","TLSv1.2");
//        props.put("mail.debug",notificationConfiguration.getSmtpDebug());

//        String password = notificationConfiguration.getPassword();
//        if(password!=null && !password.isEmpty()) {
//            props.put("mail.smtp.auth", "true" );
//        } else {
//            props.put("mail.smtp.auth", "false" );
//        }

        //Create Mail Sender


        //TODO: configure this un a BEAN (JavaMailSender)
//        sender.setJavaMailProperties(props);
//        sender.setUsername(defaultEmail);
//        sender.setPassword(password);
//        sender.setDefaultEncoding("UTF-8");



        MimeMessage mail = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mail, true);

        helper.setReplyTo(applicationConfiguration.getAdminEmail());
        helper.setFrom(from);
        helper.setTo(to);
        helper.setCc(cc);
        helper.setBcc(bcc);
        helper.setSubject(subject);
        helper.setText("",message);

        for (Map.Entry<String, Byte[]> entry : attachment.entrySet()) {
            helper.addInline(entry.getKey(), new ByteArrayResource(ArrayUtils.toPrimitive(entry.getValue())));
        }


//        for (Byte[] bytes : attachment) {
//            helper.addInline(UUID.randomUUID().toString(), new ByteArrayResource(bytes)); // id?
//        }

        log.debug("Sending email...");
        log.debug("from " + from);
        log.debug("to " + Arrays.toString(to));
        log.debug("cc " + Arrays.toString(cc));
        log.debug("bcc " + Arrays.toString(bcc));
        log.debug(message);
        try {
            sender.send(mail);
        } catch (Exception e) {
            log.error("can't send email "+mail+" (MessagingException)");
            throw new MiddlewareException(e.getMessage());
        }
    }
}
