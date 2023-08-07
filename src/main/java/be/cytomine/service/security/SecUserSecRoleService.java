package be.cytomine.service.security;

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
import be.cytomine.domain.command.Command;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.SecUserSecRole;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.security.SecRoleRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.SecurityUtils;
import be.cytomine.utils.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class SecUserSecRoleService extends ModelService {


    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    SecUserSecRoleRepository secUserSecRoleRepository;

    @Autowired
    CurrentUserService currentUserService;

    @Autowired
    SecRoleRepository secRoleRepository;

    @Autowired
    SecUserRepository secUserRepository;
    @Autowired
    private StorageService storageService;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Class currentDomain() {
        return SecUserSecRoleService.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new SecUserSecRole().buildDomainFromJson(json, getEntityManager());
    }

    public List<SecUserSecRole> list(User user) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());
        return secUserSecRoleRepository.findAllBySecUser(user);
    }

    public SecRole getHighest(User user) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());

        List<SecUserSecRole> secUserSecRoles = secUserSecRoleRepository.findAllBySecUser(user);

        Optional<SecUserSecRole> role_super_admin = secUserSecRoles.stream()
                .filter(x -> x.getSecRole().getAuthority().equals("ROLE_SUPER_ADMIN")).findFirst();
        if (role_super_admin.isPresent()) {
            return role_super_admin.get().getSecRole();
        }

        Optional<SecUserSecRole> role_admin = secUserSecRoles.stream()
                .filter(x -> x.getSecRole().getAuthority().equals("ROLE_ADMIN")).findFirst();
        if (role_admin.isPresent()) {
            return role_admin.get().getSecRole();
        }

        Optional<SecUserSecRole> role_user = secUserSecRoles.stream()
                .filter(x -> x.getSecRole().getAuthority().equals("ROLE_USER")).findFirst();
        if (role_user.isPresent()) {
            return role_user.get().getSecRole();
        }

        Optional<SecUserSecRole> role_guest = secUserSecRoles.stream()
                .filter(x -> x.getSecRole().getAuthority().equals("ROLE_GUEST")).findFirst();
        if (role_guest.isPresent()) {
            return role_guest.get().getSecRole();
        }
        return null;
    }


    public Optional<SecUserSecRole> find(SecUser user, SecRole role) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());
        return secUserSecRoleRepository.findBySecUserAndSecRole(user, role);
    }

    @Override
    public CommandResponse add(JsonObject jsonObject) {
        SecUser currentUser = currentUserService.getCurrentUser();
        SecRole role = secRoleRepository.findById(jsonObject.getJSONAttrLong("role"))
                .orElseThrow(() -> new ObjectNotFoundException("Role", jsonObject.getJSONAttrStr("role")));
        SecUser user = secUserRepository.findById(jsonObject.getJSONAttrLong("user"))
                .orElseThrow(() -> new ObjectNotFoundException("User", jsonObject.getJSONAttrStr("user")));
        Set<String> userRoles = secUserSecRoleRepository.findAllBySecUser(user).stream().map(x -> x.getSecRole().getAuthority())
                .collect(Collectors.toSet());

        if (role.getAuthority().equals("ROLE_ADMIN") || role.getAuthority().equals("ROLE_SUPER_ADMIN")) {
            securityACLService.checkAdmin(currentUser);
        } else if (userRoles.size() == 1 && userRoles.contains("ROLE_GUEST")) {
            securityACLService.checkAdmin(currentUser);
        } else {
            securityACLService.checkUser(currentUser);
        }


        return executeCommand(new AddCommand(currentUser), null, jsonObject);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        SecUser currentUser = currentUserService.getCurrentUser();
        SecUserSecRole secUserSecRole = (SecUserSecRole) domain;
        if (Objects.equals(secUserSecRole.getSecUser().getId(), currentUser.getId()) && !secUserSecRole.getSecRole().getAuthority().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("You cannot remove you a role");
        }
        if (secUserSecRole.getSecUser().isAlgo()) {
            //TODO
//            Job job = ((UserJob)domain.secUser).job
//            securityACLService.check(job?.container(),READ)
        } else {
            securityACLService.checkAdmin(currentUser);
        }
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c, domain, null);
    }


    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        SecUserSecRole secUserSecRole = (SecUserSecRole) domain;
        return List.of(secUserSecRole.getSecUser().getId(), secUserSecRole.getSecRole().getId());
    }


    /**
     * Define a role for a user. If admin is defined, user will have admin,user,guest. If user is defined, user will have user,guest, etc
     */
    public void define(SecUser user, SecRole role) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkAdmin(currentUser);

        //if user has only guest role, and we want to add a new role, we are creating a storage for him
        AtomicBoolean createStorage = new AtomicBoolean(false);
        if (user.getRoles().size() == 1 && user.getRoles().stream().anyMatch(x -> x.getAuthority().equals("ROLE_GUEST")) && !role.getAuthority().equals("ROLE_GUEST")) {
            log.debug("User {} has guest role and we are assigning {}", user.getUsername(), role.getAuthority());
            List<Storage> storageList = storageService.list(user, "");
            if (storageList.isEmpty()) {
                log.debug("No storage found for user: {}", user.getUsername());
                createStorage.set(true);
            } else {
                for (Storage storage : storageList) {
                    if (storage.getUser().equals(user)) {
                        log.debug("Found user's : {} own storage", user.getUsername());
                        createStorage.set(false);
                        break;
                    } else {
                        createStorage.set(true);
                    }
                }

            }
        }

        SecRole roleGuest = secRoleRepository.getByAuthority("ROLE_GUEST");
        SecRole roleUser = secRoleRepository.getByAuthority("ROLE_USER");
        SecRole roleAdmin = secRoleRepository.getByAuthority("ROLE_ADMIN");
        SecRole roleSuperAdmin = secRoleRepository.getByAuthority("ROLE_SUPER_ADMIN");

        switch (role.getAuthority()) {
            case "ROLE_SUPER_ADMIN":
                addRole(user, roleGuest);
                addRole(user, roleUser);
                addRole(user, roleAdmin);
                addRole(user, roleSuperAdmin);
                break;
            case "ROLE_ADMIN":
                addRole(user, roleGuest);
                addRole(user, roleUser);
                addRole(user, roleAdmin);
                removeRole(user, roleSuperAdmin);
                break;
            case "ROLE_USER":
                addRole(user, roleGuest);
                addRole(user, roleUser);
                removeRole(user, roleAdmin);
                removeRole(user, roleSuperAdmin);
                break;
            case "ROLE_GUEST":
                addRole(user, roleGuest);
                removeRole(user, roleUser);
                removeRole(user, roleAdmin);
                removeRole(user, roleSuperAdmin);
                break;
        }

        if (createStorage.get()) {
            log.debug("Creating storage for user: {}", user.getUsername());
            SecurityUtils.doWithAuth(applicationContext, "admin", () -> storageService.createStorage(user));
        }
    }


    private void addRole(SecUser user, SecRole role) {
        log.debug("Adding role {} to user {}", role.getAuthority(), user.getUsername());
        Optional<SecUserSecRole> linked = secUserSecRoleRepository.findBySecUserAndSecRole(user, role);
        if (linked.isEmpty()) {
            SecUserSecRole susr = new SecUserSecRole();
            susr.setSecRole(role);
            susr.setSecUser(user);
            super.saveDomain(susr);
            log.debug("Role {} added to user {}", role.getAuthority(), user.getUsername());
        }
    }

    private void removeRole(SecUser user, SecRole role) {
        log.debug("Removing role {} to user {}", role.getAuthority(), user.getUsername());
        Optional<SecUserSecRole> linked = secUserSecRoleRepository.findBySecUserAndSecRole(user, role);
        if (linked.isPresent()) {
            if (Objects.equals(user.getId(), currentUserService.getCurrentUser().getId()) && !role.getAuthority().equals("ROLE_SUPER_ADMIN")) {
                throw new ForbiddenException("You cannot remove you a role");
            }
            super.removeDomain(linked.get());
            log.debug("Role {} removed to user {}", role.getAuthority(), user.getUsername());
        }
    }

    /**
     * Retrieve domain thanks to a JSON object
     *
     * @return domain retrieve thanks to json
     */
    @Override
    public CytomineDomain retrieve(JsonObject json) {

        SecUser secUser = secUserRepository.getById(json.getJSONAttrLong("user"));
        SecRole secRole = secRoleRepository.getById(json.getJSONAttrLong("role"));
        return secUserSecRoleRepository.findBySecUserAndSecRole(secUser, secRole)
                .orElseThrow(() -> new ObjectNotFoundException("SecUserSecRole", json.toJsonString()));
    }


    public void checkDoNotAlreadyExist(CytomineDomain domain) {
        SecUserSecRole secUserSecRole = (SecUserSecRole) domain;
        if (domain != null) {
            if (secUserSecRoleRepository.findBySecUserAndSecRole(secUserSecRole.getSecUser(), secUserSecRole.getSecRole())
                    .stream().anyMatch(x -> !Objects.equals(x.getId(), secUserSecRole.getId()))) {
                throw new AlreadyExistException("User " + secUserSecRole.getSecUser().getUsername() + " has already role " + secUserSecRole.getSecRole().getAuthority());
            }
        }
    }

}
