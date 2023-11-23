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
import be.cytomine.domain.meta.ConfigurationReadingRole;
import be.cytomine.domain.processing.ImageFilter;
import be.cytomine.domain.processing.ImagingServer;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.processing.ImageFilterRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.service.amqp.AmqpQueueConfigService;
import be.cytomine.service.utils.Dataset;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;

import static be.cytomine.domain.ontology.RelationTerm.PARENT;

@Service
@Transactional
public class BootstrapDataService {

    @Autowired
    AmqpQueueConfigService amqpQueueConfigService;
    
    @Autowired
    BootstrapUtilsService bootstrapUtilsService;

    @Autowired
    Dataset dataset;

    @Autowired
    SecUserRepository secUserRepository;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    ImageFilterRepository imageFilterRepository;

    public void initData() {

        amqpQueueConfigService.initAmqpQueueConfigDefaultValues();

        bootstrapUtilsService.initRabbitMq();

        ImagingServer imagingServer = bootstrapUtilsService.returnOrCreateImagingServer();

        initImageFilters(imagingServer);


        bootstrapUtilsService.createMime("tif", "image/pyrtiff");
        bootstrapUtilsService.createMime("jp2", "image/jp2");
        bootstrapUtilsService.createMime("ndpi", "openslide/ndpi");
        bootstrapUtilsService.createMime("mrxs", "openslide/mrxs");
        bootstrapUtilsService.createMime("vms", "openslide/vms");
        bootstrapUtilsService.createMime("svs", "openslide/svs");
        bootstrapUtilsService.createMime("scn", "openslide/scn");
        bootstrapUtilsService.createMime("bif", "openslide/bif");
        bootstrapUtilsService.createMime("tif", "openslide/ventana");
        bootstrapUtilsService.createMime("tif", "philips/tif");

        bootstrapUtilsService.createRole("ROLE_USER");
        bootstrapUtilsService.createRole("ROLE_ADMIN");
        bootstrapUtilsService.createRole("ROLE_SUPER_ADMIN");
        bootstrapUtilsService.createRole("ROLE_GUEST");

        bootstrapUtilsService.createUser("admin", "Just an", "Admin", dataset.ADMINEMAIL, dataset.ADMINPASSWORD,  List.of("ROLE_USER", "ROLE_ADMIN"));
        bootstrapUtilsService.createUser("ImageServer1", "Image", "Server", dataset.ADMINEMAIL, RandomStringUtils.random(32).toUpperCase(), List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"));
        bootstrapUtilsService.createUser("superadmin", "Super", "Admin", dataset.ADMINEMAIL, dataset.ADMINPASSWORD,  List.of("ROLE_USER", "ROLE_ADMIN","ROLE_SUPER_ADMIN"));
        bootstrapUtilsService.createUser("rabbitmq", "rabbitmq", "user", dataset.ADMINEMAIL, RandomStringUtils.random(32).toUpperCase(),  List.of("ROLE_USER", "ROLE_SUPER_ADMIN"));
        bootstrapUtilsService.createUser("monitoring", "Monitoring", "Monitoring", dataset.ADMINEMAIL, RandomStringUtils.random(32).toUpperCase(),  List.of("ROLE_USER","ROLE_SUPER_ADMIN"));

        bootstrapUtilsService.createRelation(PARENT);

        bootstrapUtilsService.createConfigurations("WELCOME", "<p>Welcome to the Cytomine software.</p><p>This software is supported by the <a href='https://cytomine.coop'>Cytomine company</a></p>", ConfigurationReadingRole.ALL);
        bootstrapUtilsService.createConfigurations("admin_email", applicationProperties.getAdminEmail(), ConfigurationReadingRole.ADMIN);
//        bootstrapUtilsService.createConfigurations("notification_email", applicationConfiguration.getNotification().getEmail(), ConfigurationReadingRole.ADMIN);
//        bootstrapUtilsService.createConfigurations("notification_password", applicationConfiguration.getNotification().getPassword(), ConfigurationReadingRole.ADMIN);
//        bootstrapUtilsService.createConfigurations("notification_smtp_host", applicationConfiguration.getNotification().getSmtpHost(), ConfigurationReadingRole.ADMIN);
//        bootstrapUtilsService.createConfigurations("notification_smtp_port", applicationConfiguration.getNotification().getSmtpPort(), ConfigurationReadingRole.ADMIN);

//        SecUser admin = secUserRepository.findByUsernameLikeIgnoreCase("admin")
//                .orElseThrow(() -> new ObjectNotFoundException("admin user does not exists"));
//        admin.setPrivateKey(applicationConfiguration.getAdminPrivateKey());
//        admin.setPublicKey(applicationConfiguration.getAdminPublicKey());
//        secUserRepository.save(admin);

        changeUserKeys("admin", applicationProperties.getAdminPrivateKey(), applicationProperties.getAdminPublicKey());
        changeUserKeys("superadmin", applicationProperties.getSuperAdminPrivateKey(), applicationProperties.getSuperAdminPublicKey());

        bootstrapUtilsService.addDefaultProcessingServer();
        bootstrapUtilsService.addDefaultConstraints();

        changeUserKeys("rabbitmq", applicationProperties.getRabbitMQPrivateKey(), applicationProperties.getRabbitMQPublicKey());

        bootstrapUtilsService.createSoftwareUserRepository("GitHub", "cytomine", "cytomine","S_");
//        sur.save(failOnError: true))
    }

    public void initImageFilters(ImagingServer imagingServer) {

        List<Map<String, Object>> filters = List.of(
                Map.of("name", "Binary", "method","binary", "imagingServer", imagingServer, "available", true),
                Map.of("name", "Huang Threshold", "method","huang", "imagingServer", imagingServer, "available", false),
                Map.of("name", "Intermodes Threshold", "method","intermodes", "imagingServer", imagingServer, "available", false),
                Map.of("name", "IsoData Threshold", "method","isodata", "imagingServer", imagingServer, "available", true),
                Map.of("name", "Li Threshold", "method","li", "imagingServer", imagingServer, "available", false),
                Map.of("name", "Max Entropy Threshold", "method","maxentropy", "imagingServer", imagingServer, "available", false),
                Map.of("name", "Mean Threshold", "method","mean", "imagingServer", imagingServer, "available", true),
                Map.of("name", "Minimum Threshold", "method","minimum", "imagingServer", imagingServer, "available", true),
                Map.of("name", "MinError(I) Threshold", "method","minerror", "imagingServer", imagingServer, "available", false),
                Map.of("name", "Moments Threshold", "method","moments", "imagingServer", imagingServer, "available", false),
                Map.of("name", "Otsu Threshold", "method","otsu", "imagingServer", imagingServer, "available", true),
                Map.of("name", "Renyi Entropy Threshold", "method","renyientropy", "imagingServer", imagingServer, "available", false),
                Map.of("name", "Shanbhag Threshold", "method","shanbhag", "imagingServer", imagingServer, "available", false),
                Map.of("name", "Triangle Threshold", "method","triangle", "imagingServer", imagingServer, "available", false),
                Map.of("name", "Yen Threshold", "method","yen", "imagingServer", imagingServer, "available", true),
                Map.of("name", "Percentile Threshold", "method","percentile", "imagingServer", imagingServer, "available", false),
                Map.of("name", "H&E Haematoxylin", "method","he-haematoxylin", "imagingServer", imagingServer, "available", true),
                Map.of("name", "H&E Eosin", "method","he-eosin", "imagingServer", imagingServer, "available", true),
                Map.of("name", "HDAB Haematoxylin", "method","hdab-haematoxylin", "imagingServer", imagingServer, "available", true),
                Map.of("name", "HDAB DAB", "method","hdab-dab", "imagingServer", imagingServer, "available", true),
                Map.of("name", "Haematoxylin", "method","haematoxylin", "imagingServer", imagingServer, "available", false), //To be removed: does not exist
                Map.of("name", "Eosin", "method","eosin", "imagingServer", imagingServer, "available", false), //To be removed: does not exist
                Map.of("name", "Red (RGB)", "method","r_rgb", "imagingServer", imagingServer, "available", true),
                Map.of("name", "Green (RGB)", "method","g_rgb", "imagingServer", imagingServer, "available", true),
                Map.of("name", "Blue (RGB)", "method","b_rgb", "imagingServer", imagingServer, "available", true),
                Map.of("name", "Cyan (CMY)", "method","c_cmy", "imagingServer", imagingServer, "available", true),
                Map.of("name", "Magenta (CMY)", "method","m_cmy", "imagingServer", imagingServer, "available", true),
                Map.of("name", "Yellow (CMY)", "method","y_cmy", "imagingServer", imagingServer, "available", true)
        );

        for (Map<String, Object> filter : filters) {
            bootstrapUtilsService.createFilter((String)filter.get("name"), (String)filter.get("method"), (ImagingServer)filter.get("imagingServer"), (Boolean)filter.get("available"));
        }
    }

    private void changeUserKeys(String username, String privateKey, String publicKey) {
        SecUser user = secUserRepository.findByUsernameLikeIgnoreCase(username)
                .orElseThrow(() -> new ObjectNotFoundException(username + " user does not exists, cannot set its keys"));
        user.setPrivateKey(privateKey);
        user.setPublicKey(publicKey);
        secUserRepository.save(user);
    }

    public void updateImageFilters() {
        ImageFilter imageFilter = imageFilterRepository.findAll().stream().filter(x -> x.getName().equals("Binary")).findFirst().orElse(null);
        if (imageFilter!=null) {
            if (imageFilter.getMethod()==null) {
                // still old image filter data
                initImageFilters(bootstrapUtilsService.returnOrCreateImagingServer());
            }
        }
    }
}
