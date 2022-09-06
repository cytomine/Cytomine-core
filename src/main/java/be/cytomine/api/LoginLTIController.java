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
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.service.security.lti.LtiService;
import be.cytomine.utils.JsonObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("")
@AllArgsConstructor
public class LoginLTIController extends RestCytomineController {

    final LtiService ltiService;

    private final ApplicationProperties applicationProperties;

    @PostMapping("/login/loginWithLTI")
    public RedirectView authorize(

    ) throws IOException, LtiVerificationException {
        JsonObject params = super.mergeQueryParamsAndBodyParams();
        log.debug("LoginWithLTI Params: {}", params.toString());

        if (!applicationProperties.getAuthentication().getLti().isEnabled()) {
            log.warn("LTI is not enabled, refuse LTI request");
            throw new ForbiddenException("LTI is not enabled");
        }

        String redirection = ltiService.verifyAndRedirect(params, request, applicationProperties.getAuthentication().getLti(), new LtiOauthVerifier());

        return new RedirectView(redirection);
    }

}
