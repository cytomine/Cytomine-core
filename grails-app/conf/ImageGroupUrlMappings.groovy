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

/**
 * Cytomine
 * User: stevben
 * Date: 10/10/11
 * Time: 13:49
 */
class ImageGroupUrlMappings {

    static mappings = {
        /* Image Instance */
        "/api/imagegroup.$format"(controller: "restImageGroup"){
            action = [POST:"add"]
        }
        "/api/imagegroup/$id.$format"(controller: "restImageGroup"){
            action = [GET:"show", PUT:"update", DELETE:"delete"]
        }
        "/api/project/$id/imagegroup.$format"(controller: "restImageGroup"){
            action = [GET:"listByProject"]
        }

        "/api/imagegroup/$id/characteristics.$format"(controller: "restImageGroup"){
            action = [GET:"characteristics"]
        }

        "/api/imagegroup/$id/thumb.$format"(controller: "restImageGroup"){
            action = [GET:"thumb"]
        }

        /* Image group Hdf5 special */
        /*"/api/imagegroupHDF5.$format"(controller: "restImageGroupHDF5"){
            action = [POST:"add"]
        }

        "/api/imagegroupHDF5/$id.$format"(controller: "restImageGroupHDF5"){
            action = [GET:"show", PUT:"update", DELETE:"delete"]
        }

        "/api/imagegroup/$group/imagegroupHDF5.$format"(controller: "restImageGroupHDF5"){
            action = [GET: "showFromImageGroup", DELETE:"deleteFromImageGroup"]
        }

        "/api/imagegroupHDF5/$id/$x/$y/pixel.$format"(controller: "restImageGroupHDF5"){
            action = [GET: "pixelHDF5"]
        }

        "/api/imagegroupHDF5/$id/$x/$y/$w/$h/rectangle.$format"(controller: "restImageGroupHDF5"){
            action = [GET: "rectangleHDF5"]
        }*/

    }
}
