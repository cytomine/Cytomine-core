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
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.project.Project;
import be.cytomine.repository.image.group.ImageGroupRepository;
import be.cytomine.service.ModelService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class ImageGroupService extends ModelService {

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private ImageGroupRepository imageGroupRepository;

    @Override
    public Class currentDomain() {
        return ImageGroup.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new ImageGroup().buildDomainFromJson(json, getEntityManager());
    }

    public Optional<ImageGroup> find(Long id) {
        Optional<ImageGroup> ImageGroup = imageGroupRepository.findById(id);
        ImageGroup.ifPresent(group -> securityACLService.check(group.container(), READ));
        return ImageGroup;
    }

    public List<ImageGroup> list(Project project) {
        securityACLService.check(project.container(), READ);
        return imageGroupRepository.findAllByProject(project);
    }
}
