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

package be.cytomine.image.multidim

import be.cytomine.CytomineDomain
import be.cytomine.Exception.ConstraintException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.api.UrlApi
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.security.SecUser
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.transaction.Transactional
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import static groovyx.net.http.ContentType.*
import static org.springframework.security.acls.domain.BasePermission.READ

@Transactional
class ImageGroupHDF5Service  extends  ModelService{

    def securityACLService
    def imageGroupService
    def imageSequenceService
    def cytomineMailService
    def abstractImageService


    def currentDomain() {
        return ImageGroupHDF5
    }

    def getStringParamsI18n(def domain) {
         return [domain.id, domain.group.name]
    }

    ImageGroupHDF5 get(def id){
        def group = ImageGroupHDF5.get(id)
        if(group) {
            securityACLService.check(group.container(), READ)
        }
        group
    }

    ImageGroupHDF5 read(def id){
        def group = ImageGroupHDF5.read(id)
        if(group) {
            securityACLService.check(group.container(), READ)
        }
        group
    }

    def list(){
        ImageGroupHDF5.list()
    }

    def getByGroup(ImageGroup group){
        ImageGroupHDF5.findByGroup(group)
    }

    def add(def json){
        securityACLService.check(json.group, ImageGroup,"container",READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        json.user = currentUser.id

        def group = JSONUtils.getJSONAttrInteger(json,'group',0)
        json.filename = "${grailsApplication.config.fast_data_path}/${currentUser.id}/${group}.h5"

        //First get all the ImageSequence from the imageGroup
        ImageGroup imageGroup = imageGroupService.read(group)
        if (imageGroup == null)
            return

        def imagesSequenceList = imageSequenceService.list(imageGroup)
        if (imagesSequenceList.size() == 0)
            throw new ConstraintException("You need to have at least one ImageSequence in your ImageGroup to convert it")


        def response = executeCommand(new AddCommand(user: currentUser), null, json)
        convert(currentUser, imagesSequenceList, json.filename, response?.data?.imagegrouphdf5?.id)
        return response
    }

    private void convert(SecUser currentUser, def imagesSequenceList, def destination, def id){
        imagesSequenceList.sort{a,b ->
            if (a.channel == b.channel && a.time == b.time)
                a.zStack <=> b.zStack
            else if (a.channel == b.channel)
                a.time <=> b.time
            else
                a.channel <=> b.channel
        }
        def maxBits = 8
        def imagesFilenames = imagesSequenceList.collect {
            def baseImage = it.image.baseImage
            def absolutePath =  baseImage.getPath()
            def path = baseImage.path
            def basePath = absolutePath - path
            basePath + baseImage.filename
        }

        imagesSequenceList.each {
            maxBits = Math.max(maxBits, it.image.baseImage.bitDepth ?: 8)
        }

        def body = [user: currentUser.id, files: imagesFilenames, dest: destination, id: id,
                    cytomine:UrlApi.serverUrl(), bpc:maxBits]

        log.info body

        String imageServerURL = grailsApplication.config.grails.imageServerURL[0]
        String url = "/multidim/convert.json"
        def http = new HTTPBuilder(imageServerURL)
        http.post( path: url, requestContentType: URLENC, body : body)
    }

    def update(ImageGroupHDF5 domain, def jsonNewData) {
        securityACLService.check(domain.container(),READ)

        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new EditCommand(user: currentUser)
        executeCommand(c,domain,jsonNewData)
    }

//    def retrieve(def ids) {
//        def id = Integer.parseInt(ids + "")
//        CytomineDomain domain = currentDomain().get(id)
//        if (!domain) {
//            throw new ObjectNotFoundException("${currentDomain().class} " + id + " not found")
//        }
//        return domain
//    }

    def delete(ImageGroupHDF5 domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        securityACLService.check(domain.container(),READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser, transaction:transaction)
        return executeCommand(c,domain,null)
    }
}
