package be.cytomine.api;

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

import be.cytomine.api.controller.RestCytomineController;
import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.config.security.JWTFilter;
import be.cytomine.domain.security.AuthWithToken;
import be.cytomine.domain.security.ForgotPasswordToken;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.User;
import be.cytomine.dto.LoginVM;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.security.AuthWithTokenRepository;
import be.cytomine.repository.security.ForgotPasswordTokenRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.security.CustomUserDetailsAuthenticationProvider;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.security.jwt.TokenType;
import be.cytomine.security.ldap.LdapIdentityAuthenticationProvider;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.utils.NotificationService;
import be.cytomine.utils.JsonObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.utils.DateUtils.MAX_DATE_FOR_DATABASE;

@Slf4j
@RestController
@RequestMapping("")
@AllArgsConstructor
public class LoginController extends RestCytomineController {

    private final TokenProvider tokenProvider;

    private final UserRepository userRepository;

    private final NotificationService notificationService;

    private final SecurityACLService securityACLService;

    private final Environment env;

    private final CustomUserDetailsAuthenticationProvider customUserDetailsAuthenticationProvider;

    private final LdapIdentityAuthenticationProvider ldapIdentityAuthenticationProvider;

    private final ApplicationProperties applicationProperties;
    @Autowired
    AuthWithTokenRepository authWithTokenRepository;
    @Autowired
    ForgotPasswordTokenRepository forgotPasswordTokenRepository;


    @RequestMapping(
            method = RequestMethod.GET,
            value = {"/saml/login"})
    public void loginWithSSO(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.debug("User {} logged in with SSO", username);
        User user = userRepository.findByUsernameLikeIgnoreCaseAndEnabledIsTrue(username)
                .orElseThrow(() -> new ObjectNotFoundException("User", username));
        log.debug("generate tokenKey for user {} ", username);
        Date expiration = DateUtils.addMinutes(new Date(), (int) 60d);
        String tokenKey = UUID.randomUUID().toString();
        AuthWithToken token = new AuthWithToken();
        token.setUser(user);
        token.setExpiryDate(expiration);
        token.setTokenKey(tokenKey);
        authWithTokenRepository.save(token);
        log.debug("tokenKey: {} generated for user {} ", tokenKey, username);

        // Prepare redirection
        String redirection = request.getParameter("cytomine_redirect");
        UriComponents originalRedirection = UriComponentsBuilder.fromHttpUrl(redirection).build();
        String originalFragment = originalRedirection.getFragment();

        // Parse the optional fragment as a URI to be able to work as query params
        UriComponents tmpFragmentToParseAsURI = UriComponentsBuilder
                .fromUriString(originalFragment)
                .queryParam("token", token.getTokenKey())
                .queryParam("username", authentication.getName())
                .build(); // this would merge already existing query params from the original fragment

        // Keep the original redirection URL, but override the new fragment
        UriComponents finalRedirection = UriComponentsBuilder
                .fromHttpUrl(originalRedirection.toString())  // keep the original request
                .scheme(originalRedirection.getScheme())
                .fragment(tmpFragmentToParseAsURI.toString()) // but apply a new merged fragment
                .build();

        log.debug("redirect to {}", finalRedirection);
        response.sendRedirect(finalRedirection.toString());
    }

    @PostMapping("/api/authenticate")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM) {

        try {
            JWTToken jwtTokens = logInAndGenerateCredentials(loginVM.getUsername(), loginVM.getPassword(), loginVM.isRememberMe());
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwtTokens.getToken());
            return new ResponseEntity<>(jwtTokens, httpHeaders, HttpStatus.OK);
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

    @RequestMapping(path = {"/login/forgotUsername"}, method = {RequestMethod.POST})
    public ResponseEntity<String> checkPassword(@RequestBody MultiValueMap<String, String> formData) throws MessagingException {
        log.debug("REST request to retrieve username");

        User user = userRepository.findByEmailLikeIgnoreCase(formData.getFirst("j_email"))
                .orElseThrow(() -> new ObjectNotFoundException("User", JsonObject.of("email", formData.getFirst("j_email")).toJsonString()));
        notificationService.notifyForgotUsername(user);
        return responseSuccess(JsonObject.of("message", "Check your inbox"));
    }

    @RequestMapping(path = {"/login/forgotPassword"}, method = {RequestMethod.POST})
    public ResponseEntity<String> forgotPassword(
            @RequestBody MultiValueMap<String, String> formData
    ) throws MessagingException {
        String username = formData.getFirst("j_username");
        if (username != null) {
            Optional<User> user = userRepository.findByUsernameLikeIgnoreCase(username); //we are not logged, so we bypass the service
            if (user.isPresent()) {
                String tokenKey = UUID.randomUUID().toString();
                ForgotPasswordToken forgotPasswordToken = new ForgotPasswordToken();
                forgotPasswordToken.setUser(user.get());
                forgotPasswordToken.setExpiryDate(DateUtils.addDays(new Date(), 1));
                forgotPasswordToken.setTokenKey(tokenKey);
                forgotPasswordTokenRepository.save(forgotPasswordToken);

                notificationService.notifyForgotPassword(user.get(), forgotPasswordToken);

                return responseSuccess(JsonObject.of("success", true, "message", "Check your inbox"));
            }

        }
        return responseSuccess(JsonObject.of()); //no explicit error
    }

    @RequestMapping(path = {"/login/loginWithToken"}, method = {RequestMethod.POST, RequestMethod.GET})
    public Object loginWithToken(
            @RequestParam String username,
            @RequestParam String tokenKey,
            @RequestParam(required = false) String redirect
    ) throws MessagingException {
        User user = userRepository.findByUsernameLikeIgnoreCaseAndEnabledIsTrue(username)
                .orElseThrow(() -> new ObjectNotFoundException("User", username));

        Optional<AuthWithToken> authToken = authWithTokenRepository.findByTokenKeyAndUser(tokenKey, user);
        Optional<ForgotPasswordToken> forgotPasswordToken = forgotPasswordTokenRepository.findByTokenKeyAndUser(tokenKey, user);

        //check first if a entry is made for this token
        if (authToken.isPresent() && authToken.get().isValid()) {
            JWTToken jwtTokens = logInAndGenerateCredentials(user.getUsername(), user.getRoles(), false);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwtTokens.getToken());
            if (redirect != null) {
                return new RedirectView(applicationProperties.getServerURL() + redirect + "?redirect_token=" + jwtTokens.token);
            }
            return new ResponseEntity<>(jwtTokens.toJsonObject().toJsonString(), httpHeaders, HttpStatus.OK);
        } else if (forgotPasswordToken.isPresent() && forgotPasswordToken.get().isValid()) {
            user = forgotPasswordToken.get().getUser();
            user.setPasswordExpired(true);
            userRepository.save(user);
            JWTToken jwtTokens = logInAndGenerateCredentials(user.getUsername(), user.getRoles(), false);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwtTokens.getToken());
            return new ResponseEntity<>(jwtTokens.toJsonObject().toJsonString(), httpHeaders, HttpStatus.OK);
        } else {
            throw new WrongArgumentException("Token is not valid");
        }
    }

    @RequestMapping(path = {"/api/token.json"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> buildToken() throws IOException {

        JsonObject params = super.mergeQueryParamsAndBodyParams();
        String username = params.getJSONAttrStr("username");
        User user = userRepository.findByUsernameLikeIgnoreCaseAndEnabledIsTrue(username)
                .orElseThrow(() -> new ObjectNotFoundException("User", username));

        if (!user.getPublicUser()) {
            // if user is not a public user, token can only be generate by admin
            securityACLService.checkCurrentUserIsAdmin();
        }

        boolean infiniteExpiration = params.getJSONAttrDouble("validity", -1d).equals(-1d);
        Date expiration;
        if (infiniteExpiration && user.getPublicUser()) {
            // if user is public, we allow token with no expiration
            expiration = MAX_DATE_FOR_DATABASE;
        } else if (infiniteExpiration) {
            throw new WrongArgumentException("Infinite validity can only be set for token link to public user");
        } else {
            expiration = DateUtils.addMinutes(new Date(), params.getJSONAttrDouble("validity", 60d).intValue());
        }

        String tokenKey = UUID.randomUUID().toString();
        AuthWithToken token = new AuthWithToken();
        token.setUser(user);
        token.setExpiryDate(expiration);
        token.setTokenKey(tokenKey);
        authWithTokenRepository.save(token);
        return responseSuccess(JsonObject.of("success", true, "token", token.getTokenKey()));
    }


    private JWTToken logInAndGenerateCredentials(String username, String password, Boolean isRememberMe) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                username,
                password
        );

        Authentication authentication;
        try {
            log.debug("Try to authenticate user in local database");
            authentication = customUserDetailsAuthenticationProvider.authenticate(authenticationToken);
            if (authentication == null) {
                throw new BadCredentialsException("User " + username + " cannot be authenticate");
            }
        } catch (AccountStatusException exception) {
            log.error("Error while authenticating user in local database", exception);
            throw exception;
        } catch (AuthenticationException exception) {
            log.error("Error while authenticating user in local database", exception);
            if (env.getProperty("application.authentication.ldap.enabled", Boolean.class, false)) {
                log.debug("Try to authenticate user in LDAP database");
                try {
                    authentication = ldapIdentityAuthenticationProvider.authenticate(authenticationToken);
                    if (authentication==null) {
                        throw new BadCredentialsException("User " + username + " cannot be authenticate");
                    }
                } catch (Exception exp) {
                    log.error("Error while authenticating user in LDAP database", exp);
                    throw new BadCredentialsException("User " + username + " cannot be authenticate");
                }
            } else {
                throw exception;
            }
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.createToken(authentication, isRememberMe ? TokenType.REMEMBER_ME : TokenType.SESSION);
        String shortTermToken = tokenProvider.createToken(SecurityContextHolder.getContext().getAuthentication(), TokenType.SHORT_TERM);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + token);
        return new JWTToken(token, shortTermToken);
    }


    private JWTToken logInAndGenerateCredentials(String username, Set<SecRole> roles, Boolean isRememberMe) {
        List<GrantedAuthority> authorities = roles.stream().map(x -> new SimpleGrantedAuthority(x.getAuthority())).collect(Collectors.toList());
        Authentication newAuth = new UsernamePasswordAuthenticationToken(username, "************", authorities);
        SecurityContextHolder.getContext().setAuthentication(newAuth);
        String token = tokenProvider.createToken(newAuth, isRememberMe ? TokenType.REMEMBER_ME : TokenType.SESSION);
        String shortTermToken = tokenProvider.createToken(SecurityContextHolder.getContext().getAuthentication(), TokenType.SHORT_TERM);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + token);
        return new JWTToken(token, shortTermToken);
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String token;

        private String shortTermToken;

        JWTToken(String idToken, String shortTermToken) {
            this.token = idToken;
            this.shortTermToken = shortTermToken;
        }

        @JsonProperty("token")
        String getToken() {
            return token;
        }

        @JsonProperty("shortTermToken")
        String getShortTermToken() {
            return shortTermToken;
        }

        public JsonObject toJsonObject() {
            return JsonObject.of("token", token, "shortTermToken", shortTermToken);
        }
    }
}
