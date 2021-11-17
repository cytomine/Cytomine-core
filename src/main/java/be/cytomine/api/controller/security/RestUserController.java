package be.cytomine.api.controller.security;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.service.security.SecUserService;
import be.cytomine.utils.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestUserController extends RestCytomineController {

    private final SecUserService secUserService;

    @GetMapping("/user/{id}")
    public ResponseEntity<String> getUser(
            @PathVariable String id
    ) {
        log.debug("REST request to get User : {}", id);

        Optional<SecUser> user = secUserService.find(id);
        if (user.isEmpty()) {
            user = secUserService.findByUsername(id);
        }

        return user.map( secUser -> {
            JsonObject object = User.getDataFromDomain(secUser);
            //TODO
//            JsonObject  authMaps = secUserService.getAuth(user);
//            object.put("admin", authMaps.get("admin"));
//            object.put("user", authMaps.get("user"));
//            object.put("guest", authMaps.get("guest"));
            return ResponseEntity.ok(convertObjectToJSON(object));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseNotFound("User", id).toJsonString()));
    }

    @GetMapping("/user/current.{ext}")
    public ResponseEntity<String> getCurrentUser(
    ) {
        log.debug("REST request to get current User");

        Optional<SecUser> user = secUserService.findCurrentUser();
        //TODO: admin, user, guest, adminByNow...
        return user.map( secUser -> {
            JsonObject object = User.getDataFromDomain(secUser);
            return ResponseEntity.ok(convertObjectToJSON(object));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseNotFound("User", "current").toJsonString()));
    }


    /**
     * Add a new user
     */
    @PostMapping("/user")
    public ResponseEntity<String> createUser(@RequestBody JsonObject json) {
        log.debug("REST request to save User : " + json);
        return add(secUserService, json);
    }

    @PutMapping("/user/{id}")
    public ResponseEntity<String> updateUser(@PathVariable String id, @RequestBody JsonObject json) {
        log.debug("REST request to update User : {}", id);
        return update(secUserService, json);
    }

    /**
     * Delete the "login" User.
     */
    @DeleteMapping("/user/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable String id) {
        log.debug("REST request to delete User: {}", id);
        return delete(secUserService, JsonObject.of("id", Long.parseLong(id)), null);
    }



//    @GetMapping("/project/{id}/user.json")
//    public ResponseEntity<String> showByProject(
//            @PathVariable String id
//    ) {
//        log.debug("REST request to get project users : {}", id);
//        secUserService.li
//    }


}
