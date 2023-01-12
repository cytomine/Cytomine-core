package be.cytomine.api.controller;

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

import be.cytomine.api.JsonResponseEntity;
import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.social.LastConnection;
import be.cytomine.domain.social.PersistentConnection;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import be.cytomine.repositorynosql.social.PersistentConnectionRepository;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.security.jwt.TokenType;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.EndOfLifeUtils;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@RestController
@RequestMapping("")
@Slf4j
@RequiredArgsConstructor
public class ServerController extends RestCytomineController {

    private final ApplicationProperties applicationProperties;

    private final CurrentUserService currentUserService;

    private final SequenceService sequenceService;

    private final PersistentConnectionRepository persistentConnectionRepository;

    private final LastConnectionRepository lastConnectionRepository;

    private final TokenProvider tokenProvider;

    private final CurrentRoleService currentRoleService;

    //@Secured("IS_AUTHENTICATED_REMEMBERED") //TODO????
    @RequestMapping(value = {"/server/ping.json", "/server/ping"}, method = {RequestMethod.GET, RequestMethod.POST}) // without.json is deprecated
    public ResponseEntity<String> ping(HttpSession session) throws IOException {
        log.debug("REST request to ping");
        JsonObject json = super.mergeQueryParamsAndBodyParams();
        JsonObject response = new JsonObject();
        response.put("alive", true);
        response.put("authenticated", SecurityUtils.isAuthenticated());
        response.put("version", applicationProperties.getVersion());
        response.put("serverURL", applicationProperties.getServerURL());
        response.put("serverID", applicationProperties.getServerId());
        response.put("ltiEnabled", applicationProperties.getAuthentication().getLti().isEnabled());
        response.put("ldapEnabled", applicationProperties.getAuthentication().getLdap().getEnabled());

        if (SecurityUtils.isAuthenticated()) {
            SecUser user = currentUserService.getCurrentUser();
            response.put("user", user.getId());
            response.put("shortTermToken", tokenProvider.createToken(SecurityContextHolder.getContext().getAuthentication(), TokenType.SHORT_TERM));

            if (!user.getEnabled()) {
                log.info("Disabled user. Invalidation of its sessions");
                session.invalidate();
            }
            Long idProject = null;
            if(!json.getJSONAttrStr("project", "null").equals("null")) {
                idProject = json.getJSONAttrLong("project");
            }
            addLastConnection(user, idProject);
        }
        return JsonResponseEntity.status(HttpStatus.OK).body(response.toJsonString());

        //{"alive":true,"authenticated":true,"version":"0.0.0","serverURL":"https://demo.cytomine.com","serverID":"938a336f-d600-48ac-9c3a-aaedc03a9f84","user":6399285}
    }

    void addLastConnection(SecUser user, Long idProject) {
        try {
            LastConnection connection = new LastConnection();
            connection.setId(sequenceService.generateID());
            connection.setUser(user.getId());
            connection.setDate(new Date());
            connection.setCreated(connection.getDate());
            connection.setProject(idProject);
            lastConnectionRepository.insert(connection); //don't use save (stateless collection)

            PersistentConnection connectionPersist = new PersistentConnection();
            connectionPersist.setId(sequenceService.generateID());
            connectionPersist.setUser(user.getId());
            connectionPersist.setProject(idProject);
            connectionPersist.setCreated(new Date());
            connectionPersist.setSession(RequestContextHolder.currentRequestAttributes().getSessionId());
            persistentConnectionRepository.insert(connectionPersist); //don't use save (stateless collection)
        } catch (NonTransientDataAccessException e) {
            log.error(e.getMessage());
        }
    }

    @GetMapping("/status.json")
    public ResponseEntity<String> status() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("alive", true);
        jsonObject.put("version", applicationProperties.getVersion());
        jsonObject.put("serverURL", applicationProperties.getServerURL());
        if (SecurityUtils.isAuthenticated() && currentRoleService.isAdmin(currentUserService.getCurrentUser())) {
            jsonObject.put("endOfLife", EndOfLifeUtils.END_OF_LIFE.format(DateTimeFormatter.ISO_DATE_TIME));
        }
        return ResponseEntity.ok(jsonObject.toJsonString());
    }

}
