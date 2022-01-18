package be.cytomine.ontology

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

import be.cytomine.AnnotationDomain
import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.MiddlewareException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.*
import be.cytomine.security.ForgotPasswordToken
import be.cytomine.security.SecRole
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON
import grails.util.Environment

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import static org.springframework.security.acls.domain.BasePermission.*

class SharedAnnotationService extends ModelService {

    static transactional = true
    def securityACLService
    def springSecurityService
    def secRoleService
    def secUserSecRoleService
    def abstractImageService
    def imageServerService


    // Avoid loading loop because secUserService -> userAnnotationService -> shareAnnotationService
    //def secUserService
    private getSecUserService(){
        grailsApplication.mainContext.secUserService
    }
    // notificationService -> secUserService
    //def notificationService
    private getNotificationService(){
        grailsApplication.mainContext.notificationService
    }
    // abstractImageService -> imageInstanceService -> userAnnotationService
    private getAbstractImageService(){
        grailsApplication.mainContext.abstractImageService
    }

    def currentDomain() {
        return SharedAnnotation
    }

    def list() {
        securityACLService.checkAdmin(cytomineService.currentUser)
        return SharedAnnotation.list()
    }

    SharedAnnotation read(def id) {
        def sharedAnnot = SharedAnnotation.read(id)
        if (sharedAnnot) {
            securityACLService.check(sharedAnnot.container(),READ)
        }
        sharedAnnot
    }

    SharedAnnotation get(def id) {
        def sharedAnnot= SharedAnnotation.get(id)
        if (sharedAnnot) {
            securityACLService.check(sharedAnnot.container(),READ)
        }
        sharedAnnot
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json, AnnotationDomain annotation, def params) throws CytomineException {
        User sender = User.read(springSecurityService.currentUser.id)
        securityACLService.checkUser(sender)


        if (!json.sender) {
            json.sender = springSecurityService.currentUser.id
        }

        json.annotationIdent = annotation.id
        json.annotationClassName = annotation.getClass().getName()

        String cid = UUID.randomUUID().toString()

        //create annotation crop (will be send with comment)
        File annotationCrop = null
        try {
            params.format = "png"
            params.alphaMask = true
            params.geometry = annotation.location
            params.complete = true
            params.maxSize = 512
            BufferedImage bufferedImage
            try {
                InputStream is = new ByteArrayInputStream(imageServerService.crop(annotation, params));
                bufferedImage = ImageIO.read(is)
            } catch(Exception e) {
                bufferedImage = null
            }

            log.info "Image " + bufferedImage

            if (bufferedImage != null) {
                annotationCrop = File.createTempFile("temp", ".${params.format}")
                annotationCrop.deleteOnExit()
                ImageIO.write(bufferedImage, params.format as String, annotationCrop)
            }
        } catch (FileNotFoundException e) {
            annotationCrop = null
        }

        def attachments = []
        if (annotationCrop != null) {
            attachments << [cid: cid, file: annotationCrop]
        }

        //do receivers email list
        String[] receiversEmail
        List<User> receivers = [];

        if (json.receivers) {
            receivers = JSONUtils.getJSONList(json.receivers).collect { userID ->
                User.read(userID)
            }
            receiversEmail = receivers.collect { it.getEmail() }
        } else if (json.emails) {
            receiversEmail = json.emails.split(",")
            receiversEmail.each { email ->
                if (!secUserService.findByEmail(email)) {
                    def guestUser = [username : email, firstname : 'firstname',
                                     lastname : 'lastname', email : email,
                                     password : 'passwordExpired', color : "#FF0000"]
                    secUserService.add(JSON.parse(JSONUtils.toJSONString(guestUser)))
                    User user = (User) secUserService.findByUsername(guestUser.username)
                    SecRole secRole = secRoleService.findByAuthority("ROLE_GUEST")
                    secUserSecRoleService.add(JSON.parse(JSONUtils.toJSONString([ user : user.id, role : secRole.id])))
                    secUserService.addUserToProject(user, annotation.getProject(), false)

                    if (user) {
                        user.passwordExpired = true
                        user.save()
                        ForgotPasswordToken forgotPasswordToken = new ForgotPasswordToken(
                                user : user,
                                tokenKey: UUID.randomUUID().toString(),
                                expiryDate: new Date() + 1
                        ).save()
                        notificationService.notifyWelcome(sender, user, forgotPasswordToken)
                    } else {
                        throw new ObjectNotFoundException("User with username "+guestUser.username+" not found")
                    }
                }
                receivers << secUserService.findByEmail(email)
            }

            json.receivers = receivers.collect{it.id}
        }

        securityACLService.checkFullOrRestrictedForOwner(annotation, annotation.user)
        def result =  executeCommand(new AddCommand(user: sender), null,json)

        if (result) {
            log.info "send mail to " + receiversEmail
            try {
                notificationService.notifyShareAnnotation(sender, receiversEmail, json, attachments, cid)
            } catch (MiddlewareException e) {
                if(Environment.getCurrent() == Environment.DEVELOPMENT){
                    e.printStackTrace()
                } else {
                    throw e
                }
            }
        }

        return result
    }


    def listComments(AnnotationDomain annotation) {
        User user = User.read(springSecurityService.currentUser.id)
        def sharedAnnotations = SharedAnnotation.createCriteria().list {
            eq("annotationIdent", annotation.id)
            eq("annotationClassName", annotation.class.name)
            or {
                eq("sender", user)
                receivers {
                    eq("id", user.id)
                }
            }
            order("created", "desc")
        }
        return sharedAnnotations.unique()
    }

    def delete(SharedAnnotation domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkFullOrRestrictedForOwner(domain,domain.sender)
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.sender.id, domain.annotationIdent, domain.annotationClassName]
    }

}

