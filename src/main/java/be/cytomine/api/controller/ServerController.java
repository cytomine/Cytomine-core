package be.cytomine.api.controller;

import be.cytomine.config.ApplicationConfiguration;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.social.LastConnection;
import be.cytomine.domain.social.PersistentConnection;
import be.cytomine.repositorynosql.social.LastConnectionRepository;
import be.cytomine.repositorynosql.social.PersistentConnectionRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.database.SequenceService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpSession;
import java.util.Date;

@RestController
@RequestMapping("")
@Slf4j
@RequiredArgsConstructor
public class ServerController extends RestCytomineController {

    private final ApplicationConfiguration applicationConfiguration;

    private final CurrentUserService currentUserService;

    private final SequenceService sequenceService;

    private final PersistentConnectionRepository persistentConnectionRepository;

    private final LastConnectionRepository lastConnectionRepository;

    //@Secured("IS_AUTHENTICATED_REMEMBERED") //TODO????
    @PostMapping("/server/ping.json")
    public ResponseEntity<String> ping(@RequestBody JsonObject json,  HttpSession session) {
        log.debug("REST request to ping");
        JsonObject response = new JsonObject();
        response.put("alive", true);
        response.put("authenticated", SecurityUtils.isAuthenticated());
        response.put("version", applicationConfiguration.getVersion());
        response.put("serverURL", applicationConfiguration.getServerURL());
        response.put("serverID", applicationConfiguration.getServerId());

        if (SecurityUtils.isAuthenticated()) {
            SecUser user = currentUserService.getCurrentUser();
            response.put("user", user.getId());
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
        return ResponseEntity.ok(response.toJsonString());

        //{"alive":true,"authenticated":true,"version":"0.0.0","serverURL":"https://demo.cytomine.com","serverID":"938a336f-d600-48ac-9c3a-aaedc03a9f84","user":6399285}
    }

    void addLastConnection(SecUser user, Long idProject) {
        try {
            LastConnection connection = new LastConnection();
            connection.setId(sequenceService.generateID());
            connection.setUser(user.getId());
            connection.setDate(new Date());
            connection.setProject(idProject);
            lastConnectionRepository.insert(connection); //don't use save (stateless collection)

            PersistentConnection connectionPersist = new PersistentConnection();
            connectionPersist.setId(sequenceService.generateID());
            connectionPersist.setUser(user.getId());
            connectionPersist.setProject(idProject);
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
        jsonObject.put("version", applicationConfiguration.getVersion());
        jsonObject.put("serverURL", applicationConfiguration.getServerURL());
        return ResponseEntity.ok(jsonObject.toJsonString());
    }

}
