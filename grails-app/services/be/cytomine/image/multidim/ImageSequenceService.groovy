package be.cytomine.image.multidim

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

import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.*
import be.cytomine.image.ImageInstance
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.springframework.security.acls.domain.BasePermission.READ

class ImageSequenceService extends ModelService {

    static transactional = true

    def cytomineService
    def transactionService
    def userAnnotationService
    def algoAnnotationService
    def dataSource
    def reviewedAnnotationService
    def securityACLService
    def imageInstanceService

    def currentDomain() {
        return ImageSequence
    }

    def read(def id) {
        def image = ImageSequence.read(id)
        if(image) {
            securityACLService.check(image.container(),READ)
        }
        image
    }

    def get(def id) {
        def image = ImageSequence.get(id)
        if(image) {
            securityACLService.check(image.container(),READ)
        }
        image
    }

    def get(ImageInstance image) {
        ImageSequence.findAllByImage(image)
    }

    def list(ImageGroup imageGroup) {
        ImageSequence.findAllByImageGroup(imageGroup)
    }

    def getPossibilities(ImageInstance image) {

        def imageSeq = ImageSequence.findByImage(image)
        if (!imageSeq)  {
            return [slice: null,zStack:null,time:null,channel:null, imageGroup:null]
        }

        def poss = ImageSequence.findAllByImageGroup(imageSeq.imageGroup)

        def z = []
        def t = []
        def c = []
        def s = []

        poss.each {
            z << it.zStack
            t << it.time
            c << it.channel
            s << it.slice
        }

        z = z.unique().sort()
        t = t.unique().sort()
        c = c.unique().sort()
        s = s.unique().sort()
        return [slice:s,zStack:z,time:t,channel:c, imageGroup:imageSeq.imageGroup.id,c:imageSeq.channel,z:imageSeq.zStack,s:imageSeq.slice,t:imageSeq.time]
    }

    //channel,zStack,slice,time
    def get(ImageGroup imageGroup,Integer channel,Integer zStack,Integer slice, Integer time) {
        def data = ImageSequence.findWhere([imageGroup:imageGroup,slice:slice,zStack:zStack,time:time,channel:channel])
        if (!data) {
             throw new ObjectNotFoundException("There is no sequence value for this image group [${imageGroup.id}] and theses values=[$channel,$zStack,$slice,$time,]")
        }
        data
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        //If the information about multidim hasn't been set, set it automatically according to filename
        if(!(json.channel && json.time && json.zstack)){
            ImageInstance imageInstance = imageInstanceService.read(json.image)
            if (imageInstance)  {
                def filename = imageInstance.baseImage.originalFilename

                Pattern patternZstack = Pattern.compile("-z[0-9]*");
                Pattern patternChannel = Pattern.compile("-c[0-9]*");
                Pattern patternTime = Pattern.compile("-t[0-9]*");

                Matcher matcher = patternZstack.matcher(filename);
                if (matcher.find()) {
                    json.zstack = matcher.group(0).substring(2)
                }
                matcher = patternTime.matcher(filename);
                if (matcher.find()) {
                    json.time = matcher.group(0).substring(2)
                }
                matcher = patternChannel.matcher(filename);
                if (matcher.find()) {
                    json.channel = matcher.group(0).substring(2)
                }

            }
            else {
                return;
            }
        }
        securityACLService.check(json.imageGroup,ImageGroup,"container",READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        json.user = currentUser.id
        synchronized (this.getClass()) {
            Command c = new AddCommand(user: currentUser)
            executeCommand(c,null,json)
        }
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(ImageSequence domain, def jsonNewData) {
        securityACLService.check(domain.container(),READ)

        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new EditCommand(user: currentUser)
        executeCommand(c,domain,jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(ImageSequence domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        securityACLService.check(domain.container(),READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    // TODO: OK if group/sequence created from a multidimensional file (eg. ome.tiff)
    // TODO: But images should be kept if the group is "artificial" (eg painting)
    def abstractImageService
    def deleteDependentImageInstance(ImageSequence domain, Transaction transaction, Task task = null) {
        imageInstanceService.delete(domain.image,transaction,null,false)
        abstractImageService.delete(domain.image.baseImage)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id,  domain.image.id, domain.imageGroup.id]
    }
}
