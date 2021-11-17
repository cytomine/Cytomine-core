package be.cytomine.test

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
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
import be.cytomine.CytomineDomain
import be.cytomine.meta.Property
import be.cytomine.image.*
import be.cytomine.image.acquisition.Instrument
import be.cytomine.image.multidim.ImageGroup
import be.cytomine.image.multidim.ImageGroupHDF5
import be.cytomine.image.multidim.ImageSequence
import be.cytomine.image.server.*
import be.cytomine.laboratory.Sample
import be.cytomine.meta.Tag
import be.cytomine.meta.TagDomainAssociation
import be.cytomine.middleware.AmqpQueue
import be.cytomine.middleware.AmqpQueueConfig
import be.cytomine.middleware.AmqpQueueConfigInstance
import be.cytomine.middleware.ImageServer
import be.cytomine.middleware.MessageBrokerServer
import be.cytomine.ontology.*
import be.cytomine.processing.*
import be.cytomine.project.Discipline
import be.cytomine.project.Project
import be.cytomine.project.ProjectDefaultLayer
import be.cytomine.project.ProjectRepresentativeUser
import be.cytomine.search.SearchEngineFilter
import be.cytomine.security.*
import be.cytomine.social.AnnotationAction
import be.cytomine.social.LastUserPosition
import be.cytomine.social.PersistentImageConsultation
import be.cytomine.social.PersistentProjectConnection
import be.cytomine.social.PersistentUserPosition
import be.cytomine.meta.AttachedFile
import be.cytomine.meta.Configuration
import be.cytomine.meta.Description
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApiObjectField
import org.springframework.dao.DataRetrievalFailureException

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 9/02/11
 * Time: 13:37
 * Build sample domain data
 */
class BasicInstanceBuilder {

    def springSecurityService

    def grailsApplication

    private static Log log = LogFactory.getLog(BasicInstanceBuilder.class)

    /**
     * Check if a domain is valid during test
     * @param domain Domain to check
     */
    static def checkDomain(def domain) {
        log.debug "check domain " + domain.id
        boolean validate = domain.validate()
        if(!validate) {
            log.debug domain.errors
        }
        assert validate
        domain
    }

    /**
     *  Check if a domain is well saved during test
     * @param domain Domain to check
     */
    static def saveDomain(def domain) {
        domain.getClass().withTransaction {
            domain = domain.save(flush: true, failOnError:true)
        }
        domain.refresh()
        domain
    }
    static def insertDomain(def domain) {
        domain.insert(flush: true, failOnError:true)
        domain
    }

    /**
     * Compare  expected data (in map) to  new data (json)
     * This method is used in update test method to check if data are well changed
     * @param map Expected data
     * @param json New Data
     */
    static void compare(map, json) {
        map.each {
            def propertyValue = it.value
            def compareValue = json[it.key]
            assert propertyValue.toString().equals(compareValue.toString())
        }
    }

    static boolean checkIfDomainExist(def domain, boolean exist=true) {
        log.info("checkIfDomainExist for "+ domain.class.name+ " " +domain.id)
        try {
            domain.refresh()
        } catch(DataRetrievalFailureException e){
            log.debug("refresh impossible. Maybe the resource has been previously deleted")
        }
        domain = domain.read(domain.id)
        boolean domainExist = domain && !domain.checkDeleted()
        assert domainExist == exist
        domainExist
    }

    static void checkIfDomainsExist(def domains) {
        domains.each {
            checkIfDomainExist(it,true)
        }
    }

    static void checkIfDomainsNotExist(def domains) {
        domains.each {
            checkIfDomainExist(it,false)
        }
    }

    static User getUser() {
        def user = SecUser.findByUsername("BasicUser")
        if (!user) {
            user = new User(
                    username: "BasicUser",
                    firstname: "Basic",
                    lastname: "User",
                    email: "Basic@User.be",
                    password: "password",
                    enabled: true,
                    origin: "TEST")
            user.generateKeys()
            saveDomain(user)
            SecUserSecRole.create(user,SecRole.findByAuthority("ROLE_USER"),true)
        }

        user
    }



    static User getUser1() {
       return getUser("user1","password")
    }


    static User getUser2() {
        return getUser("user2","password")
    }

    static UserJob getUserJob(Project project) {
        Job job = getJobNotExist()
        job.project = project
        saveDomain(job)
        getSoftwareProjectNotExist(job.software,job.project,true)
        UserJob userJob = getUserJobNotExist()
        userJob.job = job
        userJob.user = User.findByUsername(Infos.SUPERADMINLOGIN)
        saveDomain(userJob)
    }

    static UserJob getUserJob(String username, String password) {
        UserJob user = UserJob.findByUsername(username)
        if (!user) {
            user = new UserJob(username: username, user:User.findByUsername(Infos.SUPERADMINLOGIN),password: password,enabled: true,job: getJob(), origin: "TEST")
            user.generateKeys()
            saveDomain(user)
            SecUserSecRole.findAllBySecUser(User.findByUsername(Infos.SUPERADMINLOGIN)).collect { it.secRole }.each { secRole ->
                SecUserSecRole.create(userJob, secRole)
            }
        }
        user
    }


    static UserJob getUserJob() {
        UserJob userJob = UserJob.findByUsername("BasicUserJob")
        if (!userJob) {
            userJob = new UserJob(username: "BasicUserJob",password: "PasswordUserJob",enabled: true,user : User.findByUsername(Infos.SUPERADMINLOGIN),job: getJob(), origin: "TEST")
            userJob.generateKeys()
            saveDomain(userJob)
            SecUserSecRole.findAllBySecUser(User.findByUsername(Infos.SUPERADMINLOGIN)).collect { it.secRole }.each { secRole ->
                SecUserSecRole.create(userJob, secRole)
            }
        }
        userJob
    }

    static String getRandomString() {
        def random = new Random()
        new Date().time.toString() + random.nextInt()
    }

    static getRandomInteger(int rangeMin, int rangeMax) {
        def random = new Random()
        Integer randInt = random.nextInt((rangeMax - rangeMin) + 1) + rangeMin
        randInt
    }

    static UserJob getUserJobNotExist(boolean save = false) {
        getUserJobNotExist(User.findByUsername(Infos.SUPERADMINLOGIN), save)
    }

    static UserJob getUserJobNotExist(Job job, boolean save = false) {
        getUserJobNotExist(job, User.findByUsername(Infos.SUPERADMINLOGIN), save)
    }

    static UserJob getUserJobNotExist(User user, boolean save = false) {
        Job job = getJobNotExist(true)
        Infos.addUserRight(user, job.project)
        getUserJobNotExist(job, user, save)
    }

    static UserJob getUserJobNotExist(Job job, User user, boolean save = false) {
        UserJob userJob = new UserJob(username:getRandomString(),password: "PasswordUserJob",enabled: true,user : user,job: job, origin: "TEST")
        userJob.generateKeys()

        if(save) {
            saveDomain(userJob)
            SecUserSecRole.findAllBySecUser(userJob.user).collect { it.secRole }.each { secRole ->
                SecUserSecRole.create(user, secRole)
            }
        } else {
            checkDomain(userJob)
        }
        userJob
    }

    static User getAdmin(String username, String password) {
        User user = getUser(username,password)
        SecUserSecRole.create(user,SecRole.findByAuthority("ROLE_ADMIN"))
        user
    }
    static User getSuperAdmin(String username, String password) {
        User user = getUser(username,password)
        SecUserSecRole.create(user,SecRole.findByAuthority("ROLE_ADMIN"))
        SecUserSecRole.create(user,SecRole.findByAuthority("ROLE_SUPER_ADMIN"))
        user
    }

    static SliceInstance getSliceInstance() {
        getSliceInstanceNotExist(BasicInstanceBuilder.getImageInstance(),true)
    }

    static SliceInstance getSliceInstanceNotExist(ImageInstance image = BasicInstanceBuilder.getImageInstance(), boolean save = false) {
        SliceInstance slice = new SliceInstance(
                baseSlice: BasicInstanceBuilder.getAbstractSliceNotExist(image.baseImage, true),
                image: image,
                project: image.project
        )
        save ? BasicInstanceBuilder.saveDomain(slice) : BasicInstanceBuilder.checkDomain(slice)
    }


    static ImageInstance getImageInstance() {
        getImageInstanceNotExist(getProject(),true)
    }

    static ImageInstance getImageInstanceNotExist(Project project = getProject(), boolean save = false) {
        ImageInstance image = new ImageInstance(
                baseImage: getAbstractImageNotExist(true),
                project: project,
                //slide: getSlide(),
                user: User.findByUsername(Infos.SUPERADMINLOGIN))
        if(save) {
            saveDomain(image)
            saveDomain(new SliceInstance(image: image, project: image.project, baseSlice: image.baseImage.referenceSlice))
            return image
        } else {
            checkDomain(image)
        }
    }


    static NestedImageInstance getNestedImageInstance() {
        saveDomain(getNestedImageInstanceNotExist())
    }

    static NestedImageInstance getNestedImageInstanceNotExist(ImageInstance imageInstance = getImageInstance(), boolean save = false) {
        NestedImageInstance nestedImage = new NestedImageInstance(
                baseImage: getAbstractImageNotExist(true),
                parent: imageInstance,
                x: 10,
                y: 20,
                project: imageInstance.project,
                //slide: getSlide(),
                user: imageInstance.user)
        save ? saveDomain(nestedImage) : checkDomain(nestedImage)
    }

    static AlgoAnnotation getAlgoAnnotationNotExist(Project project, boolean save = false) {
        ImageInstance image = getImageInstanceNotExist(project,true)
        def annotation = new AlgoAnnotation(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image: image,
                slice: image.referenceSlice,
                user: getUserJobNotExist(getJobNotExist(true, project), true),
                project:project
        )
        save ? saveDomain(annotation) : checkDomain(annotation)
    }

    static AlgoAnnotation getAlgoAnnotationNotExist(ImageInstance image, boolean save = false) {
        def annotation = new AlgoAnnotation(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image: image,
                slice: image.referenceSlice,
                user: getUserJob(),
                project:image.project
        )
        save ? saveDomain(annotation) : checkDomain(annotation)
    }


    static AlgoAnnotation getAlgoAnnotation() {
        ImageInstance image = getImageInstance()
        def annotation = AlgoAnnotation.findOrCreateWhere(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image: image,
                slice: image.referenceSlice,
                user: getUserJob(),
                project:getImageInstance().project
        )
        saveDomain(annotation)
    }


    static AlgoAnnotation getAlgoAnnotationNotExist(Job job = getJob(), UserJob user = getUserJob(),boolean save = false) {
        ImageInstance image = getImageInstanceNotExist(job.project,true)
        AlgoAnnotation annotation = new AlgoAnnotation(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image:image,
                slice: image.referenceSlice,
                user: user,
                project:job.project
        )
        save ? saveDomain(annotation) : checkDomain(annotation)
    }

    static AlgoAnnotation getAlgoAnnotationNotExist(Job job, UserJob user, ImageInstance image, boolean save = false) {
        AlgoAnnotation annotation = new AlgoAnnotation(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image:image,
                slice: image.referenceSlice,
                user: user,
                project:job.project
        )
        save ? saveDomain(annotation) : checkDomain(annotation)
    }

//    static AlgoAnnotationTerm createAlgoAnnotationTerm(Job job, AnnotationDomain annotation, UserJob userJob) {
//        AlgoAnnotationTerm at = getAlgoAnnotationTermNotExist()
//        at.project = job.project
//        at.annotationIdent = annotation.id
//        at.domainClassName = annotation.class.getName()
//        at.userJob = userJob
//        checkDomain(at)
//        saveDomain(at)
//        at
//    }

    //CytomineDomain annotation = (useAlgoAnnotation? saveDomain(getUserAnnotationNotExist()) :  saveDomain(getAlgoAnnotationNotExist()))

    static AlgoAnnotationTerm getAlgoAnnotationTerm(Job job = getJob(), AnnotationDomain annotation, UserJob user = getUserJob()) {
        def term = getTermNotExist()
        term.ontology = annotation.project.ontology
        saveDomain(term)
        def algoannotationTerm = new AlgoAnnotationTerm(term:term,expectedTerm:term,userJob:user,rate:0)
        algoannotationTerm.setAnnotation(annotation)
        saveDomain(algoannotationTerm)
    }

    static AlgoAnnotationTerm getAlgoAnnotationTerm(boolean useAlgoAnnotation) {
        getAlgoAnnotationTerm(getJob(),getUserJob(),useAlgoAnnotation)
    }

    static AlgoAnnotationTerm getAlgoAnnotationTerm(Job job = getJob(), UserJob user = getUserJob(),boolean useAlgoAnnotation = false) {
        def annotation = (useAlgoAnnotation? saveDomain(getAlgoAnnotationNotExist()) :  saveDomain(getUserAnnotationNotExist()))
        getAlgoAnnotationTerm(job,annotation,user)
    }

    //getAlgoAnnotationTermForAlgoAnnotation
    static AlgoAnnotationTerm getAlgoAnnotationTermNotExist(AnnotationDomain annotation, Term term,boolean save = false) {
        UserJob userJob = getUserJobNotExist(getJobNotExist(true, annotation.project), true)
        def algoannotationTerm = new AlgoAnnotationTerm(term:term,userJob:userJob, expectedTerm: term, rate:1d)
        algoannotationTerm.setAnnotation(annotation)
        save ? saveDomain(algoannotationTerm) : checkDomain(algoannotationTerm)
    }

    //getAlgoAnnotationTermForAlgoAnnotation
    static AlgoAnnotationTerm getAlgoAnnotationTermNotExist(Job job = getJob(),UserJob userJob = getUserJob(),AnnotationDomain annotation = saveDomain(getUserAnnotationNotExist()),boolean save = false) {
        def term = getTermNotExist()
        term.ontology = annotation.project.ontology
        saveDomain(term)
        def algoannotationTerm = new AlgoAnnotationTerm(term:term,userJob:userJob, expectedTerm: term, rate:1d)
        algoannotationTerm.setAnnotation(annotation)
        algoannotationTerm
    }

    static AlgoAnnotationTerm getAlgoAnnotationTermNotExistForAlgoAnnotation() {
        def term = saveDomain(getTermNotExist())
        def annotation = saveDomain(getAlgoAnnotationNotExist())
        def user = saveDomain(getUserJobNotExist())
        def algoannotationTerm = new AlgoAnnotationTerm(term:term,userJob:user, expectedTerm: term, rate:1d)
        algoannotationTerm.setAnnotation(annotation)
        algoannotationTerm
    }

    static AlgoAnnotation getAlgoAnnotationNotExist(ImageInstance image, String polygon, UserJob user, Term term) {
        AlgoAnnotation annotation = new AlgoAnnotation(
                location: new WKTReader().read(polygon),
                image:image,
                user: user,
                project:project
        )
        annotation = saveDomain(annotation)


       def at = getAlgoAnnotationTermNotExist(user.job,user,annotation,true)
        at.term = term
        at.userJob = user
        saveDomain(at)
        annotation
    }

    static ReviewedAnnotation createReviewAnnotation(ImageInstance image) {
        ReviewedAnnotation review = getReviewedAnnotationNotExist()
        review.project = image.project
        review.image = image
        review.slice = image.referenceSlice
        saveDomain(review)
        review
    }

    static ReviewedAnnotation createReviewAnnotation(AnnotationDomain annotation) {
        ReviewedAnnotation review = getReviewedAnnotationNotExist()
        review.project = annotation.project
        review.image = annotation.image
        review.slice = annotation.slice
        review.location = annotation.location
        review.putParentAnnotation(annotation)
        saveDomain(review)
        review
    }

    static ReviewedAnnotation getReviewedAnnotation(ImageInstance image, boolean save = false) {
        def basedAnnotation = saveDomain(getUserAnnotationNotExist())
        def annotation = new ReviewedAnnotation(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image: image,
                slice: image.referenceSlice,
                user: User.findByUsername(Infos.SUPERADMINLOGIN),
                project:image.project,
                status : 0,
                reviewUser: User.findByUsername(Infos.SUPERADMINLOGIN)
        )
        annotation.putParentAnnotation(basedAnnotation)
        save ? saveDomain(annotation) : checkDomain(annotation)

        def term = getTerm()
        term.ontology = image.project.ontology
        save ? saveDomain(term) : checkDomain(term)

        annotation.addToTerms(term)
        save ? saveDomain(annotation) : checkDomain(term)
        annotation
    }

    static ReviewedAnnotation getReviewedAnnotation() {
        def basedAnnotation = saveDomain(getUserAnnotationNotExist())
        def image = getImageInstance()
        def annotation = ReviewedAnnotation.findOrCreateWhere(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image: image,
                slice: image.referenceSlice,
                user: User.findByUsername(Infos.SUPERADMINLOGIN),
                project:image.project,
                status : 0,
                reviewUser: User.findByUsername(Infos.SUPERADMINLOGIN)
        )
        annotation.putParentAnnotation(basedAnnotation)
        saveDomain(annotation)

        def term = getTerm()
        term.ontology = image.project.ontology
        checkDomain(term)
        saveDomain(term)

        annotation.addToTerms(term)
        checkDomain(annotation)
        saveDomain(annotation)
        annotation
    }

    static ReviewedAnnotation getReviewedAnnotationNotExist(Project project, boolean save = false) {
        def basedAnnotation = saveDomain(getUserAnnotationNotExist())
        ImageInstance image = getImageInstanceNotExist(project,true)

        def annotation = new ReviewedAnnotation(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image: image,
                slice: image.referenceSlice,
                user: User.findByUsername(Infos.SUPERADMINLOGIN),
                project:project,
                status : 0,
                reviewUser: User.findByUsername(Infos.SUPERADMINLOGIN)
        )
        annotation.putParentAnnotation(basedAnnotation)
        save ? saveDomain(annotation) : checkDomain(annotation)
    }

    static ReviewedAnnotation getReviewedAnnotationNotExist(ImageInstance image, boolean save = false) {
        def basedAnnotation = saveDomain(getUserAnnotationNotExist())

        def annotation = new ReviewedAnnotation(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image: image,
                slice: image.referenceSlice,
                user: User.findByUsername(Infos.SUPERADMINLOGIN),
                project:image.project,
                status : 0,
                reviewUser: User.findByUsername(Infos.SUPERADMINLOGIN)
        )
        annotation.putParentAnnotation(basedAnnotation)
        save ? saveDomain(annotation) : checkDomain(annotation)
    }

     static ReviewedAnnotation getReviewedAnnotationNotExist() {
         def basedAnnotation = saveDomain(getUserAnnotationNotExist())
         ImageInstance image = getImageInstance()

         def annotation = ReviewedAnnotation.findOrCreateWhere(
                 location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                 image: image,
                 slice: image.referenceSlice,
                 user: User.findByUsername(Infos.SUPERADMINLOGIN),
                 project:getImageInstance().project,
                 status : 0,
                 reviewUser: User.findByUsername(Infos.SUPERADMINLOGIN)
         )
         annotation.putParentAnnotation(basedAnnotation)
         checkDomain(annotation)
     }

    static AnnotationTerm getAnnotationTerm() {
        def annotation = saveDomain(getUserAnnotationNotExist())
        def term = getTermNotExist()
        term.ontology = annotation.project.ontology
        term = saveDomain(term)
        def user = User.findByUsername(Infos.SUPERADMINLOGIN)
        saveDomain(new AnnotationTerm(userAnnotation: annotation, term: term,user: user))
    }

    static AnnotationTerm getAnnotationTermNotExist(UserAnnotation annotation=saveDomain(getUserAnnotationNotExist()),boolean save=false) {
        def term = getTermNotExist()
        term.ontology = annotation.project.ontology
        saveDomain(term)
        def user = User.findByUsername(Infos.SUPERADMINLOGIN)
        def annotationTerm = new AnnotationTerm(userAnnotation:annotation,term:term,user:user)
        save ? saveDomain(annotationTerm) : checkDomain(annotationTerm)
    }

    static AnnotationTerm getAnnotationTermNotExist(UserAnnotation annotation,Term term,boolean save=false) {
        def user = User.findByUsername(Infos.SUPERADMINLOGIN)
        def annotationTerm = new AnnotationTerm(userAnnotation:annotation,term:term,user:user)
        save ? saveDomain(annotationTerm) : checkDomain(annotationTerm)
    }

    static UserAnnotation getUserAnnotation() {
        ImageInstance image = getImageInstance()
        def annotation = UserAnnotation.findOrCreateWhere(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image: image,
                slice: image.referenceSlice,
                user: User.findByUsername(Infos.SUPERADMINLOGIN),
                project:image.project
        )
        saveDomain(annotation)
    }


    static UserAnnotation getUserAnnotationNotExist(boolean save = false) {
        ImageInstance image = getImageInstance()
        Project project = image.project
        getUserAnnotationNotExist(project,image,save)
    }

    static UserAnnotation getUserAnnotationNotExist(Project project, boolean save = false) {
        getUserAnnotationNotExist(project,getImageInstanceNotExist(project, true),save)
    }

    static UserAnnotation getUserAnnotationNotExist(SliceInstance slice, boolean save = false) {
        getUserAnnotationNotExist(slice, User.findByUsername(Infos.SUPERADMINLOGIN),save)
    }

    static UserAnnotation getUserAnnotationNotExist(Project project = getImageInstance().project, ImageInstance image,boolean save = false) {
        getUserAnnotationNotExist(project, image, User.findByUsername(Infos.SUPERADMINLOGIN), save)
    }

    static UserAnnotation getUserAnnotationNotExist(SliceInstance slice, User user, boolean save = false) {
        getUserAnnotationNotExist(project, slice.image, user, save)
    }

    static UserAnnotation getUserAnnotationNotExist(Project project = getImageInstance().project, ImageInstance image, User user, boolean save = false) {
        UserAnnotation annotation = new UserAnnotation(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image:image,
                slice: image.referenceSlice,
                user: user,
                project:project
        )
        save ? saveDomain(annotation) : checkDomain(annotation)
    }

    static UserAnnotation getUserAnnotationNotExist(SliceInstance slice, String polygon, User user, Term term) {
        UserAnnotation annotation = new UserAnnotation(
                location: new WKTReader().read(polygon),
                image: slice.image,
                slice: slice,
                user: user,
                project:slice.project
        )
        annotation = saveDomain(annotation)


        def at = getAnnotationTermNotExist(annotation,true)
        at.term = term
        at.user = user
        saveDomain(at)
        annotation
    }

    static UserAnnotation getUserAnnotationNotExist(ImageInstance image, User user, Term term) {
        getUserAnnotationNotExist(image.referenceSlice, user, term)
    }
    static UserAnnotation getUserAnnotationNotExist(SliceInstance slice, User user, Term term) {
        UserAnnotation annotation = new UserAnnotation(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image:slice.image,
                slice: slice,
                user: user,
                project:slice.image.project
        )
        annotation = saveDomain(annotation)

        if(term) {
            def at = getAnnotationTermNotExist(annotation,true)
            at.term = term
            at.user = user
            saveDomain(at)
        }

        annotation
    }


    static RoiAnnotation getRoiAnnotation() {
        def annotation = RoiAnnotation.findOrCreateWhere(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image: getImageInstance(),
                user:User.findByUsername(Infos.SUPERADMINLOGIN),
                project:getImageInstance().project
        )
        saveDomain(annotation)
    }

    static RoiAnnotation getRoiAnnotationNotExist(ImageInstance image = getImageInstance(),boolean save = false) {
        RoiAnnotation annotation = new RoiAnnotation(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image:image,
                user: User.findByUsername(Infos.SUPERADMINLOGIN),
                project:image.project
        )
        save ? saveDomain(annotation) : checkDomain(annotation)
    }

    static RoiAnnotation getRoiAnnotationNotExist(ImageInstance image = getImageInstance(),User user,boolean save = false) {
        RoiAnnotation annotation = new RoiAnnotation(
                location: new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))"),
                image:image,
                user: user,
                project:image.project
        )
        save ? saveDomain(annotation) : checkDomain(annotation)
    }

    static ReviewedAnnotation getReviewedAnnotationNotExist(SliceInstance slice, String polygon, User user, Term term) {
        def annotation = getUserAnnotationNotExist(slice,polygon,user,term)

        def reviewedAnnotation = ReviewedAnnotation.findOrCreateWhere(
                location: annotation.location,
                image: annotation.image,
                slice : slice,
                user: user,
                project:annotation.project,
                status : 0,
                reviewUser: user
        )
        reviewedAnnotation.putParentAnnotation(annotation)
        reviewedAnnotation.addToTerms(term)
        saveDomain(reviewedAnnotation)
    }
    static ReviewedAnnotation getReviewedAnnotationNotExist(ImageInstance image, String polygon, User user, Term term) {
        getReviewedAnnotationNotExist(image.referenceSlice, polygon, user, term)
    }


    static SharedAnnotation getSharedAnnotation() {
        AnnotationDomain annotation = getUserAnnotation()
        def sharedannotation = SharedAnnotation.findOrCreateWhere(
                sender: User.findByUsername(Infos.SUPERADMINLOGIN),
                comment: "This is a test",
                annotationIdent: annotation.id,
                annotationClassName: annotation.class.name
        )

        sharedannotation.receivers = new HashSet<User>();
        sharedannotation.receivers.add(getUser( Infos.ADMINLOGIN, Infos.ADMINPASSWORD))
        saveDomain(sharedannotation)
    }

    static SharedAnnotation getSharedAnnotationNotExist(boolean save = false) {
        AnnotationDomain annotation = getUserAnnotation()
        def sharedannotation = new SharedAnnotation(
                sender: User.findByUsername(Infos.SUPERADMINLOGIN),
                comment: "This is a test",
                annotationIdent: annotation.id,
                annotationClassName: annotation.class.name
        )
        sharedannotation.receivers = new HashSet<User>();
        sharedannotation.receivers.add(getSuperAdmin( Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD))
        save ? saveDomain(sharedannotation) : checkDomain(sharedannotation)
    }

    static Track getTrack() {
        def track = Track.findByName("BasicTrack")
        if (!track) {
            track = new Track(name: "BasicTrack", image: getImageInstance(), color: "FF0000", project: getProject())
            saveDomain(track)
        }
        track
    }

    static Track getTrackNotExist(boolean save = false) {
        getTrackNotExist(getImageInstance(), save)
    }

    static Track getTrackNotExist(ImageInstance image, boolean save = false) {
        Track track = new Track(name: getRandomString(), image: image, color: "FF0000", project: getProject())
        save ? saveDomain(track) : checkDomain(track)
    }

    static AnnotationTrack getAnnotationTrack() {
        def at = AnnotationTrack.findByTrackAndAnnotationIdent(getTrack(), getUserAnnotation().id)
        if (!at) {
            UserAnnotation ua = getUserAnnotation()
            at = new AnnotationTrack(track: getTrack(), annotationIdent: ua.id, annotationClassName: ua.class.name, slice: getSliceInstance())
            saveDomain(at)
        }
        at
    }

    static AnnotationTrack getAnnotationTrack(AnnotationDomain annotation, Track track, SliceInstance slice) {
        def at = AnnotationTrack.findByTrackAndAnnotationIdent(track, annotation.id)
        if (!at) {
            at = new AnnotationTrack(track: track, annotationIdent: annotation.id, annotationClassName: annotation.class.name, slice: slice)
            saveDomain(at)
        }
        at
    }

    static AnnotationTrack getAnnotationTrackNotExist(boolean save = false) {
        getAnnotationTrackNotExist(getTrack(), getSliceInstance(), save)
    }

    static AnnotationTrack getAnnotationTrackNotExist(Track track, SliceInstance slice, boolean save = false) {
        UserAnnotation ua = getUserAnnotationNotExist()
        ua.slice = slice
        ua.image = slice.image
        ua = ua.save()
        AnnotationTrack at = new AnnotationTrack(track: track, annotationIdent: ua.id, annotationClassName: ua.class.name, slice: slice)
        save ? saveDomain(at) : checkDomain(at)
    }

    static AttachedFile getAttachedFileNotExist(boolean save = false) {
        getAttachedFileNotExist("test/functional/be/cytomine/utils/simpleFile.txt",save)
    }

    static AttachedFile getAttachedFileNotExist(String file,boolean save = false) {
        return getAttachedFileNotExist(file, new File(file).name, save)
    }

    static AttachedFile getAttachedFileNotExist(String file, String filename, boolean save = false) {
        def project = getProjectNotExist(true)
        return getAttachedFileNotExist(file, filename, project, save)
    }

    static AttachedFile getAttachedFileNotExist(CytomineDomain domain, boolean save = false) {
        return getAttachedFileNotExist("test/functional/be/cytomine/utils/simpleFile.txt", "simpleFile.txt", domain, save)
    }

    static AttachedFile getAttachedFileNotExist(String file, String filename, CytomineDomain domain, boolean save = false) {
        def attachedFile = new AttachedFile()
        attachedFile.setDomain(domain)
        File f = new File(file)
        attachedFile.filename = filename
        attachedFile.data = f.bytes
        save ? saveDomain(attachedFile) : checkDomain(attachedFile)
    }

    static ImageFilter getImageFilter() {
       def imagefilter = ImageFilter.findByName("imagetest")
       if(!imagefilter) {
           imagefilter = new ImageFilter(name:"imagetest",baseUrl:"baseurl",imagingServer:getImagingServer())
           saveDomain(imagefilter)
       }
        imagefilter
    }

    static ImageFilter getImageFilterNotExist(boolean save = false) {
       def imagefilter = new ImageFilter(name:"imagetest"+new Date(),baseUrl:"baseurl",imagingServer:getImagingServer())
        save ? saveDomain(imagefilter) : checkDomain(imagefilter)
    }

    static ImageFilterProject getImageFilterProject() {
       def imageFilterProject = ImageFilterProject.find()
        if(!imageFilterProject) {
            imageFilterProject = new ImageFilterProject(imageFilter:getImageFilter(),project:getProject())
            saveDomain(imageFilterProject)
        }
        return imageFilterProject
    }

    static ImageFilterProject getImageFilterProjectNotExist(boolean save = false) {
        def imagefilter = saveDomain(getImageFilterNotExist())
        def project = saveDomain(getProjectNotExist())
        def ifp = new ImageFilterProject(imageFilter: imagefilter, project: project)

        save ? saveDomain(ifp) : checkDomain(ifp)
    }

    static AbstractSlice getAbstractSlice() {
        AbstractImage ai = getAbstractImage()
        AbstractSlice slice = AbstractSlice.findByImageAndChannelAndTimeAndZStack(ai, 0, 0, 0)
        if(!slice) {
            slice = new AbstractSlice(image: ai, uploadedFile: ai.uploadedFile, mime: getMime(), channel : 0, zStack : 0, time : 0)
            slice = saveDomain(slice)
        }
        return slice
    }

    static AbstractSlice getAbstractSliceNotExist(boolean save = false) {
        getAbstractSliceNotExist(getAbstractImage(), save)
    }

    static AbstractSlice getAbstractSliceNotExist(AbstractImage ai, boolean save = false) {
        AbstractSlice slice = new AbstractSlice(image: ai, uploadedFile: ai.uploadedFile, mime: getMime(), channel : getRandomInteger(0,1000), zStack : getRandomInteger(0,1000), time : getRandomInteger(0,1000))

        save ? saveDomain(slice) : checkDomain(slice)
    }

    static AbstractImage getAbstractImage() {
        AbstractImage image = AbstractImage.findByOriginalFilename("originalFilename")
        if (!image) {
            image = new AbstractImage(uploadedFile: getUploadedFile(), originalFilename:"originalFilename", width: 16000, height: 16000, depth: 5, duration: 2, channels: 3)
        }
        image = saveDomain(image)
        //saveDomain(new StorageAbstractImage(storage : getStorage(), abstractImage : image))
        saveDomain(new AbstractSlice(uploadedFile: image.uploadedFile, image: image, mime: getMime(),  channel: 0, zStack: 0, time: 0))
        return image
    }

    static void buildStorageImageServerLinkForImage(AbstractImage abstractImage) {
        def imageServer = ImageServer.findByName("bidon")
        if (!imageServer) {
            imageServer = new ImageServer(name:"bidon",url:"http://bidon.server.com/",service:"service",className:"sample",available:true)
            saveDomain(imageServer)
        }
        UploadedFile uploadedFile = getUploadedFileNotExist(true)
        uploadedFile.imageServer = imageServer
        saveDomain(uploadedFile)


        abstractImage.uploadedFile = uploadedFile
        saveDomain(abstractImage)
        if (!MimeImageServer.findByImageServerAndMime(imageServer, abstractImage.referenceSlice.getMime())) {
            MimeImageServer mimeImageServer = new MimeImageServer(imageServer: imageServer, mime: abstractImage.referenceSlice.getMime())
            saveDomain(mimeImageServer)
        }
        imageServer
    }

    static AbstractImage getAbstractImageNotExist(boolean save = false) {
        getAbstractImageNotExist(getRandomString() , save)
    }

    static AbstractImage getAbstractImageNotExist(String filename, boolean save = false) {
        def image = new AbstractImage(uploadedFile: getUploadedFileNotExist(true), originalFilename:filename, width: 16000, height: 16000, depth: 5, duration: 2, channels: 3)
        if(save) {
            saveDomain(image)
            saveDomain(new AbstractSlice(uploadedFile: image.uploadedFile, image: image, mime: getMime(),  channel: 0, zStack: 0, time: 0))
            return image
        } else {
            checkDomain(image)
        }
    }

    static AbstractImage getAbstractImageNotExist(UploadedFile uploadedFile, boolean save = false) {
        def image = new AbstractImage(uploadedFile: uploadedFile, originalFilename:getRandomString(), width: 16000, height: 16000, depth: 5, duration: 2, channels: 3)
        if(save) {
            saveDomain(image)
            //saveDomain(new StorageAbstractImage(storage : getStorage(), abstractImage : image))
            saveDomain(new AbstractSlice(uploadedFile: image.uploadedFile, image: image, mime: getMime(),  channel: 0, zStack: 0, time: 0))
            return image
        } else {
            checkDomain(image)
        }
    }

    /*static StorageAbstractImage getStorageAbstractImage() {
        def storage = getStorage()
        def abstractImage = getAbstractImage()
        StorageAbstractImage sai = StorageAbstractImage.findByStorageAndAbstractImage(storage, abstractImage)
        if (!sai) {
            sai = new StorageAbstractImage(storage : storage, abstractImage : abstractImage)
            saveDomain(sai)
        }
        sai
    }*/

    static ImagingServer getImagingServer() {
        def ps = ImagingServer.findByUrl("processing_server_url")
        if (!ps) {
            ps = new ImagingServer(url: "processing_server_url")
            saveDomain(ps)
        }
        ps
    }

    static ImagingServer getImagingServerNotExist(boolean save = false) {
        ImagingServer ps = new ImagingServer(url: getRandomString())
        if(save) {
            saveDomain(ps)
        } else {
            checkDomain(ps)
        }
        ps
    }

    static Discipline getDiscipline() {
        def discipline = Discipline.findByName("BASICDISCIPLINE")
        if (!discipline) {
            discipline = new Discipline(name: "BASICDISCIPLINE")
            saveDomain(discipline)
        }
        discipline
    }

    static Discipline getDisciplineNotExist() {
        Discipline discipline = new Discipline(name: getRandomString())
        checkDomain(discipline)
    }

    static UploadedFile getUploadedFile() {
        def uploadedFile = UploadedFile.findByFilename("BASICFILENAME")
        if (!uploadedFile) {
            uploadedFile = new UploadedFile(
                    user: User.findByUsername(Infos.SUPERADMINLOGIN),
                    projects:[getProject().id],
                    storage: getStorage().id,
                    filename: "BASICFILENAME",
                    imageServer : getImageServer(),
                    originalFilename: "originalFilename",
                    ext: "tiff",
                    contentType: "tiff/ddd",
                    size: 1232l
            )
            saveDomain(uploadedFile)
        }
        uploadedFile
    }

    static UploadedFile getUploadedFileNotExist(boolean save = false) {
        UploadedFile uploadedFile = new UploadedFile(
                user: User.findByUsername(Infos.SUPERADMINLOGIN),
                projects:[getProject().id],
                storage: getStorage(),
                filename: getRandomString(),
                imageServer : getImageServer(),
                originalFilename: "originalFilename",
                ext: "tiff",
                contentType: "tiff/ddd",
                size: 1232l
        )
        save ? saveDomain(uploadedFile) : checkDomain(uploadedFile)
    }

    static UploadedFile getUploadedFileNotExist(User user, boolean save = false) {
        UploadedFile uploadedFile = new UploadedFile(
                user: User.findByUsername(Infos.SUPERADMINLOGIN),
                projects:[getProject().id],
                storage: getStorageNotExist(user, true),
                filename: getRandomString(),
                imageServer : getImageServer(),
                originalFilename: "originalFilename",
                ext: "tiff",
                contentType: "tiff/ddd",
                size: 1232l
        )
        save ? saveDomain(uploadedFile) : checkDomain(uploadedFile)
    }



    static AnnotationFilter getAnnotationFilter() {
        def filter = AnnotationFilter.findByName("BASICFILTER")
        if (!filter) {
            filter = new AnnotationFilter(name:"BASICFILTER",project:getProject(),user:User.findByUsername(Infos.SUPERADMINLOGIN))
            saveDomain(filter)
            filter.addToTerms(getTerm())
            filter.addToUsers(User.findByUsername(Infos.SUPERADMINLOGIN))
            saveDomain(filter)
        }
        filter
    }

    static AnnotationFilter getAnnotationFilterNotExist() {
        def annotationFilter = new AnnotationFilter(name:getRandomString(),project:getProject(),user: User.findByUsername(Infos.SUPERADMINLOGIN))
        annotationFilter.addToTerms(getTerm())
        annotationFilter.addToUsers(User.findByUsername(Infos.SUPERADMINLOGIN))
        checkDomain(annotationFilter)
    }

    static Sample getSample() {
        def sample = Sample.findByName("BASICSAMPLE")
        if (!sample) {
            sample = new Sample(name: "BASICSAMPLE")
        }
        saveDomain(sample)
    }

    static Sample getSampleNotExist() {
        def sample = new Sample(name: getRandomString())
        checkDomain(sample)
    }


    static Job getJob() {
        def job = Job.findByProjectAndSoftware(getProject(),getSoftware())
        if(!job) {
            job = new Job(project:getProject(),software:getSoftware())
            saveDomain(job)
        }
        job
    }

    static Job getJobNotExist(boolean save = false) {
        getJobNotExist(save, saveDomain(getSoftwareNotExist()))
    }

    static Job getJobNotExist(boolean save = false, Software software) {
        Job job =  new Job(software:software, project : saveDomain(getProjectNotExist()))
        save ? saveDomain(job) : checkDomain(job)
    }

    static Job getJobNotExistWithParameters(Software software) {
        Job job =  new Job(software:software, project : saveDomain(getProjectNotExist()))
        saveDomain(job)
        SoftwareParameter.findAllBySoftware(software).each {
            saveDomain(new JobParameter(value: it.name + "_VALUE", job:job,softwareParameter:it))
        }
        job
    }

    static Job getJobNotExist(boolean save = false, Project project) {
        Job job =  new Job(software:saveDomain(getSoftwareNotExist()), project : project)
        save ? saveDomain(job) : checkDomain(job)
    }

    static Job getJobNotExist(boolean save = false, Software software, Project project) {
        Job job =  new Job(software:software, project : project)
        save ? saveDomain(job) : checkDomain(job)
    }


    static JobTemplate getJobTemplate() {
        def job = JobTemplate.findByProjectAndSoftwareAndName(getProject(),getSoftware(),"jobtemplate")
        if(!job) {
            job = new JobTemplate(project:getProject(),software:getSoftware(), name:"jobtemplate")
            saveDomain(job)
        }
        SoftwareParameter param = new SoftwareParameter(software:software, name:"annotation",type:"Domain",required: true, index:400)
        saveDomain(param)
        job
    }

    static JobTemplate getJobTemplateNotExist(boolean save = false) {
        JobTemplate job =  new JobTemplate(software:saveDomain(getSoftwareNotExist()), project : saveDomain(getProjectNotExist()), name:getRandomString())
        save ? saveDomain(job) : checkDomain(job)
    }

    static JobTemplateAnnotation getJobTemplateAnnotation() {
        def job = JobTemplateAnnotation.findByJobTemplateAndAnnotationIdent(getJobTemplate(),getRoiAnnotation().id)
        if(!job) {
            job = new JobTemplateAnnotation(jobTemplate: getJobTemplate())
            job.setAnnotation(getRoiAnnotation())
            saveDomain(job)
        }
        job
    }

    static JobTemplateAnnotation getJobTemplateAnnotationNotExist(boolean save = false) {
        JobTemplateAnnotation jobTemplateAnnotation =  new JobTemplateAnnotation(jobTemplate:saveDomain(getJobTemplate()))
        jobTemplateAnnotation.setAnnotation(getRoiAnnotation())
        save ? saveDomain(jobTemplateAnnotation) : checkDomain(jobTemplateAnnotation)
    }

    static JobData getJobDataNotExist() {
        getJobDataNotExist(saveDomain(getJobNotExist()))
    }

    static JobData getJobDataNotExist(Job job, boolean save = false) {
        JobData jobData =  new JobData(job:job, key : "TESTKEY", filename: "filename.jpg")
        save ? saveDomain(jobData) : checkDomain(jobData)
    }

    static JobData getJobData() {
        saveDomain(getJobDataNotExist())
    }

    static JobParameter getJobParameter() {
         def job = getJob()
         def softwareParam = getSoftwareParameter()
         def jobParameter = JobParameter.findByJobAndSoftwareParameter(job,softwareParam)
         if (!jobParameter) {
             jobParameter = new JobParameter(value: "toto", job:job,softwareParameter:softwareParam)
             saveDomain(jobParameter)
         }
         jobParameter
     }

     static JobParameter getJobParameterNotExist() {
         def job = getJobNotExist(true)
         def softwareParam = getSoftwareParameterNotExist(true)
         def jobParameter = new JobParameter(value: "toto", job:job,softwareParameter:softwareParam)
         checkDomain(jobParameter)
     }

    static Ontology getOntology() {
        def ontology = Ontology.findByName("BasicOntology")
        if (!ontology || ontology.deleted != null) {
            ontology = new Ontology(name: "BasicOntology", user: User.findByUsername(Infos.SUPERADMINLOGIN))
            saveDomain(ontology)
            def term = getTermNotExist()
            term.ontology = ontology
            saveDomain(term)
            Infos.addUserRight(User.findByUsername(Infos.SUPERADMINLOGIN),ontology)
        }
        ontology
    }

    static Ontology getOntologyNotExist(boolean save = false) {
        Ontology ontology = new Ontology(name: getRandomString() + "", user: User.findByUsername(Infos.SUPERADMINLOGIN))
        save ? saveDomain(ontology) : checkDomain(ontology)
        if (save) {
            Term term = getTermNotExist(ontology,true)
            Infos.addUserRight(Infos.SUPERADMINLOGIN,ontology)
        }
        ontology
    }

    static Project getProject() {
        def name = "BasicProject".toUpperCase()
        def project = Project.findByName(name)
        if (!project || project.deleted != null) {
            project = new Project(name: name, ontology: getOntology(), discipline: getDiscipline())
            saveDomain(project)
            Infos.addUserRight(Infos.SUPERADMINLOGIN,project)
        }
        project
    }

    static Project getProjectNotExist(Ontology ontology,boolean save = false) {
        Project project = new Project(name: getRandomString(), ontology: ontology, discipline: getDiscipline()  )
        if(save) {
            saveDomain(project)
            Infos.addUserRight(Infos.SUPERADMINLOGIN,project)
        } else{
            checkDomain(project)
        }
        return project
    }

    static Project getProjectNotExist(boolean save = false) {
        getProjectNotExist(getOntologyNotExist(true),save)
    }


    static Relation getRelation() {
        def relation = Relation.findByName("BasicRelation")
        if (!relation) {
            relation = new Relation(name: "BasicRelation")
            saveDomain(relation)
        }
        relation
    }

    static Relation getRelationNotExist() {
        def relation = new Relation(name: getRandomString())
        checkDomain(relation)
    }

    static RelationTerm getRelationTerm() {
        def relation = getRelation()
        def term1 = getTerm()
        def term2 = getAnotherBasicTerm()

        def relationTerm = RelationTerm.findWhere('relation': relation, 'term1': term1, 'term2': term2)
        if (!relationTerm) {
            relationTerm = new RelationTerm(relation:relation, term1:term1, term2:term2)
            saveDomain(relationTerm)
        }
        relationTerm
    }

    static RelationTerm getParentRelationTerm(Term parent, Term child) {
        def relation = Relation.findByName(RelationTerm.names.PARENT)
        if (!relation) {
            relation = new Relation(name: RelationTerm.names.PARENT)
            BasicInstanceBuilder.saveDomain(relation)
        }

        def relationTerm = RelationTerm.findWhere('relation': relation, 'term1': parent, 'term2': child)
        if (!relationTerm) {
            relationTerm = new RelationTerm(relation:relation, term1:parent, term2:child)
            saveDomain(relationTerm)
        }
        relationTerm
    }

    static RelationTerm getRelationTermNotExist() {
        def relation = saveDomain(getRelationNotExist())
        def term1 = saveDomain(getTermNotExist())
        def term2 = saveDomain(getTermNotExist())
        term2.ontology = term1.ontology
        saveDomain(term2)
        def relationTerm = new RelationTerm(relation: relation, term1: term1, term2: term2)
        checkDomain(relationTerm)
    }

    static Mime getMime() {
        def mime = Mime.findByExtension("tif")
        if(!mime) {
            mime = new Mime(extension:"tif",mimeType: "tif")
            saveDomain(mime)
            def mis = new MimeImageServer(imageServer: getImageServer(),mime:mime)
            saveDomain(mis)
        }
        mime.refresh()
        mime.imageServers()
        mime
    }

    static Mime getMimeNotExist() {
        def mime = Mime.findByMimeType("mimeT");
        if (mime == null) {
            mime = new Mime(extension: "ext", mimeType: "mimeT")
            saveDomain(mime)
        }
        mime
    }

    static Property getAnnotationProperty() {
        def annotation = getUserAnnotation()
        def annotationProperty = Property.findByDomainIdentAndKey(annotation.id,'MyKeyBasic')
        if (!annotationProperty) {
            annotationProperty = new Property(domain: annotation, key: 'MyKeyBasic', value:"MyValueBasic")
            saveDomain(annotationProperty)
        }
        annotationProperty
    }

    static Property getAnnotationPropertyNotExist(UserAnnotation annotation = getUserAnnotation(), boolean save = false) {
        def annotationProperty = new Property(domain: annotation, key: getRandomString(),value: "MyValueBasic")
        save? saveDomain(annotationProperty) : checkDomain(annotationProperty)
    }

    static Property getProjectProperty() {
        def project = getProject()
        def projectProperty = Property.findByDomainIdentAndKey(project.id,'MyKeyBasic')
        if (!projectProperty) {
            projectProperty = new Property(domain: project, key: 'MyKeyBasic', value:"MyValueBasic")
            saveDomain(projectProperty)
        }
        projectProperty
    }

    static Property getProjectPropertyNotExist(Project project = getProject(), boolean save = false) {
        def projectProperty = new Property(domain: project, key: getRandomString(),value: "MyValueBasic")
        save? saveDomain(projectProperty) : checkDomain(projectProperty)
    }

    static Property getImageInstanceProperty() {
        def imageInstance = getImageInstance()
        def imageInstanceProperty = Property.findByDomainIdentAndKey(imageInstance.id,'MyKeyBasic')
        if (!imageInstanceProperty) {
            imageInstanceProperty = new Property(domain: imageInstance, key: 'MyKeyBasic', value:"MyValueBasic")
            saveDomain(imageInstanceProperty)
        }
        imageInstanceProperty
    }

    static Property getAbstractImageProperty() {
        def abstractImage = getAbstractImage()
        def abstractImageProperty = Property.findByDomainIdentAndKey(abstractImage.id,'MyKeyBasic')
        if (!abstractImageProperty) {
            abstractImageProperty = new Property(domain: abstractImage, key: 'MyKeyBasic', value:"MyValueBasic")
            saveDomain(abstractImageProperty)
        }
        abstractImageProperty
    }

    static Property getImageInstancePropertyNotExist(ImageInstance imageInstance = getImageInstance(), boolean save = false) {
        def imageInstanceProperty = new Property(domain: imageInstance, key: getRandomString(),value: "MyValueBasic")
        save? saveDomain(imageInstanceProperty) : checkDomain(imageInstanceProperty)
    }

    static Property getAbstractImagePropertyNotExist(AbstractImage abstractImage = getAbstractImage(), boolean save = false) {
        def abstractImageProperty = new Property(domain: abstractImage, key: getRandomString(),value: "MyValueBasic")
        save? saveDomain(abstractImageProperty) : checkDomain(abstractImageProperty)
    }

    static Tag getTag() {
        Tag tag = Tag.findByName("TEST_TAG")
        if (!tag) {
            tag = new Tag(name: 'TEST_TAG', user: User.findByUsername(Infos.SUPERADMINLOGIN))
            saveDomain(tag)
        }
        tag
    }

    static Tag getTagNotExist(boolean save  = false) {
        Tag tag = new Tag(name: getRandomString(), user: User.findByUsername(Infos.SUPERADMINLOGIN))
        save? saveDomain(tag) : checkDomain(tag)
    }

    static TagDomainAssociation getTagDomainAssociation() {
        TagDomainAssociation association = TagDomainAssociation.findByTag(getTag())
        if (!association) {
            association = new TagDomainAssociation(tag: getTag())
            association.setDomain(getProject())
            saveDomain(association)
        }
        association
    }

    static TagDomainAssociation getTagDomainAssociationNotExist(boolean save  = false) {
        return getTagDomainAssociationNotExist(getProjectNotExist(true), save)
    }

    static TagDomainAssociation getTagDomainAssociationNotExist(CytomineDomain domain, boolean save  = false) {
        TagDomainAssociation association = new TagDomainAssociation(tag: getTagNotExist(true))
        association.setDomain(domain)
        save? saveDomain(association) : checkDomain(association)
    }

    static Instrument getScanner() {
        Instrument scanner = new Instrument(brand: "brand", model: "model")
        saveDomain(scanner)
    }

    static Instrument getNewScannerNotExist(boolean save  = false) {
        def scanner = new Instrument(brand: "newBrand", model: getRandomString())
        save? saveDomain(scanner) : checkDomain(scanner)
    }

    static Sample getSlide() {
        def name = "BasicSlide".toUpperCase()
        def slide = Sample.findByName(name)
        if (!slide) {
            slide = new Sample(name: name)
            saveDomain(slide)
        }
        slide
    }

    static Sample getSlideNotExist() {
        def slide = new Sample(name: getRandomString())
        checkDomain(slide)
    }

    static User getUser(String username, String password) {
        def user = SecUser.findByUsername(username)
        if (!user) {
            user = new User(username: username,firstname: "Basic",lastname: "User ($username)",email: "Basic@User.be",password: password,enabled: true, origin: "TEST")
            user.generateKeys()
            user = saveDomain(user)
            SecUserSecRole.create(user,SecRole.findByAuthority("ROLE_USER"),true)
        }
        user
    }

    static User getGhest(String username, String password) {
        def user = SecUser.findByUsername(username)
        if (!user) {
            user = new User(username: username,firstname: "Basic",lastname: "User",email: "Basic@User.be",password: password,enabled: true, origin: "TEST")
            user.generateKeys()
            saveDomain(user)
            SecUserSecRole.create(user,SecRole.findByAuthority("ROLE_GUEST"),true)
        }
        user
    }

    static User getUserNotExist(boolean save = false) {
       User user = new User(username: getRandomString(),firstname: "BasicNotExist",lastname: "UserNotExist",email: "BasicNotExist@User.be",password: "password",enabled: true, origin: "TEST")
        user.generateKeys()
        if(save) {
            saveDomain(user)
            SecUserSecRole.create(user,SecRole.findByAuthority("ROLE_USER"),true)
        } else {
            checkDomain(user)
        }

        user
    }

    static User getGhestNotExist(boolean save = false) {
       User user = new User(username: getRandomString(),firstname: "BasicNotExist",lastname: "UserNotExist",email: "BasicNotExist@User.be",password: "password",enabled: true, origin: "TEST")
        user.generateKeys()
        if(save) {
            saveDomain(user)
            SecUserSecRole.create(user,SecRole.findByAuthority("ROLE_GUEST"),true)
        } else {
            checkDomain(user)
        }

        user
    }

    static Group getGroup() {
        def name = "BasicGroup".toUpperCase()
        def group = Group.findByName(name)
        if (!group) {
            group = new Group(name: name)
            saveDomain(group)
        }
        group
    }

    static Group getGroupNotExist() {
        Group group = new Group(name: getRandomString())
        checkDomain(group)
    }

    static Storage getStorage() {
        def storage = Storage.findByName("bidon")
        if(!storage) {
            storage = new Storage(name:"bidon",user: User.findByUsername(Infos.SUPERADMINLOGIN))
            saveDomain(storage)
            Infos.addUserRight(User.findByUsername(Infos.SUPERADMINLOGIN),storage)
        }
        return storage
    }

    static Storage getStorageNotExist(boolean save = false) {
        getStorageNotExist(User.findByUsername(Infos.SUPERADMINLOGIN), save)
    }

    static Storage getStorageNotExist(User user, boolean save = false) {
        Storage storage = new Storage(name: getRandomString(), user: user)

        if(save) {
            saveDomain(storage)
            Infos.addUserRight(user.username,storage)
        } else {
            checkDomain(storage)
        }
        storage
    }

    static Term getTerm() {
        def term = Term.findByName("BasicTerm")
        if (!term) {
            term = new Term(name: "BasicTerm", ontology: getOntology(), color: "FF0000")
            saveDomain(term)
        }
        term
    }

    static Term getAnotherBasicTerm() {
        def term = Term.findByName("AnotherBasicTerm")
        if (!term) {
            term = new Term(name: "AnotherBasicTerm", ontology: getOntology(), color: "F0000F")
            saveDomain(term)
        }
        term
    }

    static Term getTermNotExist(boolean save = false) {
        getTermNotExist(saveDomain(getOntologyNotExist()), save)
    }

    static Term getTermNotExist(Ontology ontology,boolean save = false) {
        Term term = new Term(name: getRandomString(), ontology: ontology, color: "0F00F0")
        save ? saveDomain(term) :  checkDomain(term)
    }

    static ProcessingServer getProcessingServer() {
        def processingServer = ProcessingServer.findByName("TestingProcessingServer")

        if (!processingServer) {
            processingServer = new ProcessingServer(username : "test", name: "TestingProcessingServer", host: "localhost",port : 10022, index : 0)
            saveDomain(processingServer)
        }
        processingServer
    }

    static ProcessingServer getProcessingServerNotExist(boolean save = false) {
        def processingServer = new ProcessingServer(username : "test", name: getRandomString(), host: getRandomString(),port : 10022, index : 0)
        if(save) {
            saveDomain(processingServer)
        } else {
            checkDomain(processingServer)
        }
        processingServer
    }

    static Software getSoftware() {
        def software = Software.findByName("AnotherBasicSoftware")
        if (!software) {
            software = new Software(name: "AnotherBasicSoftware")
            saveDomain(software)
            Infos.addUserRight(Infos.SUPERADMINLOGIN,software)
        }
        software
    }

    static Software getSoftwareNotExist(boolean save = false) {
        def software = new Software(name: getRandomString())
        if(save) {
            saveDomain(software)
            Infos.addUserRight(Infos.SUPERADMINLOGIN,software)
        } else {
            checkDomain(software)
        }

        software
    }

    static Software getSoftwareNotExistWithParameters() {
        Software software = getSoftwareNotExist(true)
        saveDomain(new SoftwareParameter(name: getRandomString(),software:software,type:"String"))
        saveDomain(new SoftwareParameter(name: getRandomString(),software:software,type:"String"))
        software
    }

    static Software getSoftwareNotExistForRabbit(boolean save = false) {
        def software = new Software(name: getRandomString())
        if(save) {
            saveDomain(software)
            Infos.addUserRight(Infos.SUPERADMINLOGIN,software)
        } else {
            checkDomain(software)
        }

        software
    }

    static SoftwareParameter getSoftwareParameter() {
        Software software = getSoftware()
        def parameter = SoftwareParameter.findBySoftware(software)
        if (!parameter) {
            parameter = new SoftwareParameter(name:"anotherParameter",software:software,type:"String")
            saveDomain(parameter)
        }
        parameter
    }
    static SoftwareParameter getSoftwareParameterNotExist(boolean save = false) {
        Software software = getSoftware()
        def parameter =   new SoftwareParameter(name: getRandomString(),software:software,type:"String")
        if(save) {
            saveDomain(parameter)
        } else {
            checkDomain(parameter)
        }
    }

    static ParameterConstraint getParameterConstraint() {
        ParameterConstraint parameter = ParameterConstraint.findByNameAndDataType("equals","Number")
        if (!parameter) {
            parameter = new ParameterConstraint(name: "equals", expression: '(Double.valueOf("[parameterValue]") as Number) == (Double.valueOf("[value]") as Number)', dataType: "Number")
            saveDomain(parameter)
        }
        parameter
    }

    static ParameterConstraint getParameterConstraintNotExist(boolean save = false) {
        def parameter = new ParameterConstraint(name : getRandomString(), expression: getRandomString(), dataType: "String")
        if(save) {
            saveDomain(parameter)
        } else {
            checkDomain(parameter)
        }
    }

    static SoftwareParameterConstraint getSoftwareParameterConstraint() {
        SoftwareParameter parameter = getSoftwareParameter()
        ParameterConstraint parameterConstraint = getParameterConstraint()

        def constraint = SoftwareParameterConstraint.findBySoftwareParameterAndParameterConstraint(parameter, parameterConstraint)

        if (!constraint) {
            constraint = new SoftwareParameterConstraint(parameterConstraint:parameterConstraint,softwareParameter:parameter, value:"0")
            saveDomain(constraint)
        }
        constraint
    }

    static SoftwareParameterConstraint getSoftwareParameterConstraintNotExist(boolean save = false) {
        SoftwareParameter parameter = getSoftwareParameterNotExist(true)
        ParameterConstraint parameterConstraint = ParameterConstraint.findByNameAndDataType("equals","String")
        def constraint =   new SoftwareParameterConstraint(parameterConstraint:parameterConstraint,softwareParameter:parameter, value:getRandomString())
        if(save) {
            saveDomain(constraint)
        } else {
            checkDomain(constraint)
        }
    }


    static SoftwareUserRepository getSoftwareUserRepository() {
        def repo = SoftwareUserRepository.findByProvider("github")
        if (!repo) {
            repo = new SoftwareUserRepository(provider: "github", username:getRandomString(),dockerUsername:getRandomString())
            saveDomain(repo)
        }
        repo
    }

    static SoftwareUserRepository getSoftwareUserRepositoryNotExist(boolean save = false) {
        def repo = new SoftwareUserRepository(provider: getRandomString(), username:getRandomString(),dockerUsername:getRandomString())
        if(save) {
            saveDomain(repo)
        } else {
            checkDomain(repo)
        }
    }


    static Description getDescriptionNotExist(CytomineDomain domain,boolean save = false) {
        Description description = new Description(domainClassName: domain.class.name, domainIdent: domain.id, data: "A description for this domain!")
        save ? saveDomain(description) : checkDomain(description)
    }

    static ImageServer getImageServer() {

        def imageServer = ImageServer.findByName("testIS")
        if (!imageServer) {
            imageServer = new ImageServer(name:"testIS",url:"http://test.server.com/",basePath:"/data/test",available:true)
            saveDomain(imageServer)
        }

        def storage = getStorage()

        def imageServerStorage = ImageServerStorage.findByImageServerAndStorage(imageServer, storage)
        if (!imageServerStorage) {
            imageServerStorage = new ImageServerStorage(imageServer: imageServer, storage : storage)
            saveDomain(imageServerStorage)
        }
        imageServer
    }

    static SoftwareProject getSoftwareProject() {
        SoftwareProject softproj = new SoftwareProject(software:getSoftware(),project:getProject())
        saveDomain(softproj)
    }

    static SoftwareProject getSoftwareProjectNotExist(Software software = getSoftwareNotExist(true), Project project = getProjectNotExist(true), boolean save = false) {
        SoftwareProject softproj = new SoftwareProject(software:software,project:project)
        save ? saveDomain(softproj) : checkDomain(softproj)
    }

    static Job createJobWithAlgoAnnotationTerm() {
        Project project = getProjectNotExist(true)
        Ontology ontology = project.ontology

        Term term1 = getTermNotExist(ontology,true)
        Term term2 = getTermNotExist(ontology,true)


        UserJob userJob = getUserJobNotExist(true)
        Job job = userJob.job
        job.project = project
        saveDomain(job)
        AlgoAnnotationTerm algoAnnotationGood = getAlgoAnnotationTermNotExist()
        algoAnnotationGood.term = term1
        algoAnnotationGood.expectedTerm = term1
        algoAnnotationGood.userJob = userJob
        saveDomain(algoAnnotationGood)

        AlgoAnnotationTerm algoAnnotationBad = getAlgoAnnotationTermNotExist()
        algoAnnotationBad.term = term1
        algoAnnotationBad.expectedTerm = term2
        algoAnnotationBad.userJob = userJob
        saveDomain(algoAnnotationBad)
        return job
    }

    static ImageSequence getImageSequence() {
        ImageSequence imageSequence = ImageSequence.findByImageGroup(getImageGroup())
        if(!imageSequence) {
            imageSequence = new ImageSequence(image:getImageInstanceNotExist(imageGroup.project,true),zStack:0,slice:0,time:0,channel:0,imageGroup:getImageGroup())
            imageSequence = saveDomain(imageSequence)
        }
        imageSequence
    }

    static ImageSequence getImageSequenceNotExist(boolean save = false) {
        def project = getProjectNotExist(true)
        def image = getImageInstanceNotExist(project,true)
        def group =  getImageGroupNotExist(project,true)
        ImageSequence seq = new ImageSequence(image:image,slice: 0, zStack:0,time:0,channel:2,imageGroup:group)
        save ? saveDomain(seq) : checkDomain(seq)
    }

    static ImageSequence getImageSequence(ImageInstance image,Integer channel, Integer zStack, Integer slice, Integer time,ImageGroup imageGroup,boolean save = false) {
        ImageSequence seq = new ImageSequence(image:image,zStack:zStack,time:time,channel:channel,slice:slice,imageGroup:imageGroup)
        save ? saveDomain(seq) : checkDomain(seq)
    }

    static ImageGroup getImageGroup() {
        ImageGroup imageGroup = ImageGroup.findByName("imagegroupname")
        if(!imageGroup) {
            imageGroup = new ImageGroup(project: getProject(), name:"imagegroupname" )
            imageGroup = saveDomain(imageGroup)
        }
        imageGroup
    }

    static ImageGroup getImageGroupNotExist(Project project = getProject(), boolean save = false) {
        ImageGroup imageGroup = new ImageGroup(project: project)
        save ? saveDomain(imageGroup) : checkDomain(imageGroup)
    }

    static def getMultiDimensionalDataSet(def channel,def zStack,def slice,def time) {
        Project project = getProjectNotExist(true)
        ImageGroup group = getImageGroupNotExist(project,true)

        def data = []

        channel.eachWithIndex { c,ci ->
            zStack.eachWithIndex { z,zi ->
                slice.eachWithIndex { s,si ->
                    time.eachWithIndex { t,ti ->
                        String filename = c+"-"+z+"-"+s+"-"+t+"-"+System.currentTimeMillis()
                        def abstractImage = getAbstractImageNotExist(filename,true)
                        def imageInstance = getImageInstanceNotExist(project,true)
                        imageInstance.baseImage = abstractImage
                        saveDomain(imageInstance)

                        ImageSequence seq = getImageSequence(imageInstance,ci,zi,si,ti,group,true)
                        data << seq
                    }

                }
            }
        }
        return data
    }

    public static ImageInstance initImage() {

        //String urlImageServer = "http://localhost:9080"
        String urlImageServer = "http://image.cytomine.coop"

        User user = getUser("imgUploader", "password")

        ImageServer imageServer = ImageServer.findByUrl(urlImageServer)
        if(!imageServer) {
            imageServer = new ImageServer()
            imageServer.name = "IMS-test"
            imageServer.url =  urlImageServer
            imageServer.available = true
            imageServer.basePath = "/tmp/"
            saveDomain(imageServer)
        }

        Mime mime = Mime.findByMimeType("image/pyrtiff")
        if(!mime) {
            mime = new Mime()
            mime.mimeType = "image/pyrtiff"
            mime.extension = "tif"
            saveDomain(mime)
        }

        MimeImageServer mimeImageServer = MimeImageServer.findByMimeAndImageServer(mime,imageServer)
        if(!mimeImageServer) {
            mimeImageServer = new MimeImageServer()
            mimeImageServer.mime = mime
            mimeImageServer.imageServer = imageServer
            saveDomain(mimeImageServer)
        }

        Storage storage = Storage.findByUser(user)
        if(!storage) {
            storage = new Storage()
            storage.name = "lrollus test storage"
            storage.user = user
            saveDomain(storage)
        }

        Project project = Project.findByName("testimage")
        if(!project) {
            project = getProjectNotExist(true)
            project.name = "testimage"
            saveDomain(project)
        }

        UploadedFile uploadedFile = UploadedFile.findByOriginalFilename("originalFilename")
        if (!uploadedFile) {
            uploadedFile = new UploadedFile()
            uploadedFile.ext = "test"
            uploadedFile.contentType = "test"
            uploadedFile.filename = "test.tif"
            uploadedFile.originalFilename = "test.tif"
            uploadedFile.user = user
            uploadedFile.storage = storage
            uploadedFile.projects = [project.id]
            uploadedFile.size = 0 //fake
            saveDomain(uploadedFile)
        }

        AbstractImage abstractImage = AbstractImage.findByOriginalFilename("1383567901006/test.tif")
        if(!abstractImage) {
            abstractImage = new AbstractImage()
            abstractImage.originalFilename = "test.tif"
            abstractImage.width = 25088
            abstractImage.height = 37888
            abstractImage.magnification = 8
            abstractImage.physicalSizeX = 0.65d
            abstractImage.originalFilename = "test01.jpg"
            abstractImage.user = user
            abstractImage.uploadedFile = uploadedFile
            BasicInstanceBuilder.saveDomain(abstractImage)
        }
        saveDomain(new AbstractSlice(uploadedFile: abstractImage.uploadedFile, image: abstractImage, mime: mime,  channel: 0, zStack: 0, time: 0))

        ImageInstance imageInstance = ImageInstance.findByBaseImageAndProject(abstractImage,project)
        if(!imageInstance) {
            imageInstance = new ImageInstance()
            imageInstance.baseImage = abstractImage
            imageInstance.project = project
            imageInstance.user = User.findByUsername(Infos.SUPERADMINLOGIN)
            saveDomain(imageInstance)
        }


        ImagingServer imagingServer = ImagingServer.findByUrl("http://image.cytomine.be")
        if (!imagingServer) {
            imagingServer = new ImagingServer()
            imagingServer.url = "http://image.cytomine.be"
            saveDomain(imagingServer)
        }

        ReviewedAnnotation.findAllByImage(imageInstance).each {
            it.delete(flush: true)
        }
        UserAnnotation.findAllByImage(imageInstance).each {
            AnnotationTerm.findAllByUserAnnotation(it).each { at ->
                at.delete(flush:true)
            }
            it.delete(flush: true)
        }

        return imageInstance

    }

    static SearchEngineFilter getSearchEngineFilter() {
        def filter = SearchEngineFilter.findByName("BasicFilter")
        if (!filter) {
            //create if not exist
            def words = ["Test", "hello"]
            def domains = []
            def attributes = []
            def projects = []

            String json = new JSONObject().put("words", words as JSON)
                    .put("domains", domains as JSON)
                    .put("attributes", attributes as JSON)
                    .put("projects", projects as JSON)
                    .put("order", null)
                    .put("sort", null)
                    .put("op", "AND")
                    .toString();

            filter = new SearchEngineFilter(name: "BasicFilter", user: User.findByUsername(Infos.SUPERADMINLOGIN), filters: json)
            saveDomain(filter)
        }
        filter
    }

    static SearchEngineFilter getSearchEngineFilterNotExist(boolean save = false) {
        def json = ([words:["Test", "hello"], domains:["project"], attributes:[], projects:[], order : null, sort : "desc", op : "AND"] as JSON).toString()

        def filter = new SearchEngineFilter(name: getRandomString(), user: User.findByUsername(Infos.SUPERADMINLOGIN), filters: json)
        save ? saveDomain(filter) : checkDomain(filter)
    }

    static ProjectDefaultLayer getProjectDefaultLayer() {
        Project project = getProject();
        User user = User.findByUsername(Infos.SUPERADMINLOGIN);
        def layer = ProjectDefaultLayer.findByUserAndProject(user, project)
        if (!layer) {
            //create if not exist
            layer = new ProjectDefaultLayer(project: project, user: user, hideByDefault: false)
            saveDomain(layer)
        }
        layer
    }

    static ProjectDefaultLayer getProjectDefaultLayerNotExist(boolean save = false, boolean hideByDefault = false) {
        Project project = getProjectNotExist(true);
        User user = User.findByUsername(Infos.SUPERADMINLOGIN);

        def layer = new ProjectDefaultLayer(project: project, user: user, hideByDefault: false)
        save ? saveDomain(layer) : checkDomain(layer)
    }

    static ProjectRepresentativeUser getProjectRepresentativeUser() {
        Project project = getProject();
        User user = User.findByUsername(Infos.SUPERADMINLOGIN);
        def ref = ProjectRepresentativeUser.findByUserAndProject(user, project)
        if (!ref) {
            //create if not exist
            ref = new ProjectRepresentativeUser(project: project, user: user)
            saveDomain(ref)
        }
        ref
    }

    static ProjectRepresentativeUser getProjectRepresentativeUserNotExist(boolean save = false) {
        Project project = getProjectNotExist(true);
        User user = User.findByUsername(Infos.SUPERADMINLOGIN);

        def ref = new ProjectRepresentativeUser(project: project, user: user)
        save ? saveDomain(ref) : checkDomain(ref)
    }

    static MessageBrokerServer getMessageBrokerServer() {
        MessageBrokerServer msb = MessageBrokerServer.findByName("BasicMessageBrokerServer")
        if (!msb) {
            msb = new MessageBrokerServer(host: "localhost", port: 5672, name: "BasicMessageBrokerServer")
            saveDomain(msb)
        }
        msb
    }

    static MessageBrokerServer getMessageBrokerServerNotExist(boolean save = false) {
        MessageBrokerServer messageBrokerServers = new MessageBrokerServer(host: "localhost", port: 5672, name: getRandomString())
        save ? saveDomain(messageBrokerServers) : checkDomain(messageBrokerServers)
        messageBrokerServers
    }

    static AmqpQueue getAmqpQueue() {
        getMessageBrokerServer()
        AmqpQueue amqpQueue = AmqpQueue.findByName("BasicAmqpQueue")
        if(!amqpQueue) {
            amqpQueue = new AmqpQueue(name: "BasicAmqpQueue", host: "rabbitmqtest", exchange: "exchange"+getRandomString())
            saveDomain(amqpQueue)
        }
        amqpQueue
    }

    static AmqpQueue getAmqpQueueNotExist(boolean save = false){
        getMessageBrokerServer()
        AmqpQueue amqpQueue = new AmqpQueue(name: getRandomString(), host: Holders.config.grails.messageBrokerServerURL.toString().split(":")[0], exchange: "exchange"+getRandomString())
        amqpQueue.validate(failOnError: true)
        save ? saveDomain(amqpQueue) : checkDomain(amqpQueue)
        amqpQueue
    }

    static AmqpQueueConfig getAmqpQueueConfig() {
        AmqpQueueConfig amqpQueueConfig = AmqpQueueConfig.findByName("BasicAmqpQueueConfig")
        if(!amqpQueueConfig) {
            amqpQueueConfig = new AmqpQueueConfig(name: "BasicAmqpQueueConfig", defaultValue: "false", index: 100, isInMap: false, type: "Boolean")
            saveDomain(amqpQueueConfig)
        }
        amqpQueueConfig
    }

    static AmqpQueueConfig getAmqpQueueConfigNotExist(boolean save = false) {
        AmqpQueueConfig amqpQueueConfig = new AmqpQueueConfig(name: getRandomString(), defaultValue: "false", index: 200, isInMap: false, type: "Boolean")
        save ? saveDomain(amqpQueueConfig) : checkDomain(amqpQueueConfig)
        amqpQueueConfig
    }

    static AmqpQueueConfigInstance getAmqpQueueConfigInstance() {

        AmqpQueue amqpQueue = getAmqpQueue()
        AmqpQueueConfig amqpQueueConfig= getAmqpQueueConfig()

        AmqpQueueConfigInstance amqpQueueConfigInstance = AmqpQueueConfigInstance.findByQueueAndConfig(amqpQueue, amqpQueueConfig)
        if(!amqpQueueConfigInstance) {
            amqpQueueConfigInstance = new AmqpQueueConfigInstance(queue: amqpQueue, config: amqpQueueConfig, value: "dummyValue")
            saveDomain(amqpQueueConfigInstance)
        }
        amqpQueueConfigInstance
    }

    static AmqpQueueConfigInstance  getAmqpQueueConfigInstanceNotExist(AmqpQueue amqpQueue = getAmqpQueueNotExist(true), AmqpQueueConfig amqpQueueConfig =  getAmqpQueueConfigNotExist(true), boolean save = false) {
        AmqpQueueConfigInstance amqpQueueConfigInstance = new AmqpQueueConfigInstance(queue: amqpQueue, config: amqpQueueConfig, value: "dummyValueNotExist")
        save ? saveDomain(amqpQueueConfigInstance) : checkDomain(amqpQueueConfigInstance)
        amqpQueueConfigInstance

    }

    static Configuration getConfiguration() {
        def key = "test_test".toUpperCase()
        def value = "test"
        def config = Configuration.findByKey(key)

        if (!config) {
            config = new Configuration(key: key, value: value, readingRole: Configuration.Role.ALL)
            config = saveDomain(config)
        }
        config
    }

    static Configuration getConfigurationNotExist(boolean save = false) {
        def config = new Configuration(key: getRandomString(), value: getRandomString(), readingRole: Configuration.Role.ALL)
        log.debug "add config "+ config.key
        Configuration.list().each {
            log.debug it.id + " " + it.version + " " + it.key
        }

        save ? saveDomain(config) : checkDomain(config)
    }

    static PersistentProjectConnection getProjectConnection(boolean insert = false) {
        PersistentProjectConnection connection = new PersistentProjectConnection(os: "Linux", browser : "HttpClient", browserVersion : "1",
                project: getProject().id, user: getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).id)
        insert ? insertDomain(connection) : checkDomain(connection)
    }

    static PersistentImageConsultation getImageConsultationNotExist(boolean insert = false) {
        def connection = getProjectConnection(true)
        ImageInstance image = getImageInstanceNotExist(Project.read(connection.project), true)
        PersistentImageConsultation consult = new PersistentImageConsultation(image : image.id, imageName: image.getBlindInstanceFilename(),
                imageThumb: 'NO THUMB', mode:"test", user:getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).id,
                project: image.project.id)
        insert ? insertDomain(consult) : checkDomain(consult)
    }
    static PersistentImageConsultation getImageConsultationNotExist(Long projectId, boolean insert = false) {
        def connection = getProjectConnection()
        connection.project = projectId;
        insertDomain(connection)
        ImageInstance image = getImageInstanceNotExist(Project.read(projectId), true)
        PersistentImageConsultation consult = new PersistentImageConsultation(image : image.id, imageName: image.getBlindInstanceFilename(),
                imageThumb: 'NO THUMB', mode:"test", user:getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).id,
                project: image.project.id)
        insert ? insertDomain(consult) : checkDomain(consult)
    }

    static PersistentImageConsultation getImageConsultationNotExist(ImageInstance image, boolean insert = false) {
        def connection = getProjectConnection()
        connection.project = image.project.id;
        insertDomain(connection)
        PersistentImageConsultation consult = new PersistentImageConsultation(image : image.id, imageName: image.getBlindInstanceFilename(),
                imageThumb: 'NO THUMB', mode:"test", user:getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).id,
                project: image.project.id)
        insert ? insertDomain(consult) : checkDomain(consult)
    }

    static AnnotationAction getAnnotationActionNotExist(UserAnnotation annot, boolean insert = false) {
        AnnotationAction action = new AnnotationAction(image : annot.image.id, action: "TEST",
                annotationIdent: annot.id, annotationClassName: annot.getClass().name,
                annotationCreator: annot.user, project: annot.project,
                user:getUser(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).id)
        insert ? insertDomain(action) : checkDomain(action)
    }

    static PersistentUserPosition getPersistentUserPosition(SliceInstance slice, User user, boolean insert = false){
        LastUserPosition tmpPosition = new LastUserPosition(user:user.id, slice:slice.id, image: slice.image.id,
                imageName: slice.image.getBlindInstanceFilename(), project:slice.image.project, session: "test", zoom:0, rotation : 0.0)

        insert ? insertDomain(tmpPosition) : checkDomain(tmpPosition)

        PersistentUserPosition position = new PersistentUserPosition(user:user.id, slice:slice.id, image: slice.image.id,
                imageName: slice.image.getBlindInstanceFilename(), project:slice.image.project, session: "test", zoom:0, rotation : 0.0)

        insert ? insertDomain(position) : checkDomain(position)
    }

    static ImageGroupHDF5 getImageGroupHDF5() {
        def project = getProject()
        ImageGroup group = getImageGroupNotExist(project, true)
        ImageGroupHDF5 imageGroupHDF5 = ImageGroupHDF5.findByGroup(group)
        if (!imageGroupHDF5) {
            imageGroupHDF5 = new ImageGroupHDF5(group: group, filename: "${group.name}.h5")
            imageGroupHDF5 = saveDomain(imageGroupHDF5)
        }
        imageGroupHDF5
    }

    //files is an array of AbstractImages that relies to file that really exist
    static ImageGroupHDF5 getImageGroupHDF5NotExist(boolean save = false, def abstractImages = []) {
        def project = getProject()
        ImageGroup group = getImageGroupNotExist(project, true)

        abstractImages.eachWithIndex{ abstractImage, i ->
            def imageInstance = getImageInstanceNotExist(project, true)
            imageInstance.baseImage = abstractImage
            saveDomain(imageInstance)
        }

        ImageGroupHDF5 imageGroupHDF5 = new ImageGroupHDF5(group: group, filename: "${group.name}.h5")
        save ? saveDomain(imageGroupHDF5) : checkDomain(imageGroupHDF5)
    }
}
