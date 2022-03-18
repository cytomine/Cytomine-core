package be.cytomine.api;

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.domain.security.User;
import be.cytomine.dto.LoginVM;
import be.cytomine.config.security.JWTFilter;
import be.cytomine.exceptions.AuthenticationException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.security.jwt.TokenType;
import be.cytomine.utils.JsonObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("")
@AllArgsConstructor
public class LoginController extends RestCytomineController {

    private final TokenProvider tokenProvider;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final UserRepository userRepository;


    @PostMapping("/api/authenticate")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginVM.getUsername(),
                loginVM.getPassword()
        );

        try {
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.createToken(authentication, loginVM.isRememberMe() ? TokenType.REMEMBER_ME : TokenType.SESSION);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
            return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
        } catch (BadCredentialsException e) {
            throw new ForbiddenException("Bad username or password");
        } catch (AccountExpiredException e) {
            throw new ForbiddenException("Account expired");
        } catch (CredentialsExpiredException e) {
            throw new ForbiddenException("Password expired");
        } catch (DisabledException e) {
            throw new ForbiddenException("Account disabled");
        } catch (LockedException e) {
            throw new ForbiddenException("Account locked");
        }
    }



    @RequestMapping(path = {"/user/forgotUsername.json"}, method = {RequestMethod.POST})
    public ResponseEntity<String> checkPassword(@RequestBody MultiValueMap<String, String> formData) {
        log.debug("REST request to retrieve username");

        User user = userRepository.findByEmailLikeIgnoreCase(formData.getFirst("j_email"))
                .orElseThrow(() -> new ObjectNotFoundException("User", JsonObject.of("email", formData.getFirst("j_email")).toJsonString()));
        //notificationService.notifyForgotUsername(user);
       return responseSuccess(JsonObject.of("message","Check your inbox"));
    }


    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}
