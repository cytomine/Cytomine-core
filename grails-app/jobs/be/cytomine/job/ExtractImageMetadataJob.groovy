package be.cytomine.job

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

import be.cytomine.image.AbstractImage
import be.cytomine.image.UploadedFile
import grails.validation.ValidationException
import org.springframework.transaction.annotation.Transactional

class ExtractImageMetadataJob {

    def imagePropertiesService
    def sampleHistogramService

    static triggers = {
        simple name: 'extractImageMetadataJob', startDelay: 10000, repeatInterval: 1000*20
    }

    @Transactional
    def execute() {
        Collection<AbstractImage> abstractImages = AbstractImage.findAllByWidthInListOrWidthIsNull([-1,0])
        abstractImages.each { image ->
            try {
                imagePropertiesService.extractUseful(image)
            } catch (ValidationException e) {
                log.error "$image cannot be saved"
                log.error e.getMessage()
            }
        }
        //TODO activate when bitPerSample is implemented
        /*Collection<AbstractImage> abstractImages = AbstractImage.findAllBySamplePerPixelIsNullOrWidthIsNullOrWidth(-1, [max: 10, sort: "created", order: "desc"])
        abstractImages.each { image ->
            try {
                UploadedFile.withNewSession {
                    AbstractImage.withNewSession {
                        image.attach()
                        log.info "Regenerate properties for image $image - ${image.originalFilename}"
                        imagePropertiesService.regenerate(image)
                        if (image.bitPerSample > 8)
                            sampleHistogramService.extractHistogram(image)
                    }
                }
            }
            catch (Exception e) {
                log.error "Error during metadata extraction for image $image: ${e.printStackTrace()}"
            }
        }*/
    }
}
