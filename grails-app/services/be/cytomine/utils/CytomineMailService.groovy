package be.cytomine.utils

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

import be.cytomine.Exception.MiddlewareException
import grails.util.Holders
import org.springframework.core.io.FileSystemResource
import org.springframework.mail.MailAuthenticationException
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper

import javax.mail.MessagingException
import javax.mail.AuthenticationFailedException
import javax.mail.internet.MimeMessage


class CytomineMailService {

    static final String NO_REPLY_EMAIL = "no-reply@cytomine.com"

    static transactional = false


    def send(String from, String[] to, String[] cc, String[] bcc, String subject, String message, def attachment = null) {

        String defaultEmail = Holders.getGrailsApplication().config.grails.notification.email

        if (!from) from = defaultEmail

        def smtpProperties = Holders.getGrailsApplication().config.grails.notification.smtp
        if (smtpProperties && smtpProperties.host == 'disabled') {
            return
        }

        Properties props = new Properties();
        props.put("mail.smtp.host",smtpProperties.host);
        props.put("mail.smtp.port",smtpProperties.port);
        props.put("mail.transport.protocol", smtpProperties.protocol);
        props.put("mail.smtp.starttls.enable",smtpProperties.starttls.enable);
        props.put("mail.smtp.starttls.required",smtpProperties.starttls.required);
        props.put("mail.smtp.ssl.protocols","TLSv1.2");
        props.put("mail.debug",smtpProperties.debug);

        String password = Holders.getGrailsApplication().config.grails.notification.password
        if(password && !password.isEmpty()) props.put("mail.smtp.auth", "true" );
        else props.put("mail.smtp.auth", "false" );

        //Create Mail Sender
        def sender = new JavaMailSenderImpl()
        sender.setJavaMailProperties(props)
        sender.setUsername(defaultEmail)
        sender.setPassword(password)
        sender.setDefaultEncoding("UTF-8")
        MimeMessage mail = sender.createMimeMessage()
        MimeMessageHelper helper = new MimeMessageHelper(mail, true)

        helper.setReplyTo(Holders.getGrailsApplication().config.grails.admin.email.toString())
        helper.setFrom(from)
        helper.setTo(to)
        //helper.setCc(cc)
        helper.setBcc(bcc)
        helper.setSubject(subject)
        helper.setText("",message)
        attachment?.each {
            helper.addInline((String) it.cid, new FileSystemResource((File)it.file))
        }

        log.info "send $mail"
        try {
            sender.send(mail)
        } catch (AuthenticationFailedException | MessagingException | MailAuthenticationException e) {
            log.error "can't send email $mail (MessagingException)"
            e.printStackTrace()
            throw new MiddlewareException(e.getMessage())
        }
    }
}
