package be.cytomine.api.controller.security;

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
import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.project.ProjectRepresentativeUser;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.dto.AuthInformation;
import be.cytomine.exceptions.AuthenticationException;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.server.StorageService;
import be.cytomine.service.ontology.OntologyService;
import be.cytomine.service.project.ProjectRepresentativeUserService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.search.UserSearchExtension;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.SecurityUtils;
import be.cytomine.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestUserJobController extends RestCytomineController {

    private final SecUserService secUserService;

    private final ProjectService projectService;

    private final CurrentUserService currentUserService;

    private final ImageInstanceService imageInstanceService;

    private final SecurityACLService securityACLService;

    private final ProjectRepresentativeUserService projectRepresentativeUserService;

    private final OntologyService ontologyService;

    private final StorageService storageService;

    private final ApplicationContext applicationContext;


    @GetMapping("/project/{id}/userjob.json")
    public ResponseEntity<String> listUserJobByProject(
            @PathVariable Long id
    ) {
        // TODO: implement...
        return responseSuccess(List.of());
    }
}
