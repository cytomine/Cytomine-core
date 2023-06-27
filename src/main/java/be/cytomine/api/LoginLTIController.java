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
import be.cytomine.config.properties.LtiConsumerProperties;
import be.cytomine.config.security.JWTFilter;
import be.cytomine.domain.meta.Configuration;
import be.cytomine.dto.LoginWithRedirection;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repository.meta.ConfigurationRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.security.jwt.TokenType;
import be.cytomine.security.lti.LitOauthVerifierCustom;
import be.cytomine.service.meta.ConfigurationService;
import be.cytomine.service.security.lti.LtiService;
import be.cytomine.utils.JsonObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static be.cytomine.service.meta.ConfigurationService.CONFIG_KEY_LTI_CONSUMERS;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Slf4j
@RestController
@RequestMapping("")
@AllArgsConstructor
public class LoginLTIController extends RestCytomineController {

    final LtiService ltiService;

    private final ApplicationProperties applicationProperties;

    private final TokenProvider tokenProvider;

    private final ConfigurationRepository configurationRepository;

    @RequestMapping(value = "/login/loginWithLTI", method = {GET, POST})
    public RedirectView authorize(

    ) throws IOException, LtiVerificationException {
        JsonObject params = super.mergeQueryParamsAndBodyParams();
        log.debug("LoginWithLTI Params: {}", params.toString());

        if (!applicationProperties.getAuthentication().getLti().isEnabled()) {
            log.warn("LTI is not enabled, refuse LTI request");
            throw new ForbiddenException("LTI is not enabled");
        }

        log.debug("Consumers config value " + configurationRepository.findByKey(CONFIG_KEY_LTI_CONSUMERS));
        List<Map<String, Object>> consumersValueKey = configurationRepository.findByKey(CONFIG_KEY_LTI_CONSUMERS)
                .map(Configuration::getValue)
                .map(JsonObject::toMapList)
                .orElseThrow(() -> new ForbiddenException("No LTI consumer defined"));

        List<LtiConsumerProperties> ltiConsumerProperties = new ArrayList<>();
        for (Map<String, Object> entry : consumersValueKey) {
            LtiConsumerProperties ltiConsumerProperty = new LtiConsumerProperties();
            ltiConsumerProperty.setKey((String)entry.get("key"));
            ltiConsumerProperty.setName((String)entry.get("name"));
            ltiConsumerProperty.setSecret((String)entry.get("secret"));
            ltiConsumerProperty.setUsernameParameter((String)entry.get("usernameParameter"));
            ltiConsumerProperties.add(ltiConsumerProperty);
        }

        LitOauthVerifierCustom litOauthVerifierCustom
                = new LitOauthVerifierCustom(applicationProperties.getServerURL()+"/login/loginWithLTI");
        LoginWithRedirection loginWithRedirection = ltiService.verifyAndRedirect(params, request, applicationProperties.getAuthentication().getLti(), ltiConsumerProperties, litOauthVerifierCustom);

        List<GrantedAuthority> authorities = loginWithRedirection.getUser().getRoles().stream().map(x -> new SimpleGrantedAuthority(x.getAuthority())).collect(Collectors.toList());
        Authentication newAuth = new UsernamePasswordAuthenticationToken(loginWithRedirection.getUser().getUsername(),"************", authorities);
        SecurityContextHolder.getContext().setAuthentication(newAuth);
        String token = tokenProvider.createToken(newAuth, TokenType.SESSION);
        response.setHeader(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + token);
        String redirectURL;
        if(loginWithRedirection.getRedirection().startsWith("http"))
        {
           redirectURL = loginWithRedirection.getRedirection() ;
        }else{
          redirectURL = applicationProperties.getServerURL() + (loginWithRedirection.getRedirection().startsWith("/") ? "" : "/") + loginWithRedirection.getRedirection();
        }
        log.debug("Redirect to {}", redirectURL);
        return new RedirectView(redirectURL+"?redirect_token=" + URLEncoder.encode(token, Charset.defaultCharset()));
    }

}
