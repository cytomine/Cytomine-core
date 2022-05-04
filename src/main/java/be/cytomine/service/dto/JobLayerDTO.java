package be.cytomine.service.dto;

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

import be.cytomine.utils.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class JobLayerDTO {

    private Long id;

    private String username;

    private String softwareName;

    private String softwareVersion;

    private Date created;

    private boolean algo = true;

    private Long job;

    public static JsonObject getDataFromDomain(JobLayerDTO jobLayer) {
        JsonObject returnArray = new JsonObject();
        returnArray.put("id", jobLayer.getId());
        returnArray.put("username", jobLayer.getUsername());
        returnArray.put("softwareName",
                (jobLayer.getSoftwareVersion()==null || jobLayer.getSoftwareVersion().trim().equals("") ?
                        jobLayer.getSoftwareName() :
                        jobLayer.getSoftwareName() + " (" + jobLayer.getSoftwareVersion() + ")"
                )
        );
        returnArray.put("created", jobLayer.getCreated().getTime());
        returnArray.put("algo", jobLayer.isAlgo());
        returnArray.put("job", jobLayer.getJob());
        return returnArray;
    }



}
