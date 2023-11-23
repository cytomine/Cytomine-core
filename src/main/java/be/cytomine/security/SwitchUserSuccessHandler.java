package be.cytomine.security;

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

import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.security.jwt.TokenType;
import be.cytomine.utils.JsonObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * Spring Security success handler, specialized for Ajax requests.
 */
public class SwitchUserSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    TokenProvider tokenProvider;

    Long tokenValidityInSeconds;

    public SwitchUserSuccessHandler(TokenProvider tokenProvider, Long tokenValidityInSeconds) {
        this.tokenProvider = tokenProvider;
        this.tokenValidityInSeconds = tokenValidityInSeconds;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String token = tokenProvider.createToken(authentication, TokenType.IMPERSONATE);

        response.getWriter().println(JsonObject.of(
                "id_token", token,
                "created", new Date().getTime(),
                "validity", new Date(System.currentTimeMillis() + (tokenValidityInSeconds * 1000)).getTime()).toJsonString()
        );

        response.setStatus(HttpServletResponse.SC_OK);
    }
}
