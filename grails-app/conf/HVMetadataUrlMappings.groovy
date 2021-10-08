/*
* Copyright (c) 2009-2017. Authors: see NOTICE file.
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

class HVMetadataUrlMappings {

    static mappings = {
        "/api/staining.$format"(controller: "restHVMetadata") {
            action = [GET: "listStaining", POST: "addStaining"]
        }
        "/api/staining/$id.$format"(controller: "restHVMetadata") {
            action = [DELETE: "delete"]
        }

        "/api/laboratory.$format"(controller: "restHVMetadata") {
            action = [GET: "listLaboratory", POST: "addLaboratory"]
        }
        "/api/laboratory/$id.$format"(controller: "restHVMetadata") {
            action = [DELETE: "delete"]
        }

        "/api/antibody.$format"(controller: "restHVMetadata") {
            action = [GET: "listAntibody", POST: "addAntibody"]
        }
        "/api/antibody/$id.$format"(controller: "restHVMetadata") {
            action = [DELETE: "delete"]
        }

        "/api/detection.$format"(controller: "restHVMetadata") {
            action = [GET: "listDetection", POST: "addDetection"]
        }
        "/api/detection/$id.$format"(controller: "restHVMetadata") {
            action = [DELETE: "delete"]
        }

        "/api/dilution.$format"(controller: "restHVMetadata") {
            action = [GET: "listDilution", POST: "addDilution"]
        }
        "/api/dilution/$id.$format"(controller: "restHVMetadata") {
            action = [DELETE: "delete"]
        }

        "/api/instrument.$format"(controller: "restHVMetadata") {
            action = [GET: "listInstrument", POST: "addInstrument"]
        }
        "/api/instrument/$id.$format"(controller: "restHVMetadata") {
            action = [DELETE: "delete"]
        }
    }

}