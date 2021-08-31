package be.cytomine.image

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
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

import be.cytomine.Exception.ConstraintException
import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ForbiddenException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.image.server.Storage
import be.cytomine.laboratory.Sample
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.meta.AttachedFile
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class AbstractImageService extends ModelService {

    static transactional = true

    def commandService
    def cytomineService
    def imagePropertiesService
    def transactionService
    def storageService
    def imageInstanceService
    def attachedFileService
    def currentRoleServiceProxy
    def securityACLService
    def imageServerService
    def projectService
    def uploadedFileService

    def currentDomain() {
        return AbstractImage
    }

    AbstractImage read(def id) {
        AbstractImage abstractImage = AbstractImage.read(id)
        if(abstractImage) {
            securityACLService.checkAtLeastOne(abstractImage, READ)
        }
        abstractImage
    }

    AbstractImage get(def id) {
        AbstractImage abstractImage = AbstractImage.get(id)
        if(abstractImage) {
            securityACLService.checkAtLeastOne(abstractImage, READ)
        }
        abstractImage
    }

    boolean hasRightToReadAbstractImageWithProject(AbstractImage image) {
        if(currentRoleServiceProxy.isAdminByNow(cytomineService.currentUser)) return true
        List<ImageInstance> imageInstances = ImageInstance.findAllByBaseImageAndDeletedIsNull(image)
        List<Project> projects = imageInstances.collect{it.project}
        for(Project project : projects) {
            if(project.hasACLPermission(project,READ)) return true
        }
        return false
    }

    // TODO: remove ? not meaningful
    def list(Project project) {
        securityACLService.check(project,READ)
        ImageInstance.createCriteria().list {
            eq("project", project)
            projections {
                groupProperty("baseImage")
            }
        }
    }

    def list(SecUser user, Project project = null, String sortedProperty = null, String sortDirection = null, Long max  = 0, Long offset = 0, searchParameters = []) {
        def validSearchParameters = getDomainAssociatedSearchParameters(AbstractImage, searchParameters)

        def result
        if(currentRoleServiceProxy.isAdminByNow(user)) {
            result =  criteriaRequestWithPagination(AbstractImage, max, offset, {
                isNull("deleted")
            }, validSearchParameters, sortedProperty, sortDirection)
        } else {
            List<Storage> storages = securityACLService.getStorageList(cytomineService.currentUser, false)
            result =  criteriaRequestWithPagination(AbstractImage, max, offset, {
                createAlias("uploadedFile", "uf")
                'in'("uf.storage.id", storages.collect{ it.id })
                isNull("deleted")
            }, validSearchParameters, sortedProperty, sortDirection)
        }

        if(project) {
            List<AbstractImage> images = result.data
            TreeSet<Long> inProjectImagesId = new TreeSet<>(ImageInstance.findAllByProjectAndDeletedIsNull(project).collect{it.baseImage.id})

            def data = []
            images.each { image ->
                def ai = AbstractImage.getDataFromDomain(image)
                ai.inProject = (inProjectImagesId.contains(image.id))
                data << ai
            }
            result.data = data
        }
        return result
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(JSONObject json) throws CytomineException {
        transactionService.start()
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)

        if (json.uploadedFile) {
            UploadedFile uploadedFile = uploadedFileService.read(json.uploadedFile as Long)
            if (uploadedFile?.status != UploadedFile.Status.DEPLOYING.code) {
                // throw new Error()
            }
        }

        return executeCommand(new AddCommand(user: currentUser), null, json)
    }

    def beforeAdd(def domain) {
        log.info "Create a new Sample"
        long timestamp = new Date().getTime()
        Sample sample = new Sample(name : timestamp.toString() + "-" + domain?.uploadedFile?.getOriginalFilename()).save()
        domain?.sample = sample
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(AbstractImage image,def jsonNewData) throws CytomineException {
        securityACLService.checkAtLeastOne(image,WRITE)
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new EditCommand(user: currentUser), image,jsonNewData)
        def attributes = JSON.parse(image.encodeAsJSON())
        def res = executeCommand(new EditCommand(user: currentUser), image,jsonNewData)
        AbstractImage abstractImage = res.object

        Integer magnification = JSONUtils.getJSONAttrInteger(attributes,'magnification',null)
        Double resolution = JSONUtils.getJSONAttrDouble(attributes,"resolution",null)

        boolean magnificationUpdated = magnification != abstractImage.magnification
        boolean resolutionUpdated = resolution != abstractImage.resolution

        def images = []
        if(resolutionUpdated && magnificationUpdated ) {
            if(resolution!= null && magnification!= null) {
                images.addAll(ImageInstance.findAllByBaseImageAndResolutionAndMagnification(image,resolution, magnification))
            } else if(resolution!= null) {
                images.addAll(ImageInstance.findAllByBaseImageAndResolutionAndMagnificationIsNull(image,resolution))
            } else if(magnification!= null) {
                images.addAll(ImageInstance.findAllByBaseImageAndResolutionIsNullAndMagnification(image,magnification))
            } else {
                images.addAll(ImageInstance.findAllByBaseImageAndResolutionIsNullAndMagnificationIsNull(image))
            }

            images.each {
                def json = JSON.parse(it.encodeAsJSON())
                json.resolution = abstractImage.resolution
                json.magnification = abstractImage.magnification
                imageInstanceService.update(it, json)
            }
        }
        //ii with same res & magn than ai were updated so we will fetch only ii with same res and different magn
        if(resolutionUpdated) {
            images = []
            if(resolution!= null) {
                images.addAll(ImageInstance.findAllByBaseImageAndResolution(image,resolution))
            } else {
                images.addAll(ImageInstance.findAllByBaseImageAndResolutionIsNull(image))
            }

            images.each {
                def json = JSON.parse(it.encodeAsJSON())
                json.resolution = abstractImage.resolution
                imageInstanceService.update(it, json)
            }

        }
        if(magnificationUpdated) {
            images = []
            if(magnification!= null) {
                images.addAll(ImageInstance.findAllByBaseImageAndMagnification(image,magnification))
            } else {
                images.addAll(ImageInstance.findAllByBaseImageAndMagnificationIsNull(image))
            }

            images.each {
                def json = JSON.parse(it.encodeAsJSON())
                json.magnification = abstractImage.magnification
                imageInstanceService.update(it, json)
            }
        }

        return res
    }

    def getUploaderOfImage(long id){
        AbstractImage img = read(id)
        return img?.uploadedFile?.user
    }

    /**
     * Check if some instances of this image exists and are still active
     */
    def isUsed(def id) {
        AbstractImage domain = AbstractImage.read(id);
        boolean usedByImageInstance = ImageInstance.findAllByBaseImageAndDeletedIsNull(domain).size() != 0
        boolean usedByNestedFile = CompanionFile.findAllByImage(domain).size() != 0

        return usedByImageInstance || usedByNestedFile
    }

    /**
     * Returns the list of all the unused abstract images
     */
    def listUnused(User user) {
        def result = []
        def abstractList = list(user);
        abstractList.data.each {
            image ->
                if(!isUsed(image.id)) result << image;
        }
        return result;
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(AbstractImage domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        securityACLService.checkAtLeastOne(domain,WRITE)

        if (!isUsed(domain.id)) {
            SecUser currentUser = cytomineService.getCurrentUser()
            Command c = new DeleteCommand(user: currentUser,transaction:transaction)
            return executeCommand(c,domain,null)
        } else{
            def instances = ImageInstance.findAllByBaseImageAndDeletedIsNull(domain)
            throw new ForbiddenException("Abstract Image has instances in active projects : " +
                    instances.collect{it.project.name}.join(",") +
                    " with the following names : " +
                    instances.collect{it.instanceFilename}.unique().join(","),
                    [projectNames:instances.collect{it.project.name},
                     imageNames:instances.collect{it.instanceFilename}.unique()]);
        }
    }

    /**
     * Get all image servers for an image id
     */
    @Deprecated
    def imageServers(def id) {
        AbstractImage image = read(id)
        AbstractSlice slice = image.getReferenceSlice()
        return [imageServersURLs : [slice?.uploadedFile?.imageServer?.url + "/slice/tile?zoomify=" + slice?.path]]
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.originalFilename]
    }

    def abstractSliceService
    def deleteDependentAbstractSlice(AbstractImage ai, Transaction transaction, Task task = null) {
        def slices = AbstractSlice.findAllByImage(ai)
        slices.each {
            abstractSliceService.delete(it, transaction, task)
        }
    }

    def deleteDependentImageInstance(AbstractImage ai, Transaction transaction,Task task=null) {
        def images = ImageInstance.findAllByBaseImageAndDeletedIsNull(ai);
        if(!images.isEmpty()) {
            throw new ConstraintException("This image $ai cannot be deleted as it has already been insert " +
                    "in projects " + images.collect{it.project.name})
        }
    }

    def companionFileService
    def deleteDependentCompanionFile(AbstractImage ai, Transaction transaction, Task task = null) {
        CompanionFile.findAllByImage(ai).each {
            companionFileService.delete(it, transaction, task)
        }
    }

    def deleteDependentAttachedFile(AbstractImage ai, Transaction transaction,Task task=null) {
        AttachedFile.findAllByDomainIdentAndDomainClassName(ai.id, ai.class.getName()).each {
            attachedFileService.delete(it,transaction,null,false)
        }
    }

    def deleteDependentNestedImageInstance(AbstractImage ai, Transaction transaction,Task task=null) {
        NestedImageInstance.findAllByBaseImage(ai).each {
            it.delete(flush: true)
        }
    }
}
