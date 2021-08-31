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

class AbstractSliceUrlMappings {

    static mappings = {
        "/api/abstractslice.$format"(controller: "restAbstractSlice"){
            action = [POST:"add"]
        }
        "/api/abstractslice/$id.$format"(controller: "restAbstractSlice"){
            action = [GET:"show", PUT:"update", DELETE:"delete"]
        }

        "/api/abstractimage/$id/$channel/$zStack/$time/abstractslice.$format"(controller: "restAbstractSlice"){
            action = [GET:"getByAbstractImageAndCoordinates"]
        }

        "/api/abstractimage/$id/abstractslice.$format"(controller: "restAbstractSlice"){
            action = [GET:"listByAbstractImage"]
        }
        "/api/uploadedfile/$id/abstractslice.$format"(controller: "restAbstractSlice") {
            action = [GET: "listByUploadedFile"]
        }

        "/api/abstractslice/$id/user.$format"(controller:"restAbstractSlice"){
            action = [GET:"showUploaderOfImage"]
        }

        "/api/abstractslice/$id/thumb.$format"(controller: "restAbstractSlice"){
            action = [GET:"thumb", POST:"thumb"]
        }
        "/api/abstractslice/$id/crop.$format"(controller: "restAbstractSlice"){
            action = [GET:"crop", POST:"crop"]
        }
        "/api/abstractslice/$id/window-$x-$y-$w-$h.$format"(controller: "restAbstractSlice"){
            action = [GET:"window", POST:"window"]
        }
        "/api/abstractslice/$id/window_url-$x-$y-$w-$h.$format"(controller: "restAbstractSlice"){
            action = [GET:"windowUrl", POST:"windowUrl"]
        }
        "/api/abstractslice/$id/camera.$format"(controller: "restAbstractSlice"){
            action = [POST:"camera"]
        }
        "/api/abstractslice/$id/camera-$x-$y-$w-$h.$format"(controller: "restAbstractSlice"){
            action = [GET:"camera", POST:"camera"]
        }
        "/api/abstractslice/$id/camera_url-$x-$y-$w-$h.$format"(controller: "restAbstractSlice"){
            action = [GET:"cameraUrl", POST:"cameraUrl"]
        }
    }
}


