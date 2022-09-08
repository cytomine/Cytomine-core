package be.cytomine.service.ontology;

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
import be.cytomine.domain.command.AddCommand;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.command.EditCommand;
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.image.group.ImageGroup;
import be.cytomine.domain.ontology.AnnotationGroup;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.InvalidRequestException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.ontology.AnnotationGroupRepository;
import be.cytomine.repository.ontology.AnnotationLinkRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
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
public class AnnotationGroupService extends ModelService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AnnotationGroupRepository annotationGroupRepository;

    @Autowired
    AnnotationLinkRepository annotationLinkRepository;

    @Override
    public Class currentDomain() {
        return AnnotationGroup.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new AnnotationGroup().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((AnnotationGroup) domain).getImageGroup().getName());
    }

    public Optional<AnnotationGroup> find(Long id) {
        Optional<AnnotationGroup> annotationGroup = annotationGroupRepository.findById(id);
        annotationGroup.ifPresent(group -> securityACLService.check(group.container(), READ));
        return annotationGroup;
    }

    public AnnotationGroup get(Long id) {
        return find(id).orElse(null);
    }

    public List<AnnotationGroup> list(ImageGroup group) {
        securityACLService.check(group, READ);
        return annotationGroupRepository.findAllByImageGroup(group);
    }

    public List<AnnotationGroup> list(Project project) {
        securityACLService.check(project, READ);
        return annotationGroupRepository.findAllByProject(project);
    }

    public CommandResponse add(JsonObject json) {
        transactionService.start();
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(json.getJSONAttrLong("project"), Project.class, READ);

        return executeCommand(new AddCommand(currentUser), null, json);
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), READ);

        return executeCommand(new EditCommand(currentUser, transaction), domain, jsonNewData);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), READ);

        return executeCommand(new DeleteCommand(currentUser, transaction), domain, null);
    }

    public CommandResponse merge(Long id, Long mergedId) {
        AnnotationGroup ag = get(id);
        AnnotationGroup agToMerge = get(mergedId);

        if (ag == null || agToMerge == null) {
            throw new ObjectNotFoundException("AnnotationGroup {} not found.", ag == null ? id : mergedId);
        }
        if (ag.getProject() != agToMerge.getProject() || ag.getImageGroup() != agToMerge.getImageGroup() || !ag.getType().equals(agToMerge.getType())) {
            throw new InvalidRequestException("Annotation groups " + id + " and " + mergedId + " are incompatible to be merged.");
        }

        annotationLinkRepository.setMergedAnnotationGroup(ag, agToMerge);
        annotationGroupRepository.delete(agToMerge);

        return executeCommand(new EditCommand(currentUserService.getCurrentUser(), null), ag, AnnotationGroup.getDataFromDomain(ag));
    }
}
