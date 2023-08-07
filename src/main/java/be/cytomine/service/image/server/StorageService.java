package be.cytomine.service.image.server;

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
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.repository.image.server.StorageRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.SecurityUtils;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.persistence.Tuple;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.springframework.security.acls.domain.BasePermission.*;

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

    private final SecUserRepository secUserRepository;

    public List<Storage> list() {
        securityACLService.checkUser(currentUserService.getCurrentUser());
        return securityACLService.getStorageList(currentUserService.getCurrentUser(), true);
    }

    public List<Storage> list(SecUser user, String searchString) {
        return list(user, false, searchString);
    }

    public List<Storage> list(SecUser user, Boolean adminByPass, String searchString) {
        securityACLService.checkUser(currentUserService.getCurrentUser());
        return securityACLService.getStorageList(user, adminByPass, searchString);
    }

    public List<JsonObject> usersStats(Storage storage, String sortColumn, String sortDirection) {
        Map<String, JsonObject> results = new HashMap<>();
        List<SecUser> users = secUserRepository.findAllUsersByStorageId(storage.getId());
        for (SecUser secUser : users) {
            if (secUser instanceof User user) {
                JsonObject usersData = new JsonObject();
                usersData.put("id", user.getId());
                usersData.put("username", user.getUsername());
                usersData.put("firstname", user.getFirstname());
                usersData.put("lastname", user.getLastname());
                usersData.put("fullName", user.getFirstname() + " " + user.getLastname());
                usersData.put("numberOfFiles", 0L);
                usersData.put("totalSize", 0L);
                usersData.put("created", user.getCreated());
                results.put(user.getUsername(), usersData);
            }

        }

        Map<String, Integer> usersPermissions = permissionService.listUsersAndPermissions(storage);

        for (Map.Entry<String, Integer> entry : usersPermissions.entrySet()) {
            if (results.containsKey(entry.getKey())) {
                results.get(entry.getKey()).put("role", permissionService.readStringFromMask(entry.getValue()));
            }
        }

        List<Tuple> stats = storageRepository.listStatsForUsers(storage.getId());

        for (Tuple stat : stats) {
            String username = stat.get(0, String.class);
            if (results.containsKey(username)) {
                results.get(username).put("numberOfFiles", stat.get(2, BigInteger.class).longValue());
                results.get(username).put("totalSize", stat.get(3, BigDecimal.class).longValue());
            }
        }

        List<JsonObject> resultsList = new ArrayList<>(results.values());
        if (sortColumn != null && sortDirection != null) {
            if (sortDirection.equalsIgnoreCase("desc")) {
                resultsList.sort(Comparator.comparing(o -> (Date) ((JsonObject) o).get(sortColumn)).reversed());
            } else {
                resultsList.sort(Comparator.comparing(o -> (Date) ((JsonObject) o).get(sortColumn)));
            }

        }

        return resultsList;
    }

    public List<JsonObject> userAccess(SecUser user) {
        securityACLService.checkUser(user);
        return securityACLService.getLightStoragesWithMaxPermission(user);
    }

    public Optional<Storage> find(Long id) {
        securityACLService.checkUser(currentUserService.getCurrentUser());
        Optional<Storage> Storage = storageRepository.findById(id);
        Storage.ifPresent(image -> securityACLService.check(image.container(), READ));
        return Storage;
    }

    public Storage get(Long id) {
        return find(id).orElse(null);
    }

    /**
     * Add the new domain with JSON data
     *
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        json.put("user", currentRoleService.isAdminByNow(currentUser) ? json.get("user") : currentUser.getId());
        return executeCommand(new AddCommand(currentUser), null, json);

    }

    protected void afterAdd(CytomineDomain domain, CommandResponse response) {
        log.info("Add permission on {} to {}", domain, ((Storage) domain).getUser().getUsername());
        Storage storage = (Storage) domain;
        if (!permissionService.hasACLPermission(storage, READ)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), READ);
        }
        if (!permissionService.hasACLPermission(storage, WRITE)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), WRITE);
        }
        if (!permissionService.hasACLPermission(storage, ADMINISTRATION)) {
            permissionService.addPermission(storage, storage.getUser().getUsername(), ADMINISTRATION);
        }
    }

    /**
     * Update this domain with new data from json
     *
     * @param domain      Domain to update
     * @param jsonNewData New domain datas
     * @return Response structure (new domain data, old domain data..)
     */
    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(), ADMINISTRATION);
        CommandResponse commandResponse = executeCommand(new EditCommand(currentUser, transaction), domain, jsonNewData);
        return commandResponse;
    }


    /**
     * Delete this domain
     *
     * @param domain       Domain to delete
     * @param transaction  Transaction link with this command
     * @param task         Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(), ADMINISTRATION);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c, domain, null);
    }

    public void initUserStorage(final SecUser user) {
        log.info("create storage for " + user.getUsername());
        final SecUser finalUser = user;
        SecurityUtils.doWithAuth(applicationContext, user.getUsername(), () -> createStorage(finalUser));
    }

    public CommandResponse createStorage(SecUser user) {
        return executeCommand(new AddCommand(user), null, JsonObject.of("name", user.getUsername() + " storage", "user", user.getId()));
    }


    @Override
    public Class currentDomain() {
        return Storage.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Storage().buildDomainFromJson(json, getEntityManager());
    }


    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((Storage) domain).getName());
    }
}
