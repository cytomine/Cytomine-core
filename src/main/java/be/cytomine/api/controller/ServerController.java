package be.cytomine.api.controller;

import be.cytomine.config.ApplicationConfiguration;
import be.cytomine.service.CurrentUserService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("")
@Slf4j
@RequiredArgsConstructor
public class ServerController extends RestCytomineController {

    private final ApplicationConfiguration applicationConfiguration;

    private final CurrentUserService currentUserService;

    @PostMapping("/server/ping.json")
    public ResponseEntity<String> ping(@RequestBody JsonObject json,  HttpSession session) {
        log.debug("REST request to ping");
        JsonObject response = new JsonObject();
        response.put("alive", true);
        response.put("authenticated", SecurityUtils.isAuthenticated());
        response.put("version", applicationConfiguration.getVersion());
        response.put("serverURL", applicationConfiguration.getServerURL());
        response.put("serverID", "TODO"); //TODO!!

        if (SecurityUtils.isAuthenticated()) {
            response.put("user", currentUserService.getCurrentUser().getId());
            if (!currentUserService.getCurrentUser().getEnabled()) {
                log.info("Disabled user. Invalidation of its sessions");
                session.invalidate();
            }

            // TODO
//            def idProject = null
//            def idUser = data.user
//            if(!jsonContent.project.toString().equals("null")) {
//                idProject = Long.parseLong(jsonContent.project+"")
//            }
//            addLastConnection(idUser,idProject)
        }
        return ResponseEntity.ok(response.toJsonString());

        //{"alive":true,"authenticated":true,"version":"0.0.0","serverURL":"https://demo.cytomine.com","serverID":"938a336f-d600-48ac-9c3a-aaedc03a9f84","user":6399285}
    }


}
