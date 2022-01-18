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

import be.cytomine.api.UrlApi
import be.cytomine.image.AbstractImage
import be.cytomine.image.ImageInstance
import be.cytomine.security.ForgotPasswordToken
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.security.UserJob

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class NotificationService {

    def grailsApplication
    def cytomineMailService
    def secUserService
    def renderService
    def abstractImageService
    def imageServerService

    public def notifyNewImageAvailable(SecUser currentUser, AbstractImage abstractImage, def projects) {
        User recipient = null
        if (currentUser instanceof User) {
            recipient = (User) currentUser
        } else if (currentUser instanceof UserJob) {
            UserJob userJob = (UserJob) currentUser
            recipient = userJob.getUser()
        }

        // send email to uploader + all project manager
        def users = [recipient]
        projects.each {
            users.addAll(secUserService.listAdmins(it))
        }
        users.unique()

        log.info "Send mail to $users"

        String macroCID = null

        def attachments = []

        String thumbURL = UrlApi.getAbstractImageThumbUrlWithMaxSize(abstractImage.id, 256)
        if (thumbURL) {
            macroCID = UUID.randomUUID().toString()
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageServerService.thumb(abstractImage, [maxSize: 256])))
            if (bufferedImage != null) {
                File macroFile = File.createTempFile("temp", ".jpg")
                macroFile.deleteOnExit()
                ImageIO.write(bufferedImage, "JPG", macroFile)
                attachments << [ cid : macroCID, file : macroFile]
            }
        }

        def imagesInstances = []
        for (imageInstance in ImageInstance.findAllByBaseImage(abstractImage)) {
            String urlImageInstance = UrlApi.getBrowseImageInstanceURL(imageInstance.getProject().id, imageInstance.getId())
            imagesInstances << [urlImageInstance : urlImageInstance, projectName : imageInstance.project.getName()]

        }
        String message = renderService.createNewImagesAvailableMessage([
                abstractImageFilename : abstractImage.getOriginalFilename(),
                cid : macroCID,
                imagesInstances : imagesInstances,
                by: grailsApplication.config.grails.serverURL,
                website: grailsApplication.config.grails.instanceHostWebsite,
                mailFrom: grailsApplication.config.grails.instanceHostSupportMail,
                phoneNumber: grailsApplication.config.grails.instanceHostPhoneNumber
        ])

        cytomineMailService.send(
                null,
                (String[]) users.collect{it.getEmail()},
                null,
                null,
                "Cytomine : a new image is available",
                message.toString(),
                attachments)

    }

    def notifyWelcome(User sender, User guestUser, ForgotPasswordToken forgotPasswordToken) {
        String welcomeMessage = renderService.createWelcomeMessage([
                senderFirstname : sender.getFirstname(),
                senderLastname : sender.getLastname(),
                senderEmail : sender.getEmail(),
                username : guestUser.getUsername(),
                tokenKey : forgotPasswordToken.getTokenKey(),
                expiryDate : forgotPasswordToken.getExpiryDate(),
                by: grailsApplication.config.grails.serverURL,
                website: grailsApplication.config.grails.instanceHostWebsite,
                mailFrom: grailsApplication.config.grails.instanceHostSupportMail,
                phoneNumber: grailsApplication.config.grails.instanceHostPhoneNumber
        ])
        String mailTitle = sender.getFirstname() + " " + sender.getLastname() + " invited you to join Cytomine"
        cytomineMailService.send(
                null,
                (String[]) [guestUser.getEmail()],
                null,
                null,
                mailTitle,
                welcomeMessage)
    }

    def notifyShareAnnotation(User sender, def receiversEmail, def request, def attachments, def cid) {
        String subject = request.subject
        String shareMessage = renderService.createShareMessage([
                from: request.from,
                comment: request.comment,
                annotationURL: request.annotationURL,
                shareAnnotationURL : request.shareAnnotationURL,
                cid : cid,
                by: grailsApplication.config.grails.serverURL,
                website: grailsApplication.config.grails.instanceHostWebsite,
                mailFrom: grailsApplication.config.grails.instanceHostSupportMail,
                phoneNumber: grailsApplication.config.grails.instanceHostPhoneNumber
        ])

        cytomineMailService.send(
                cytomineMailService.NO_REPLY_EMAIL,
                [sender.getEmail()] as String[],
                null,
                receiversEmail,
                subject,
                shareMessage,
                attachments)
    }

    def notifyForgotUsername(User user) {
        String message = renderService.createForgotUsernameMessage([
                username : user.getUsername(),
                by: grailsApplication.config.grails.serverURL,
                website: grailsApplication.config.grails.instanceHostWebsite,
                mailFrom: grailsApplication.config.grails.instanceHostSupportMail,
                phoneNumber: grailsApplication.config.grails.instanceHostPhoneNumber
        ])
        cytomineMailService.send(
                cytomineMailService.NO_REPLY_EMAIL,
                (String[]) [user.getEmail()],
                null,
                null,
                "Cytomine : your username is $user.username",
                message)
    }

    def notifyForgotPassword(User user, ForgotPasswordToken forgotPasswordToken) {
        String message = renderService.createForgotPasswordMessage([
                username : user.getUsername(),
                tokenKey : forgotPasswordToken.getTokenKey(),
                expiryDate : forgotPasswordToken.getExpiryDate(),
                by: grailsApplication.config.grails.serverURL,
                website: grailsApplication.config.grails.instanceHostWebsite,
                mailFrom: grailsApplication.config.grails.instanceHostSupportMail,
                phoneNumber: grailsApplication.config.grails.instanceHostPhoneNumber
        ])

        cytomineMailService.send(
                cytomineMailService.NO_REPLY_EMAIL,
                (String[]) [user.getEmail()],
                null,
                null,
                "Cytomine : reset your password",
                message)
    }
}
