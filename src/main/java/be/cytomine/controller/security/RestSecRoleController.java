package be.cytomine.controller.security;

import be.cytomine.controller.RestCytomineController;
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.search.UserSearchExtension;
import be.cytomine.service.security.SecRoleService;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestSecRoleController extends RestCytomineController {

    private final SecRoleService secRoleService;

    @GetMapping("/role.json")
    public ResponseEntity<String> list() {
        log.debug("REST request to list roles");
        return responseSuccess(secRoleService.list());
    }

    @GetMapping("/role/{id}.json")
    public ResponseEntity<String> get(@PathVariable Long id) {
        log.debug("REST request to list roles");
        return responseSuccess(secRoleService.find(id).orElseThrow(() -> new ObjectNotFoundException("SecRole", id)));
    }
}
