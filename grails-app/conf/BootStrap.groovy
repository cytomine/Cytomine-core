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


import be.cytomine.image.AbstractImage
import be.cytomine.image.AbstractSlice
import be.cytomine.middleware.ImageServer
import be.cytomine.utils.CytomineMailService
import be.cytomine.image.multidim.ImageGroupHDF5Service
import be.cytomine.image.ImagePropertiesService
import be.cytomine.middleware.ImageServerService
import be.cytomine.processing.ImageRetrievalService
import be.cytomine.image.AbstractImage
import be.cytomine.security.SecUser
import be.cytomine.test.Infos
import be.cytomine.utils.Version
import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Environment
import grails.util.Holders
import groovy.sql.Sql
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.grails.plugin.resource.ResourceMeta
import org.grails.plugin.resource.ResourceProcessor
import org.grails.plugin.resource.URLUtils

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Bootstrap contains code that must be execute during application (re)start
 */
class BootStrap {

    def grailsApplication

    def sequenceService
    def marshallersService
    def indexService
    def triggerService
    def grantService
    def termService
    def tableService
    def secUserService
    def noSQLCollectionService

    def retrieveErrorsService
    def bootstrapDataService

    def bootstrapUtilsService
    def bootstrapOldVersionService

    def dataSource
    def sessionFactory

    def cytomineMailService



    def init = { servletContext ->

        //Register API Authentifier
        SpringSecurityUtils.clientRegisterFilter( 'apiAuthentificationFilter', SecurityFilterPosition.DIGEST_AUTH_FILTER.order + 1)

        log.info "#############################################################################"
        log.info "#############################################################################"
        log.info "#############################################################################"
        String cytomineWelcomMessage = """
                   _____      _                  _
                  / ____|    | |                (_)
                 | |    _   _| |_ ___  _ __ ___  _ _ __   ___
                 | |   | | | | __/ _ \\| '_ ` _ \\| | '_ \\ / _ \\
                 | |___| |_| | || (_) | | | | | | | | | |  __/
                  \\_____\\__, |\\__\\___/|_| |_| |_|_|_| |_|\\___|
                 |  _ \\  __/ |     | |     | |
                 | |_) ||___/  ___ | |_ ___| |_ _ __ __ _ _ __
                 |  _ < / _ \\ / _ \\| __/ __| __| '__/ _` | '_ \\
                 | |_) | (_) | (_) | |_\\__ \\ |_| | | (_| | |_) |
                 |____/ \\___/ \\___/ \\__|___/\\__|_|  \\__,_| .__/
                                                         | |
                                                         |_|
        """
        log.info cytomineWelcomMessage
        log.info "#############################################################################"
        log.info "#############################################################################"
        log.info "#############################################################################"
        log.info "Current version " + grailsApplication.metadata.'app.version'
        log.info "Force version " + Holders.config.cytomine.forceVersion
        [
            "Environment" : Environment.getCurrent().name,
            "Server URL": grailsApplication.config.grails.serverURL,
            "Current directory": new File( './' ).absolutePath,
            "HeadLess: ": java.awt.GraphicsEnvironment.isHeadless(),
            "SQL": [url:Holders.config.dataSource.url, user:Holders.config.dataSource.username, password:Holders.config.dataSource.password, driver:Holders.config.dataSource.driverClassName],
            "NOSQL": [host:Holders.config.grails.mongo.host, port:Holders.config.grails.mongo.port, databaseName:Holders.config.grails.mongo.databaseName],
            "Datasource properties": servletContext.getAttribute(ApplicationAttributes.APPLICATION_CONTEXT).dataSourceUnproxied.properties,
            "JVM Args" : ManagementFactory.getRuntimeMXBean().getInputArguments()
        ].each {
            log.info "##### " + it.key + " = " + it.value
        }
        log.info "#############################################################################"
        log.info "#############################################################################"
        log.info "#############################################################################"

        if(Version.count()==0) {
            log.info "Version was not set, set to last version"
            try {
                Version.setCurrentVersion(grailsApplication.metadata.'app.version')
            } catch(NumberFormatException ex) {
                log.warn "Cannot parse version ${grailsApplication.metadata.'app.version'}, ignore version"
                String version = Holders.config.cytomine.forceVersion
                log.warn "Check forceVersion $version"
                Version.setCurrentVersion(version)
            }
        }

        if (!bootstrapUtilsService.checkSqlColumnExistence("sec_user", "language")) {
            new Sql(dataSource).executeUpdate("ALTER TABLE sec_user ADD COLUMN language VARCHAR;")
        }

        def test = SSLContext.getDefault().getSupportedSSLParameters()
        test.setProtocols(["TLSv1.2"] as String[]);
        System.setProperty("https.protocols", "TLSv1.2");

        //Initialize marshallers and services
        log.info "init marshaller..."
        marshallersService.initMarshallers()

        log.info "init sequences..."
        sequenceService.initSequences()

        log.info "init trigger..."
        triggerService.initTrigger()

        log.info "init index..."
        indexService.initIndex()

        log.info "init grant..."
        grantService.initGrant()

        log.info "init table..."
        tableService.initTable()

        log.info "init term service..."
        termService.initialize() //term service needs userservice and userservice needs termservice => init manualy at bootstrap

        log.info "init retrieve errors hack..."
        retrieveErrorsService.initMethods()

        /* Fill data just in test environment*/
        log.info "fill with data..."
        log.info grailsApplication.config.grails.adminPassword
        if (Environment.getCurrent() == Environment.TEST) {
            bootstrapDataService.initData()
            noSQLCollectionService.cleanActivityDB()
            println "grailsApplication.config.grails"
            println grailsApplication.config.grails
            println grailsApplication.config.grails.adminPassword
            def usersSamples = [
                    [username : Infos.ANOTHERLOGIN, firstname : 'Just another', lastname : 'User', email : grailsApplication.config.grails.admin.email, group : [[name : "Cytomine"]], password : grailsApplication.config.grails.adminPassword, color : "#FF0000", roles : ["ROLE_USER", "ROLE_ADMIN","ROLE_SUPER_ADMIN"]]
            ]
            bootstrapUtilsService.createUsers(usersSamples)

            mockServicesForTests()

        }  else if (SecUser.count() == 0) {
            //if database is empty, put minimal data
            bootstrapDataService.initData()
        }

        //set public/private keys for special image server user
        //keys regenerated at each deployment with Docker
        //if keys deleted from external config files for security, keep old keys
        if(grailsApplication.config.grails.ImageServerPrivateKey && grailsApplication.config.grails.ImageServerPublicKey) {
            SecUser imageServerUser = SecUser.findByUsernameIlike("ImageServer1")
            imageServerUser.setPrivateKey(grailsApplication.config.grails.ImageServerPrivateKey)
            imageServerUser.setPublicKey(grailsApplication.config.grails.ImageServerPublicKey)
            imageServerUser.save(flush : true)
        }
        if(grailsApplication.config.grails.rabbitMQPrivateKey && grailsApplication.config.grails.rabbitMQPublicKey) {
            SecUser rabbitMQUser = SecUser.findByUsernameIlike("rabbitmq")
            if(rabbitMQUser) {
                rabbitMQUser.setPrivateKey(grailsApplication.config.grails.rabbitMQPrivateKey)
                rabbitMQUser.setPublicKey(grailsApplication.config.grails.rabbitMQPublicKey)
                rabbitMQUser.save(flush : true)
            }
        }

        def softwareSourceDirectory = new File(grailsApplication.config.cytomine.software.path.softwareSources as String)
        if (!softwareSourceDirectory.exists() && !softwareSourceDirectory.mkdirs()) log.error "Software Sources folder doesn't exist"


        log.info "init change for old version..."
        try {
            bootstrapOldVersionService.execChangeForOldVersion()
        } catch(java.lang.NumberFormatException ex) {
            log.info "version cannot be parsed as x.y.z"
        }

        // Initialize RabbitMQ server
        bootstrapUtilsService.initRabbitMq()

        log.info "create multiple IS and Retrieval..."
        bootstrapUtilsService.createMultipleIS()
        bootstrapUtilsService.updateDefaultProcessingServer()

        bootstrapUtilsService.fillProjectConnections();
        bootstrapUtilsService.fillImageConsultations();

        fixPlugins()
    }

    private void mockServicesForTests(){
        //mock services which use IMS
        ImageGroupHDF5Service.metaClass.callIMSConversion = {
            SecUser currentUser, def imagesFilenames, String filename -> println "\n\n mocked callIMSConversion \n\n";
        }
        ImageServerService.metaClass.crop = {
            AbstractSlice slice, params, urlOnly, parametersOnly -> println "\n\n mocked crop \n\n";
                return Files.readAllBytes(Paths.get("test/functional/be/cytomine/utils/images/crop.jpg"));
        }
        ImageServerService.metaClass.storageSpace = {
            ImageServer is -> println "\n\n mocked storageSpace \n\n";
            return [used : 1, available: 1];
        }
        ImageServerService.metaClass.downloadUri = {
            AbstractImage ai -> println "\n\n mocked downloadUri \n\n";
                return "https://www.google.com";
        }
        //mock services which use Retrieval
        ImageRetrievalService.metaClass.doRetrievalIndex = {
            String url, String username, String password, def image,String id, String storage, Map<String,String> properties -> println "\n\n mocked doRetrievalIndex \n\n";
                return [code:200,response:"test"]
        }
        //mock mail service
        CytomineMailService.metaClass.send = {
            String from, String[] to, String cc, String subject, String message, def attachment -> println "\n\n mocked mail send \n\n";
        }

        ImagePropertiesService.metaClass.extractUseful = {
            AbstractImage abstractImage -> println "\n\n mocked extractUseful \n\n"; return null
        }
    }

    private void fixPlugins(){
        //grails resources
        //for https
        ResourceProcessor.metaClass.redirectToActualUrl = {
            ResourceMeta res, HttpServletRequest request, HttpServletResponse response ->
                String url
                if (URLUtils.isExternalURL(res.linkUrl)) {
                    url = res.linkUrl

                } else {
                    url = grailsApplication.config.grails.serverURL + request.contextPath + staticUrlPrefix + res.linkUrl
                }

                log.debug "Redirecting ad-hoc resource ${request.requestURI} " +
                        "to $url which makes it UNCACHEABLE - declare this resource " +
                        "and use resourceLink/module tags to avoid redirects and enable client-side caching"

                response.sendRedirect url
        }
    }
}
