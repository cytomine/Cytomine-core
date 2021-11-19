package be.cytomine.service.image.server;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ServerException;
import be.cytomine.repository.image.server.StorageRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.security.acls.domain.BasePermission.*;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageService extends ModelService {

    private final SecurityACLService securityACLService;

    private final ApplicationContext applicationContext;

    private final StorageRepository storageRepository;

    private final CurrentUserService currentUserService;

    private final CurrentRoleService currentRoleService;

    private final PermissionService permissionService;

    public List<Storage> list(SecUser user, String searchString) {
        return securityACLService.getStorageList(user, false, searchString);
    }


    public void initUserStorage(final SecUser user) {
        log.info ("create storage for " + user.getUsername());
        final SecUser finalUser = user;
        SecurityUtils.doWithAuth(applicationContext, user.getUsername(), () -> createStorage(finalUser));
//                {
//                Command c = new AddCommand(user);
//                executeCommand(c,null, JsonObject.of("name", user.getUsername() + " storage", "user", user.getId()));
//        });
    }

    public CommandResponse createStorage(SecUser user) {
        return executeCommand(new AddCommand(user),null, JsonObject.of("name", user.getUsername() + " storage", "user", user.getId()));
    }

    public Optional<Storage> find(Long id) {
        Optional<Storage> Storage = storageRepository.findById(id);
        Storage.ifPresent(image -> securityACLService.check(image.container(),READ));
        return Storage;
    }

    public Storage get(Long id) {
        return find(id).orElse(null);
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        json.put("user", currentRoleService.isAdminByNow(currentUser) ? json.get("user") : currentUser.getId());
        return executeCommand(new AddCommand(currentUser),null, json);

    }

    @Override
    public Class currentDomain() {
        return Storage.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Storage().buildDomainFromJson(json, getEntityManager());
    }

    protected void afterAdd(CytomineDomain domain, CommandResponse response) {
        log.info("Add permission on $domain to ${domain.user.username}");
        Storage storage = (Storage)domain;
        if(!permissionService.hasACLPermission(storage, READ)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), READ);
        }
        if(!permissionService.hasACLPermission(storage, WRITE)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), WRITE);
        }
        if(!permissionService.hasACLPermission(storage, ADMINISTRATION)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), ADMINISTRATION);
        }
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(), ADMINISTRATION);
        CommandResponse commandResponse = executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
        return commandResponse;
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(), ADMINISTRATION);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {

    }

    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((Storage)domain).getName());
    }
}
