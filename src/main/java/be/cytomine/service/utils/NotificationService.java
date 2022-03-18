package be.cytomine.service.utils;

import be.cytomine.config.ApplicationConfiguration;
import be.cytomine.domain.security.ForgotPasswordToken;
import be.cytomine.domain.security.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.MessagingException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationService {

    CytomineMailService cytomineMailService;

    SpringTemplateEngine templateEngine;

    ApplicationConfiguration applicationConfiguration;

    public void notifyWelcome(User sender, User guestUser, ForgotPasswordToken forgotPasswordToken) throws MessagingException {
        String mailTitle = sender.getFirstname() + " " + sender.getLastname() + " invited you to join Cytomine";
        String welcomeMessage = buildNotifyWelcomeMessage(sender,forgotPasswordToken);
        cytomineMailService.send(null, new String[] {guestUser.getEmail()},  new String[] {},  new String[] {}, mailTitle, welcomeMessage);
    }

    public String buildNotifyWelcomeMessage(User sender, ForgotPasswordToken forgotPasswordToken) {

        Context context = new Context();
        context.setVariable("senderFirstname", sender.getFirstname());
        context.setVariable("senderLastname", sender.getLastname());
        context.setVariable("senderEmail", sender.getEmail());
        context.setVariable("username", sender.getUsername());
        context.setVariable("tokenKey", forgotPasswordToken.getTokenKey());
        context.setVariable("expiryDate", forgotPasswordToken.getExpiryDate());
        context.setVariable("by", applicationConfiguration.getServerURL());
        context.setVariable("website", applicationConfiguration.getInstanceHostWebsite());
        context.setVariable("mailFrom", applicationConfiguration.getInstanceHostSupportMail());
        context.setVariable("phoneNumber", applicationConfiguration.getInstanceHostPhoneNumber());

        return templateEngine.process("mail/welcome.html", context);
    }

    public void notifyShareAnnotationMessage (
            User sender,
            List<String> receiversEmail,
            String subject,
            String from,
            String comment,
            String annotationURL,
            String shareAnnotationURL,
            Map<String, Byte[]> attachments,
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
        context.setVariable("by", applicationConfiguration.getServerURL());
        context.setVariable("website", applicationConfiguration.getInstanceHostWebsite());
        context.setVariable("mailFrom", applicationConfiguration.getInstanceHostSupportMail());
        context.setVariable("phoneNumber", applicationConfiguration.getInstanceHostPhoneNumber());

        return templateEngine.process("mail/share.html", context);
    }


    public void notifyForgotUsername (User user) throws MessagingException {
        String message = buildNotifyForgotUsername(user.getUsername());
        String subject = "Cytomine : your username is " + user.getUsername();
        cytomineMailService.send(CytomineMailService.NO_REPLY_EMAIL, new String[] {user.getEmail()},  new String[] {}, new String[]{}, subject, message);
    }

    public String buildNotifyForgotUsername(String username) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("by", applicationConfiguration.getServerURL());
        context.setVariable("website", applicationConfiguration.getInstanceHostWebsite());
        context.setVariable("mailFrom", applicationConfiguration.getInstanceHostSupportMail());
        context.setVariable("phoneNumber", applicationConfiguration.getInstanceHostPhoneNumber());

        return templateEngine.process("mail/forgot_username.html", context);
    }

    public void notifyForgotPassword (User user, ForgotPasswordToken forgotPasswordToken) throws MessagingException {
        String message = buildNotifyForgotPassword(user.getUsername(), forgotPasswordToken);
        String subject = "Cytomine : reset your password";
        cytomineMailService.send(CytomineMailService.NO_REPLY_EMAIL, new String[] {user.getEmail()},  new String[] {}, new String[]{}, subject, message);
    }

    public String buildNotifyForgotPassword(String username, ForgotPasswordToken forgotPasswordToken) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("tokenKey", forgotPasswordToken.getTokenKey());
        context.setVariable("expiryDate", forgotPasswordToken.getExpiryDate());
        context.setVariable("by", applicationConfiguration.getServerURL());
        context.setVariable("website", applicationConfiguration.getInstanceHostWebsite());
        context.setVariable("mailFrom", applicationConfiguration.getInstanceHostSupportMail());
        context.setVariable("phoneNumber", applicationConfiguration.getInstanceHostPhoneNumber());

        return templateEngine.process("mail/forgot_password.html", context);
    }
}
