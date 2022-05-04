package be.cytomine.api.controller.processing;

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
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.project.ProjectDefaultLayerRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.project.ProjectDefaultLayerService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.utils.TaskService;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class RestMockProcessingResponseController extends RestCytomineController {

    @GetMapping("/imagefilter.json")
    public ResponseEntity<String> imagefilter(
    ) {
        return responseSuccess(new ArrayList());
    }

    @GetMapping("/project/{project}/imagefilterproject.json")
    public ResponseEntity<String> imagefilterproject(
    ) {
        return responseSuccess(new ArrayList());
    }
}
