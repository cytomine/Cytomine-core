package be.cytomine.service.utils;

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Dataset {

    @Value("${application.serverURL}")
    public String CYTOMINEURL;

    public String ADMINLOGIN = "admin";

    @Value("${application.adminPassword}")
    public String ADMINPASSWORD;

    public String SUPERADMINLOGIN = "superadmin";

    @Value("${application.adminPassword}")
    public String SUPERADMINPASSWORD;

    public String ANOTHERLOGIN = "anotheruser";

    @Value("${application.adminPassword}")
    public String ANOTHERPASSWORD;

    @Value("${application.adminEmail}")
    public String ADMINEMAIL;

    public String BADLOGIN = "badlogin";
    public String BADPASSWORD = "badpassword";


    public String UNDOURL = "command/undo.json";
    public String REDOURL = "command/redo.json";
}
