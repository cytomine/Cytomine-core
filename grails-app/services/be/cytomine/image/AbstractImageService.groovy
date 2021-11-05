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
import be.cytomine.Exception.CytomineMethodNotYetImplementedException
import be.cytomine.Exception.ForbiddenException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.image.hv.HVMetadata
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
import groovy.sql.GroovyResultSet
import groovy.sql.Sql
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.util.ReflectionUtils

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
    def dataSource

    def currentDomain() {
        return AbstractImage
    }

    AbstractImage read(def id) {
        AbstractImage abstractImage = AbstractImage.read(id)
        if(abstractImage) {
            securityACLService.checkAtLeastOne(abstractImage, READ)
            checkDeleted(abstractImage)
        }
        abstractImage
    }

    AbstractImage get(def id) {
        AbstractImage abstractImage = AbstractImage.get(id)
        if(abstractImage) {
            securityACLService.checkAtLeastOne(abstractImage, READ)
            checkDeleted(abstractImage)
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

    def fullSearch(SecUser user, Project project = null, String sortColumn = null, String sortDirection = null, Long max  = 0, Long offset = 0, searchParameters = []) {
        securityACLService.checkIsSameUser(user, cytomineService.currentUser)

        String searchText = searchParameters.find { it.field.equals("searchText") }?.values

        String abstractImageAlias = "ai"

        if (!sortColumn) sortColumn = "created"
        if (!sortDirection) sortDirection = "asc"

        String sortedProperty = ReflectionUtils.findField(AbstractImage, sortColumn) ? "${abstractImageAlias}." + sortColumn : null
        if (!sortedProperty) throw new CytomineMethodNotYetImplementedException("AbstractImage list sorted by $sortDirection is not implemented")
        sortedProperty = fieldNameToSQL(sortedProperty)

        String select, from, where, search, sort
        String request

        select = "SELECT distinct $abstractImageAlias.* "
        from = "FROM abstract_image $abstractImageAlias "
        where = "WHERE ${abstractImageAlias}.deleted IS NULL "
        search = ""
        def mapParams = [:]

        if(searchText && !searchText.isEmpty()) {
            from += "LEFT OUTER JOIN description d ON ${abstractImageAlias}.id = d.domain_ident AND d.domain_class_name = 'be.cytomine.image.AbstractImage' "
            from += "LEFT OUTER JOIN tag_domain_association tda ON ${abstractImageAlias}.id = tda.domain_ident AND tda.domain_class_name = 'be.cytomine.image.AbstractImage' "
            from += "LEFT OUTER JOIN tag tag ON tag.id = tda.tag_id "

            def searchTexts
            if(searchText.contains(' ')) searchTexts = searchText.split(' ')
            else searchTexts = [searchText]

            searchTexts.eachWithIndex { Object entry, int i -> mapParams.put("parameter_"+i, '%'+entry+'%')}

            search += " AND "
            search += " ( "
            search += "("+mapParams.keySet().collect {" ${abstractImageAlias}.original_filename ILIKE :${it} "}.join(" AND ")+")"
            search += " OR "
            search += "("+mapParams.keySet().collect {" tag.name ILIKE :${it} "}.join(" AND ")+")"
            search += " OR "
            search += "("+mapParams.keySet().collect {" d.data ILIKE :${it} "}.join(" AND ")+")"
            search += " ) "
        }

        def validatedSearchParameters = getDomainAssociatedSearchParameters(AbstractImage, searchParameters)

        validatedSearchParameters = validatedSearchParameters.findAll {['staining','instrument','detection','dilution','laboratory','antibody'].contains(it.property)}
        validatedSearchParameters = validatedSearchParameters.collect {
            [operator: it.operator, property:it.property+"Id", value:it.value.id]
        }

        def hvSQLSearch = searchParametersToSQLConstraints(validatedSearchParameters)

        if(hvSQLSearch && hvSQLSearch.data && !hvSQLSearch.data.isEmpty()) {
            search += " AND "
            search += hvSQLSearch.data.collect{it.sql}.join(" AND ")
            hvSQLSearch.sqlParameters.each{
                mapParams.put(it.key,it.value)
            }
        }


        if(!currentRoleServiceProxy.isAdminByNow(user)) {
            List<Long> storages = securityACLService.getStorageList(cytomineService.currentUser, false).collect{it.id}
            if(!storages.isEmpty()) {
                from += "LEFT OUTER JOIN uploaded_file uf ON uf.id = ai.uploaded_file_id "
                where += " AND uf.storage_id IN (${storages.join(",")}) "
            }
        }



        sort = " ORDER BY " + sortedProperty
        sort += (sortDirection.equals("desc")) ? " DESC " : " ASC "

        request = select + from + where + search + sort
        if (max > 0) request += " LIMIT $max"
        if (offset > 0) request += " OFFSET $offset"


        def sql = new Sql(dataSource)
        def data = []
        if(mapParams.isEmpty()) mapParams = []

        sql.eachRow(request, mapParams) {
            def map = [:]

            for(int i =1; i<=((GroovyResultSet) it).getMetaData().getColumnCount(); i++){
                String key = ((GroovyResultSet) it).getMetaData().getColumnName(i)
                String objectKey = key.replaceAll( "(_)([A-Za-z0-9])", { Object[] test -> test[2].toUpperCase() } )


                map.putAt(objectKey, it[key])
            }

            map['created'] = map['created'].getTime()
            map['deleted'] = map['deleted']?.getTime()
            map['updated'] = map['updated']?.getTime()
            map['uploadedFile'] = map['uploadedFileId']
            map['sample'] = map['sampleId']
            map['scanner'] = map['scannerId']

            def line = AbstractImage.getDataFromDomain(AbstractImage.insertDataIntoDomain(map))
            data << line
        }
        def size
        request = "SELECT COUNT(DISTINCT ${abstractImageAlias}.id) " + from + where + search

        sql.eachRow(request, mapParams) {
            size = it.count
        }
        sql.close()

        def result = [data:data, total:size]
        max = (max > 0) ? max : Integer.MAX_VALUE
        result.offset = offset
        result.perPage = Math.min(max, result.total)
        result.totalPages = Math.ceil(result.total / max)

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
        securityACLService.check(image.container(),WRITE)
        SecUser currentUser = cytomineService.getCurrentUser()
        def attributes = JSON.parse(image.encodeAsJSON())
        def res = executeCommand(new EditCommand(user: currentUser), image,jsonNewData)
        AbstractImage abstractImage = res.object

        Integer magnification = JSONUtils.getJSONAttrInteger(attributes,'magnification',null)
        Double physicalSizeX = JSONUtils.getJSONAttrDouble(attributes,"physicalSizeX",null)

        boolean magnificationUpdated = magnification != abstractImage.magnification
        boolean physicalSizeXUpdated = physicalSizeX != abstractImage.physicalSizeX
        log.info("magnificationUpdated=$magnificationUpdated")
        log.info("physicalSizeXUpdated=$physicalSizeXUpdated")
        log.info("magnification=$magnification")
        log.info("physicalSizeX=$physicalSizeX")

        def images = []
        if(physicalSizeXUpdated && magnificationUpdated ) {
            if(physicalSizeX!= null && magnification!= null) {
                images.addAll(ImageInstance.findAllByBaseImageAndPhysicalSizeXAndMagnification(image,physicalSizeX, magnification))
            } else if(physicalSizeX!= null) {
                images.addAll(ImageInstance.findAllByBaseImageAndPhysicalSizeXAndMagnificationIsNull(image,physicalSizeX))
            } else if(magnification!= null) {
                images.addAll(ImageInstance.findAllByBaseImageAndPhysicalSizeXIsNullAndMagnification(image,magnification))
            } else {
                images.addAll(ImageInstance.findAllByBaseImageAndPhysicalSizeXIsNullAndMagnificationIsNull(image))
            }
            images.each {
                def json = JSON.parse(it.encodeAsJSON())
                json.physicalSizeX = abstractImage.physicalSizeX
                json.magnification = abstractImage.magnification
                imageInstanceService.update(it, json)
            }
        }
        //ii with same res & magn than ai were updated so we will fetch only ii with same res and different magn
        if(physicalSizeXUpdated) {
            images = []
            if(physicalSizeX!= null) {
                images.addAll(ImageInstance.findAllByBaseImageAndPhysicalSizeX(image,physicalSizeX))
            } else {
                images.addAll(ImageInstance.findAllByBaseImageAndPhysicalSizeXIsNull(image))
            }

            images.each {
                def json = JSON.parse(it.encodeAsJSON())
                json.physicalSizeX = abstractImage.physicalSizeX
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

        String oldFilename = JSONUtils.getJSONAttrStr(attributes,'originalFilename')
        boolean filenameUpdated = oldFilename != abstractImage.originalFilename

        if(filenameUpdated) {
            def json = JSON.parse(abstractImage.uploadedFile.encodeAsJSON())
            json.originalFilename = abstractImage.originalFilename
            uploadedFileService.update(abstractImage.uploadedFile, json)

            ImageInstance.findAllByInstanceFilename(oldFilename).each {
                json = JSON.parse(it.encodeAsJSON())
                json.instanceFilename = abstractImage.originalFilename
                imageInstanceService.update(it, json)
            }
        }

        ["laboratory", "staining", "antibody","detection", "dilution", "instrument"].each {
            HVMetadata oldMetadata = JSONUtils.getJSONAttrDomain(attributes,it , new HVMetadata(), false)
            boolean metadataUpdated = !oldMetadata.equals(abstractImage[it])
            if(metadataUpdated) {
                images = ImageInstance.findAllByBaseImage(image).findAll {ii ->
                    ii[it] == oldMetadata
                }.each {ii ->
                    def json = JSON.parse(ii.encodeAsJSON())
                    json[it] = abstractImage[it]?.id
                    imageInstanceService.update(ii, json)
                }
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
    def delete(AbstractImage domain, Transaction transaction = null, Task task = null, boolean printMessage = true, boolean deleteUploadFileLink = false) {
        securityACLService.checkAtLeastOne(domain,WRITE)

        if (!isUsed(domain.id)) {
            def jsonNewData = JSON.parse(domain.encodeAsJSON())
            jsonNewData.deleted = new Date().time
            Command c = new EditCommand(user: cytomineService.currentUser)
            c.delete = true
            log.info "abstract image delete (soft)"
            def response = executeCommand(c,domain,jsonNewData)
            if (deleteUploadFileLink) {
                // has to be done after the command otherwise fails on security check (uploadedfile null => container null)
                domain.refresh()
                domain.uploadedFile = null
                domain.save(flush:true)
            }
            return response
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
        log.info "abstract image deleteDependentAbstractSlice"
        def slices = AbstractSlice.findAllByImageAndDeletedIsNotNull(ai)
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
