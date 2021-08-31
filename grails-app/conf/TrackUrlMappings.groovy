/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
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

class TrackUrlMappings {
    static mappings = {

        "/api/track.$format"(controller:"restTrack"){
            action = [POST:"add"]
        }
        "/api/track/$id.$format"(controller:"restTrack"){
            action = [GET:"show",PUT:"update", DELETE:"delete"]
        }
        "/api/project/$id/track.$format"(controller:"restTrack"){
            action = [GET:"listByProject"]
        }
        "/api/imageinstance/$id/track.$format"(controller:"restTrack"){
            action = [GET:"listByImageInstance"]
        }

        "/api/annotationtrack/$annotation/$track.$format"(controller: "restAnnotationTrack"){
            action = [GET: "show", DELETE: "delete"]
        }

        "/api/annotationtrack.$format"(controller: "restAnnotationTrack"){
            action = [POST: "add"]
        }

        "/api/track/$id/annotationtrack.$format"(controller: "restAnnotationTrack") {
            action = [GET: "listByTrack"]
        }

        "/api/annotation/$id/annotationtrack.$format"(controller: "restAnnotationTrack") {
            action = [GET: "listByAnnotation"]
        }
    }
}
