package be.cytomine.service.database;

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
import be.cytomine.domain.image.Mime;
import be.cytomine.domain.meta.Configuration;
import be.cytomine.domain.meta.ConfigurationReadingRole;
import be.cytomine.domain.processing.ImageFilter;
import be.cytomine.domain.security.*;
import be.cytomine.repository.image.MimeRepository;
import be.cytomine.repository.meta.ConfigurationRepository;
import be.cytomine.repository.ontology.RelationRepository;
import be.cytomine.repository.processing.ImageFilterRepository;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.image.server.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class BootstrapUtilsService {

    @Autowired
    SecRoleRepository secRoleRepository;

    @Autowired
    SecUserSecRoleRepository secSecUserSecRoleRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    StorageService storageService;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    RelationRepository relationRepository;

    @Autowired
    ImageFilterRepository imageFilterRepository;

    @Autowired
    MimeRepository mimeRepository;

    @Autowired
    ConfigurationRepository configurationRepository;

    @Autowired
    EntityManager entityManager;

    public void createRole(String role) {
        secRoleRepository.createIfNotExist(role);
    }

    public void createUser(String username, String firstname, String lastname, List<String> roles) {
        if (userRepository.findByUsernameLikeIgnoreCase(username).isEmpty()) {
            log.info("Creating {}...", username);
            User user = new User();
            user.setUsername(username);
            user.setName(firstname + " " + lastname);
            user.setReference(UUID.randomUUID().toString());
            user.generateKeys();

            log.info("Saving {}...", user.getUsername());
            user = userRepository.save(user);

            for (String role : roles) {
                SecUserSecRole secSecUserSecRole = new SecUserSecRole();
                secSecUserSecRole.setSecRole(secRoleRepository.getByAuthority(role));
                secSecUserSecRole.setSecUser(user);
                secSecUserSecRoleRepository.save(secSecUserSecRole);
            }

            storageService.initUserStorage(user);
        }
    }

    public void createRelation(String name) {
        relationRepository.createIfNotExist(name);
    }

    public void createFilter(String name, String method, Boolean available) {
        ImageFilter filter = imageFilterRepository.findByName(name)
                .orElseGet(ImageFilter::new);
        filter.setName(name);
        filter.setMethod(method);
        filter.setAvailable(available);
        imageFilterRepository.save(filter);
    }

    public void createMime(String extension, String mimeType) {
        if (mimeRepository.findByMimeType(mimeType).isEmpty()) {
            Mime mime = new Mime();
            mime.setMimeType(mimeType);
            mime.setExtension(extension);
            mimeRepository.save(mime);
        }
    }



    public void createConfigurations(String key, String value, ConfigurationReadingRole readingRole){
        Configuration configuration = new Configuration();
        configuration.setKey(key);
        configuration.setValue(value);
        configuration.setReadingRole(readingRole);
        configurationRepository.save(configuration);
    }


    // TODO: required?

//    def mongo
//    def noSQLCollectionService
//    def imageConsultationService
//    void fillProjectConnections() {
//        SpringSecurityUtils.doWithAuth("superadmin", {
//                Date before = new Date();
//
//        def connections = PersistentProjectConnection.findAllByTimeIsNullOrCountCreatedAnnotationsIsNullOrCountViewedImagesIsNull(sort: 'created', order: 'desc', max: Integer.MAX_VALUE)
//        log.info "project connections to update " + connections.size()
//
//        def sql = new Sql(dataSource)
//
//        for (PersistentProjectConnection projectConnection : connections) {
//            Date after = projectConnection.created;
//
//            // collect {it.created.getTime} is really slow. I just want the getTime of PersistentConnection
//            def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
//            def lastConnection = db.persistentConnection.aggregate(
//                    [$match: [project: projectConnection.project, user: projectConnection.user, $and : [[created: [$gte: after]],[created: [$lte: before]]]]],
//                        [$sort: [created: 1]],
//                        [$project: [dateInMillis: [$subtract: ['$created', new Date(0L)]]]]
//                );
//
//            def continuousConnections = lastConnection.results().collect { it.dateInMillis }
//
//            //we calculate the gaps between connections to identify the period of non activity
//            def continuousConnectionIntervals = []
//
//            continuousConnections.inject(projectConnection.created.time) { result, i ->
//                    continuousConnectionIntervals << (i-result)
//                i
//            }
//
//            projectConnection.time = continuousConnectionIntervals.split{it < 30000}[0].sum()
//            if(projectConnection.time == null) projectConnection.time=0;
//
//            // count viewed images
//            projectConnection.countViewedImages = imageConsultationService.getImagesOfUsersByProjectBetween(projectConnection.user, projectConnection.project,after, before).size()
//
//            db.persistentImageConsultation.update(
//                    [$and :[ [project:projectConnection.project],[user:projectConnection.user],[created:[$gte:after]],[created:[$lte:before]]]],
//                        [$set: [projectConnection: projectConnection.id]])
//
//            // count created annotations
//            String request = "SELECT COUNT(*) FROM user_annotation a WHERE a.project_id = ${projectConnection.project} AND a.user_id = ${projectConnection.user} AND a.created < '${before}' AND a.created > '${after}'"
//
//            sql.eachRow(request) {
//                projectConnection.countCreatedAnnotations = it[0];
//            }
//
//            projectConnection.save(flush : true, failOnError: true)
//            before = projectConnection.created
//        }
//        sql.close()
//        });
//    }
//    void fillImageConsultations() {
//        SpringSecurityUtils.doWithAuth("superadmin", {
//                Date before = new Date();
//
//        def consultations = PersistentImageConsultation.findAllByTimeIsNullOrCountCreatedAnnotationsIsNull(sort: 'created', order: 'desc', max: Integer.MAX_VALUE)
//        log.info "image consultations to update " + consultations.size()
//
//        def sql = new Sql(dataSource)
//
//        for (PersistentImageConsultation consultation : consultations) {
//            Date after = consultation.created;
//
//            // collect {it.created.getTime} is really slow. I just want the getTime of PersistentConnection
//            def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
//            def positions = db.persistentUserPosition.aggregate(
//                    [$match: [project: consultation.project, user: consultation.user, image: consultation.image, $and : [[created: [$gte: after]],[created: [$lte: before]]]]],
//                        [$sort: [created: 1]],
//                        [$project: [dateInMillis: [$subtract: ['$created', new Date(0L)]]]]
//                );
//
//            def continuousConnections = positions.results().collect { it.dateInMillis }
//
//            //we calculate the gaps between connections to identify the period of non activity
//            def continuousConnectionIntervals = []
//
//            continuousConnections.inject(consultation.created.time) { result, i ->
//                    continuousConnectionIntervals << (i-result)
//                i
//            }
//
//            consultation.time = continuousConnectionIntervals.split{it < 30000}[0].sum()
//            if(consultation.time == null) consultation.time=0;
//
//            // count created annotations
//            String request = "SELECT COUNT(*) FROM user_annotation a WHERE " +
//                    "a.project_id = ${consultation.project} " +
//                    "AND a.user_id = ${consultation.user} " +
//                    "AND a.image_id = ${consultation.image} " +
//                    "AND a.created < '${before}' AND a.created > '${after}'"
//
//            sql.eachRow(request) {
//                consultation.countCreatedAnnotations = it[0];
//            }
//
//            consultation.save(flush : true, failOnError: true)
//            before = consultation.created
//        }
//        sql.close()
//        });
//    }


}
