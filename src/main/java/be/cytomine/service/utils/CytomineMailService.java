package be.cytomine.service.utils;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.config.properties.NotificationProperties;
import be.cytomine.exceptions.MiddlewareException;
import be.cytomine.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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
    ApplicationProperties applicationProperties;

    @Value("${spring.mail.host}")
    public String smtpHost;


    public void send(String from, String[] to, String[] cc, String[] bcc, String subject, String message) throws MessagingException {
        send(from, to, cc, bcc, subject, message, new HashMap<>());
    }

    public void send(String from, String[] to, String[] cc, String[] bcc, String subject, String message, Map<String,File> attachment) throws MessagingException {
        NotificationProperties notificationConfiguration = applicationProperties.getNotification();
        String defaultEmail = notificationConfiguration.getEmail();

        if (StringUtils.isBlank(from)) {
            from = defaultEmail;
        }

        if (smtpHost.equals("disabled")) {
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

        helper.setReplyTo(applicationProperties.getAdminEmail());
        helper.setFrom(from);
        helper.setTo(to);
        helper.setCc(cc);
        helper.setBcc(bcc);
        helper.setSubject(subject);
        helper.setText("",message);

        for (Map.Entry<String, File> entry : attachment.entrySet()) {
            helper.addInline(entry.getKey(), entry.getValue());
        }


//        for (Byte[] bytes : attachment) {
//            helper.addInline(UUID.randomUUID().toString(), new ByteArrayResource(bytes)); // id?
//        }

        log.debug("Sending email...");
        log.debug("from " + from);
        log.debug("to " + Arrays.toString(to));
        log.debug("cc " + Arrays.toString(cc));
        log.debug("bcc " + Arrays.toString(bcc));
        try {
            sender.send(mail);
        } catch (Exception e) {
            log.error("can't send email "+mail+" (MessagingException)");
            throw new MiddlewareException(e.getMessage());
        }
    }
}
