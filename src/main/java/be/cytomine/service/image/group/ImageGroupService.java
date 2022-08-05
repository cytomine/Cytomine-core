package be.cytomine.service.image.group;

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

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.image.group.ImageGroupImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.repository.image.group.ImageGroupImageInstanceRepository;
import be.cytomine.repository.image.group.ImageGroupRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.UrlApi;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@Slf4j
@Service
@Transactional
public class ImageGroupService extends ModelService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ImageGroupImageInstanceService imageGroupImageInstanceService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ImageGroupRepository imageGroupRepository;

    @Autowired
    private ImageGroupImageInstanceRepository imageGroupImageInstanceRepository;

    @Override
    public Class currentDomain() {
        return ImageGroup.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new ImageGroup().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((ImageGroup) domain).getName(), ((ImageGroup) domain).getProject().getName());
    }

    public Optional<ImageGroup> find(Long id) {
        Optional<ImageGroup> imageGroup = imageGroupRepository.findById(id);
        imageGroup.ifPresent(group -> securityACLService.check(group.container(), READ));
        return imageGroup;
    }

    public ImageGroup get(Long id) {
        return find(id).orElse(null);
    }

    public List<ImageGroup> list(Project project) {
        securityACLService.check(project, READ);

        List<ImageGroup> groups = imageGroupRepository.findAllByProject(project);
        for (ImageGroup group : groups) {
            List<Object> images = new ArrayList<>();
            for (ImageGroupImageInstance igii : imageGroupImageInstanceService.list(group)) {
                images.add(Map.of(
                        "id", igii.getImage().getId(),
                        "thumb", UrlApi.getImageInstanceThumbUrlWithMaxSize(igii.getImage().getId()),
                        "width", igii.getImage().getBaseImage().getWidth(),
                        "height", igii.getImage().getBaseImage().getHeight()
                ));
            }

            group.setImages(images);
        }

        return groups;
    }

    public CommandResponse add(JsonObject json) {
        transactionService.start();
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);

        return executeCommand(new AddCommand(currentUser), null, json);
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), WRITE);

        return executeCommand(new EditCommand(currentUser, transaction), domain, jsonNewData);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), WRITE);

        return executeCommand(new DeleteCommand(currentUser, transaction), domain, null);
    }

    protected void beforeDelete(CytomineDomain domain) {
        ImageGroup group = (ImageGroup) domain;

        imageGroupImageInstanceRepository.deleteAllByGroup(group);
    }
}
