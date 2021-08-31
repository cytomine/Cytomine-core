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

class SliceInstanceUrlMappings {

    static mappings = {
        "/api/sliceinstance.$format"(controller: "restSliceInstance"){
            action = [POST:"add"]
        }
        "/api/sliceinstance/$id.$format"(controller: "restSliceInstance"){
            action = [GET:"show", PUT:"update", DELETE:"delete"]
        }

        "/api/imageinstance/$id/$channel/$zStack/$time/sliceinstance.$format"(controller: "restSliceInstance"){
            action = [GET:"getByImageInstanceAndCoordinates"]
        }

        "/api/imageinstance/$id/sliceinstance.$format"(controller: "restSliceInstance"){
            action = [GET:"listByImageInstance"]
        }

        "/api/sliceinstance/$id/thumb.$format"(controller: "restSliceInstance"){
            action = [GET:"thumb", POST:"thumb"]
        }
        "/api/sliceinstance/$id/crop.$format"(controller: "restSliceInstance"){
            action = [GET:"crop", POST:"crop"]
        }
        "/api/sliceinstance/$id/window-$x-$y-$w-$h.$format"(controller: "restSliceInstance"){
            action = [GET:"window", POST:"window"]
        }
        "/api/sliceinstance/$id/window_url-$x-$y-$w-$h.$format"(controller: "restSliceInstance"){
            action = [GET:"windowUrl", POST:"windowUrl"]
        }
        "/api/sliceinstance/$id/camera.$format"(controller: "restSliceInstance"){
            action = [POST:"camera"]
        }
        "/api/sliceinstance/$id/camera-$x-$y-$w-$h.$format"(controller: "restSliceInstance"){
            action = [GET:"camera", POST:"camera"]
        }
        "/api/sliceinstance/$id/camera_url-$x-$y-$w-$h.$format"(controller: "restSliceInstance"){
            action = [GET:"cameraUrl", POST:"cameraUrl"]
        }
    }
}


