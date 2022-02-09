package be.cytomine.service.meta;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.Configuration;
import be.cytomine.domain.meta.ConfigurationReadingRole;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.meta.AttachedFileRepository;
import be.cytomine.repository.meta.ConfigurationRepository;
import be.cytomine.repository.ontology.AnnotationDomainRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@Transactional
public class ConfigurationService extends ModelService {

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private CurrentRoleService currentRoleService;

    @Override
    public Class currentDomain() {
        return Configuration.class;
    }

    public List<Configuration> list() {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkGuest(currentUser);
        if(currentRoleService.isAdminByNow(currentUser)) {
            return configurationRepository.findAll();
        } else if(currentRoleService.isUser(currentUser)) {
            return configurationRepository.findAllByReadingRoleIn(List.of(ConfigurationReadingRole.ALL, ConfigurationReadingRole.USER));
        } else {
            return configurationRepository.findAllByReadingRole(ConfigurationReadingRole.ALL);
        }
    }

    public Optional<Configuration> findByKey(String key) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());
        Optional<Configuration> config = configurationRepository.findByKey(key);
        config.ifPresent(this::checkPermission);
        return config;
    }

    private void checkPermission(Configuration config){
        SecUser currentUser = currentUserService.getCurrentUser();
        if(config.getReadingRole().equals(ConfigurationReadingRole.ALL)) {
            return;
        }
        if(currentRoleService.isAdminByNow(currentUser)) {
            return;
        }
        if(currentRoleService.isUser(currentUser)  && config.getReadingRole().equals(ConfigurationReadingRole.USER)) {
            return;
        }
        else throw new ForbiddenException("You don't have the right to read this resource!");
    }

    @Override
    public CommandResponse add(JsonObject jsonObject) {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        SecUser currentUser = currentUserService.getCurrentUser();
        return executeCommand(new AddCommand(currentUser),null,jsonObject);
    }


    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        SecUser currentUser = currentUserService.getCurrentUser();
        return executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkAdmin(currentUser);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Configuration().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        Configuration configuration = (Configuration)domain;
        return Arrays.asList(configuration.getKey());
    }

}
