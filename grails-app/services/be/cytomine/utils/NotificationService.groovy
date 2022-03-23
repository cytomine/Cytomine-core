package be.cytomine.utils

import be.cytomine.AnnotationDomain

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
import be.cytomine.project.Project
import be.cytomine.project.ProjectRepresentativeUser
import be.cytomine.security.ForgotPasswordToken
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.security.UserJob

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat

class NotificationService {

    def grailsApplication
    def cytomineMailService
    def secUserService
    def renderService
    def abstractImageService
    def imageServerService

    def notifyProjectDelete(def receiversEmail, Project project) {

        String HV_instance_specific;

        switch(grailsApplication.config.cytomine.HV_instance.toString()) {
            case "DEVELOPMENT" :
                HV_instance_specific = "Test Utvikling"
                break
            case "QA_UNDERVISNING" :
                HV_instance_specific = "Test Undervisning"
                break
            case "UNDERVISNING" :
                HV_instance_specific = "Undervisning"
                break
            case "QA_LABO" :
                HV_instance_specific = "Test Laboratorienettverk"
                break
            case "LABO" :
                HV_instance_specific = "Laboratorienettverk"
                break
            case "QA_KOLLEGIAL" :
                HV_instance_specific = "Test Kollegialrådføring"
                break
            case "KOLLEGIAL" :
                HV_instance_specific = "Kollegialrådføring"
                break
            default:
                HV_instance_specific = ""
        }

        String message = renderService.createProjectDeleteWarning([
                projectId : project.id,
                projectName : project.name,
                toDeleteAt: new SimpleDateFormat("dd/MM/YYYY").format(project.toDeleteAt),
                hv_instance: HV_instance_specific,
                by: grailsApplication.config.grails.serverURL,
                website: grailsApplication.config.grails.instanceHostWebsite,
                mailFrom: grailsApplication.config.grails.instanceHostSupportMail,
                phoneNumber: grailsApplication.config.grails.instanceHostPhoneNumber
        ])
        String mailTitle = "Varsel om sletting av et prosjekt i Cytomine "
        mailTitle += HV_instance_specific

        println message

        cytomineMailService.send(
                null,
                receiversEmail as String[],
                null,
                new String[0],
                mailTitle,
                message)
    }

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

    def notifyShareAnnotation(User sender, def receiversEmail, def request, def attachments, def cid, String mode = "classic") {
        String subject = request.subject

        String instanceHostSupportMail = grailsApplication.config.grails.instanceHostSupportMail
        if(!instanceHostSupportMail) instanceHostSupportMail = grailsApplication.config.grails.admin.email

        String shareMessage = renderService.createShareMessage([
                from: request.from,
                comment: request.comment,
                annotationURL: request.annotationURL,
                shareAnnotationURL : request.shareAnnotationURL,
                cid : cid,
                by: grailsApplication.config.grails.serverURL,
                website: grailsApplication.config.grails.instanceHostWebsite,
                mailFrom: instanceHostSupportMail,
                phoneNumber: grailsApplication.config.grails.instanceHostPhoneNumber
        ], mode)

        /*AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(request.annotationIdent, request.annotationClassName)
        println (ProjectRepresentativeUser.findAllByProject(annotation.project)*.user.email)*/

        cytomineMailService.send(
                cytomineMailService.NO_REPLY_EMAIL,
                [sender.getEmail()] as String[],
                null,
                receiversEmail as String[],
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
