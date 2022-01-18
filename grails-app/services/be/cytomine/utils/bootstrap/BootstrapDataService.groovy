package be.cytomine.utils.bootstrap

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

import be.cytomine.security.SecUser
import groovy.sql.Sql
import org.apache.commons.lang.RandomStringUtils
import be.cytomine.processing.SoftwareUserRepository

/**
 * Cytomine @ ULG
 * User: stevben
 * Date: 13/03/13
 * Time: 11:30
 */
class BootstrapDataService {

    def grailsApplication
    def bootstrapUtilsService
    def dataSource
    def amqpQueueConfigService

    def initData() {

        recreateTableFromNotDomainClass()
        amqpQueueConfigService.initAmqpQueueConfigDefaultValues()
        bootstrapUtilsService.initRabbitMq()

        def imagingServer = bootstrapUtilsService.createNewImagingServer()
        def filters = [
                [name: "Binary", baseUrl: "/vision/process?method=binary&url=", imagingServer: imagingServer],
                [name: "Huang Threshold", baseUrl: "/vision/process?method=huang&url=", imagingServer: imagingServer],
                [name: "Intermodes Threshold", baseUrl: "/vision/process?method=intermodes&url=", imagingServer: imagingServer],
                [name: "IsoData Threshold", baseUrl: "/vision/process?method=isodata&url=", imagingServer: imagingServer],
                [name: "Li Threshold", baseUrl: "/vision/process?method=li&url=", imagingServer: imagingServer],
                [name: "Max Entropy Threshold", baseUrl: "/vision/process?method=maxentropy&url=", imagingServer: imagingServer],
                [name: "Mean Threshold", baseUrl: "/vision/process?method=mean&url=", imagingServer: imagingServer],
                [name: "Minimum Threshold", baseUrl: "/vision/process?method=minimum&url=", imagingServer: imagingServer],
                [name: "MinError(I) Threshold", baseUrl: "/vision/process?method=minerror&url=", imagingServer: imagingServer],
                [name: "Moments Threshold", baseUrl: "/vision/process?method=moments&url=", imagingServer: imagingServer],
                [name: "Otsu Threshold", baseUrl: "/vision/process?method=otsu&url=", imagingServer: imagingServer],
                [name: "Renyi Entropy Threshold", baseUrl: "/vision/process?method=renyientropy&url=", imagingServer: imagingServer],
                [name: "Shanbhag Threshold", baseUrl: "/vision/process?method=shanbhag&url=", imagingServer: imagingServer],
                [name: "Triangle Threshold", baseUrl: "/vision/process?method=triangle&url=", imagingServer: imagingServer],
                [name: "Yen Threshold", baseUrl: "/vision/process?method=yen&url=", imagingServer: imagingServer],
                [name: "Percentile Threshold", baseUrl: "/vision/process?method=percentile&url=", imagingServer: imagingServer],
                [name: "H&E Haematoxylin", baseUrl: "/vision/process?method=he-haematoxylin&url=", imagingServer: imagingServer],
                [name: "H&E Eosin", baseUrl: "/vision/process?method=he-eosin&url=", imagingServer: imagingServer],
                [name: "HDAB Haematoxylin", baseUrl: "/vision/process?method=hdab-haematoxylin&url=", imagingServer: imagingServer],
                [name: "HDAB DAB", baseUrl: "/vision/process?method=hdab-dab&url=", imagingServer: imagingServer],
                [name: "Haematoxylin", baseUrl: "/vision/process?method=haematoxylin&url=", imagingServer: imagingServer],
                [name: "Eosin", baseUrl: "/vision/process?method=eosin&url=", imagingServer: imagingServer],
                [name: "Red (RGB)", baseUrl: "/vision/process?method=r_rgb&url=", imagingServer: imagingServer],
                [name: "Green (RGB)", baseUrl: "/vision/process?method=g_rgb&url=", imagingServer: imagingServer],
                [name: "Blue (RGB)", baseUrl: "/vision/process?method=b_rgb&url=", imagingServer: imagingServer],
                [name: "Cyan (CMY)", baseUrl: "/vision/process?method=c_cmy&url=", imagingServer: imagingServer],
                [name: "Magenta (CMY)", baseUrl: "/vision/process?method=m_cmy&url=", imagingServer: imagingServer],
                [name: "Yellow (CMY)", baseUrl: "/vision/process?method=y_cmy&url=", imagingServer: imagingServer],
        ]
        bootstrapUtilsService.createFilters(filters)

        def nativelySupportedMimes = [
                [extension : 'tif', mimeType : 'image/pyrtiff'],
                [extension : 'jp2', mimeType : 'image/jp2'],
                [extension : 'ndpi', mimeType : 'openslide/ndpi'],
                [extension : 'mrxs', mimeType : 'openslide/mrxs'],
                [extension : 'vms', mimeType : 'openslide/vms'],
                [extension : 'svs', mimeType : 'openslide/svs'],
                [extension : 'scn', mimeType : 'openslide/scn'],
                [extension : 'bif', mimeType : 'openslide/bif'],
                [extension : 'tif', mimeType : 'openslide/ventana'],
                [extension : 'tif', mimeType : 'philips/tif']
        ]
        bootstrapUtilsService.createMimes(nativelySupportedMimes)


        def usersSamples = [
                [username : 'ImageServer1', firstname : 'Image', lastname : 'Server', email : grailsApplication.config.grails.admin.email, group : [[name : "Cytomine"]], password : RandomStringUtils.random(32,  (('A'..'Z') + ('0'..'0')).join().toCharArray()), color : "#FF0000", roles : ["ROLE_USER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"]],
                [username : 'superadmin', firstname : 'Super', lastname : 'Admin', email : grailsApplication.config.grails.admin.email, group : [[name : "Cytomine"]], password : grailsApplication.config.grails.adminPassword, color : "#FF0000", roles : ["ROLE_USER", "ROLE_ADMIN","ROLE_SUPER_ADMIN"]],
                [username : 'admin', firstname : 'Just an', lastname : 'Admin', email : grailsApplication.config.grails.admin.email, group : [[name : "Cytomine"]], password : grailsApplication.config.grails.adminPassword, color : "#FF0000", roles : ["ROLE_USER", "ROLE_ADMIN"]],
                [username : 'rabbitmq', firstname : 'rabbitmq', lastname : 'user', email : grailsApplication.config.grails.admin.email, group : [[name : "Cytomine"]], password : RandomStringUtils.random(32,  (('A'..'Z') + ('0'..'0')).join().toCharArray()), color : "#FF0000", roles : ["ROLE_USER", "ROLE_SUPER_ADMIN"]],
                [username : 'monitoring', firstname : 'Monitoring', lastname : 'Monitoring', email : grailsApplication.config.grails.admin.email, group : [[name : "Cytomine"]], password : RandomStringUtils.random(32,  (('A'..'Z') + ('0'..'0')).join().toCharArray()), color : "#FF0000", roles : ["ROLE_USER","ROLE_SUPER_ADMIN"]]
        ]

        bootstrapUtilsService.createUsers(usersSamples)
        bootstrapUtilsService.createRelation()
        bootstrapUtilsService.createConfigurations(false)

        SecUser admin = SecUser.findByUsername("admin")
        if(!grailsApplication.config.grails.adminPrivateKey) {
            throw new IllegalArgumentException("adminPrivateKey must be set!")
        }
        if(!grailsApplication.config.grails.adminPublicKey) {
            throw new IllegalArgumentException("adminPublicKey must be set!")
        }
        admin.setPrivateKey((String) grailsApplication.config.grails.adminPrivateKey)
        admin.setPublicKey((String) grailsApplication.config.grails.adminPublicKey)
        admin.save(flush : true)

        SecUser superAdmin = SecUser.findByUsername("superadmin")
        if(!grailsApplication.config.grails.superAdminPrivateKey) {
            throw new IllegalArgumentException("superAdminPrivateKey must be set!")
        }
        if(!grailsApplication.config.grails.superAdminPublicKey) {
            throw new IllegalArgumentException("superAdminPublicKey must be set!")
        }
        superAdmin.setPrivateKey((String) grailsApplication.config.grails.superAdminPrivateKey)
        superAdmin.setPublicKey((String) grailsApplication.config.grails.superAdminPublicKey)
        superAdmin.save(flush : true)

        bootstrapUtilsService.addDefaultProcessingServer()
        bootstrapUtilsService.addDefaultConstraints()

        SecUser rabbitMQUser = SecUser.findByUsername("rabbitmq")
        if(!grailsApplication.config.grails.rabbitMQPrivateKey) {
            throw new IllegalArgumentException("rabbitMQPrivateKey must be set!")
        }
        if(!grailsApplication.config.grails.rabbitMQPublicKey) {
            throw new IllegalArgumentException("rabbitMQPublicKey must be set!")
        }
        rabbitMQUser.setPrivateKey(grailsApplication.config.grails.rabbitMQPrivateKey)
        rabbitMQUser.setPublicKey(grailsApplication.config.grails.rabbitMQPublicKey)
        rabbitMQUser.save(flush : true)

        SoftwareUserRepository sur = new SoftwareUserRepository(provider : "GitHub", username: "cytomine", dockerUsername : "cytomine", prefix : "S_")
        sur.save(failOnError: true)
    }

    public void recreateTableFromNotDomainClass() {
        new Sql(dataSource).executeUpdate("DROP TABLE IF EXISTS  task_comment")
        new Sql(dataSource).executeUpdate("DROP TABLE IF EXISTS  task")

        new Sql(dataSource).executeUpdate("CREATE TABLE task (id bigint,progress bigint,project_id bigint,user_id bigint,print_in_activity boolean)")
        new Sql(dataSource).executeUpdate("CREATE TABLE task_comment (task_id bigint,comment character varying(255),timestamp bigint)")
    }

}
