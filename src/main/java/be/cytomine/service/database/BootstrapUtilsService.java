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
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;

@Service
@Slf4j
@Transactional
public class BootstrapUtilsService {

    @Autowired
    SecRoleRepository secRoleRepository;

    @Autowired
    SecUserSecRoleRepository secUserSecRoleRepository;

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

    @Autowired
    SecUserRepository secUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void createRole(String role) {
        secRoleRepository.createIfNotExist(role);
    }

    public void createUser(String username, String firstname, String lastname, String email, String password, List<String> roles) {
        if (userRepository.findByUsernameLikeIgnoreCase(username).isEmpty()) {
            log.info("Creating {}...", username);
            User user = new User();
            user.setUsername(username);
            user.setFirstname(firstname);
            user.setLastname(lastname);
            user.setEmail(email);
            user.setPassword(password);
            user.encodePassword(passwordEncoder);
            user.setLanguage(Language.valueOf(applicationProperties.getDefaultLanguage()));
            user.setEnabled(true);
            user.setIsDeveloper(false);
            user.setOrigin("BOOTSTRAP");
            user.generateKeys();

            log.info("Saving {}...", user.getUsername());
            user = userRepository.save(user);

            for (String role : roles) {
                SecUserSecRole secUserSecRole = new SecUserSecRole();
                secUserSecRole.setSecRole(secRoleRepository.getByAuthority(role));
                secUserSecRole.setSecUser(user);
                secUserSecRoleRepository.save(secUserSecRole);
            }

            SecurityUtils.reauthenticate(applicationContext, "admin", "admin");

            storageService.initUserStorage(user);
        }
    }


    public void createUserJob(String username, String password, User creator, List<String> roles) {
        if (secUserRepository.findByUsernameLikeIgnoreCase(username).isEmpty()) {
            log.info("Creating {}...", username);
            UserJob user = new UserJob();
            user.setUsername(username);
            user.setPassword(password);
            user.setEnabled(true);
            user.setOrigin("BOOTSTRAP");
            user.setUser(creator);
            user.generateKeys();
            log.info("Saving {}...", user.getUsername());
            user = secUserRepository.save(user);

            for (String role : roles) {
                SecUserSecRole secUserSecRole = new SecUserSecRole();
                secUserSecRole.setSecRole(secRoleRepository.getByAuthority(role));
                secUserSecRole.setSecUser(user);
                secUserSecRoleRepository.save(secUserSecRole);
            }
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
}
