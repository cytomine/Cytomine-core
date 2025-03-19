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

import be.cytomine.exceptions.AuthenticationException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.security.jwt.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.GenericFilterBean;


import java.io.IOException;
import java.util.Locale;

import static be.cytomine.security.jwt.TokenType.SHORT_TERM;
import static org.springframework.jmx.export.naming.IdentityNamingStrategy.TYPE_KEY;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
public class JWTFilter extends GenericFilterBean {

    public static final String AUTHORIZATION_HEADER = "Authorization";

    private final TokenProvider tokenProvider;

    public JWTFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String jwt = resolveToken(httpServletRequest);

        if (StringUtils.hasText(jwt)) {
            Jws<Claims> claimsJws = this.tokenProvider.decodeToken(jwt);
            if (claimsJws!=null) {
                if (isShortTermToken(claimsJws) && !((HttpServletRequest) servletRequest).getMethod().toUpperCase(Locale.ROOT).equals("GET")) {
                    throw new ForbiddenException("Short term token can only be use with GET request");
                }
                try {
                    Authentication authentication = this.tokenProvider.getAuthentication(jwt, claimsJws.getBody());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (AuthenticationException exception) {
                    ((HttpServletResponse)servletResponse).sendError(HttpStatus.UNAUTHORIZED.value(), exception.msg);
                    return;
                }

            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        bearerToken = request.getParameter(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public static String resolveToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isShortTermToken(Jws<Claims> claimsJws) {
        return claimsJws.getBody().containsKey(TYPE_KEY) && claimsJws.getBody().get(TYPE_KEY).equals(SHORT_TERM.toString());
    }

}
