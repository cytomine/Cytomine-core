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
import be.cytomine.domain.security.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import java.io.File;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationService {

    CytomineMailService cytomineMailService;

    SpringTemplateEngine templateEngine;

    ApplicationProperties applicationProperties;

    public void notifyShareAnnotationMessage (
            User sender,
            List<String> receiversEmail,
            String subject,
            String from,
            String comment,
            String annotationURL,
            String shareAnnotationURL,
            Map<String, File> attachments,
            String cid) throws MessagingException {
        String sharedMessage = buildNotifyShareAnnotationMessage(from, comment, annotationURL, shareAnnotationURL, cid);
        cytomineMailService.send(CytomineMailService.NO_REPLY_EMAIL, new String[] {sender.getEmail()},  new String[] {}, receiversEmail.toArray(new String[receiversEmail.size()]), subject, sharedMessage, attachments);
    }

    public String buildNotifyShareAnnotationMessage (
            String from,
            String comment,
            String annotationURL,
            String shareAnnotationURL,
            String cid) {

        Context context = new Context();
        context.setVariable("from", from);
        context.setVariable("comment", comment);
        context.setVariable("annotationURL", annotationURL);
        context.setVariable("shareAnnotationURL", shareAnnotationURL);
        context.setVariable("cid", cid);
        context.setVariable("by", applicationProperties.getServerURL());
        context.setVariable("website", applicationProperties.getInstanceHostWebsite());
        context.setVariable("mailFrom", applicationProperties.getInstanceHostSupportMail());
        context.setVariable("phoneNumber", applicationProperties.getInstanceHostPhoneNumber());

        return templateEngine.process("mail/share.html", context);
    }
}
