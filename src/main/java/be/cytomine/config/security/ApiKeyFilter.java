package be.cytomine.config.security;

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

import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.AuthenticationException;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.security.DomainUserDetailsService;
import be.cytomine.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

public class ApiKeyFilter extends OncePerRequestFilter {

    private final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final DomainUserDetailsService domainUserDetailsService;

    private final SecUserRepository secUserRepository;


    public ApiKeyFilter(DomainUserDetailsService domainUserDetailsService, SecUserRepository secUserRepository) {
        this.domainUserDetailsService = domainUserDetailsService;
        this.secUserRepository = secUserRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //log with token id TODO: auth with authWithToken
        boolean token = false; //tryAPIAUhtentificationWithToken(request, response);
        if(!token) {
            //with signature (in header)
            tryAPIAuthentification(request, response);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * http://code.google.com/apis/storage/docs/reference/v1/developer-guidev1.html#authentication
     */
    private boolean tryAPIAuthentification(HttpServletRequest request, HttpServletResponse response) {
        if (request.getHeader("date") == null) {
            return false;
        }
        if (request.getHeader("host") == null) {
            return false;
        }
        String authorization = request.getHeader("authorization");
        if (authorization == null) {
            return false;
        }
        if (!authorization.startsWith("CYTOMINE") || !authorization.contains(" ") || !authorization.contains(":")) {
            return false;
        }
        try {

            String content_md5 = (request.getHeader("content-MD5") != null) ? request.getHeader("content-MD5") : "";

            String content_type = (request.getHeader("content-type") != null) ? request.getHeader("content-type") : "";
            content_type = (request.getHeader("Content-Type") != null) ? request.getHeader("Content-Type") : content_type;
            String date = (request.getHeader("date") != null) ? request.getHeader("date") : "";

            String queryString = (request.getQueryString() != null) ? "?" + request.getQueryString() : "";

            String path = request.getRequestURI().toString(); // TODO; forwardUri?
            // original URI Request

            String accessKey = authorization.substring(authorization.indexOf(" ") + 1, authorization.indexOf(":"));
            String authorizationSign = authorization.substring(authorization.indexOf(":") + 1);

            Optional<SecUser> user = secUserRepository.findByPublicKeyAndEnabled(accessKey,true);

            if (user.isEmpty()) {
                log.debug("User cannot be extracted with accessKey {}", accessKey);
                //response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                throw new AuthenticationException("User cannot be extracted with accessKey " + accessKey);
            } else {
                String signature = SecurityUtils.generateKeys(request.getMethod(),content_md5, content_type,date,queryString,path,user.get());
                if (authorizationSign.equals(signature)) {
                    this.reauthenticate(user.get(), null);
                    return true;
                } else {
                    // the java client does not set content-type, so we override the header to application/json BEFORE this authentication.
                    // So the client thinks content-type is "" while spring boot set it to application/json. In order to match the client signature, we generate it
                    // with an empty value.
                    // => it would be better to improve the java client to set a valid content type.
                    String signatureWithEmptyContentType = SecurityUtils.generateKeys(request.getMethod(),content_md5, "",date,queryString,path,user.get());
                    if (authorizationSign.equals(signatureWithEmptyContentType)) {
                        this.reauthenticate(user.get(), null);
                        return true;
                    }


                    return false;
                }
            }



        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Rebuild an Authentication for the given username and register it in the security context.
     * Typically used after updating a user's authorities or other auth-cached info.
     * <p/>
     * Also removes the user from the user cache to force a refresh at next login.
     *
     * @param username the user's login name
     * @param password optional
     */
    public void reauthenticate(final SecUser secUser, final String password) {
        UserDetailsService userDetailsService = this.domainUserDetailsService;

        UserDetails userDetails = userDetailsService.loadUserByUsername(secUser.getUsername());
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, password == null ? userDetails.getPassword() : password, userDetails.getAuthorities());
        usernamePasswordAuthenticationToken.setDetails(secUser);

        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
    }
}
