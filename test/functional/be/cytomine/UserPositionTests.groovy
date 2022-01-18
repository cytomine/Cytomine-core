package be.cytomine

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

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.UserAPI
import be.cytomine.test.http.UserPositionAPI
import grails.converters.JSON

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 16/03/11
 * Time: 16:12
 * To change this template use File | Settings | File Templates.
 */
class UserPositionTests  {


    void testLastByUser() {
        def image = BasicInstanceBuilder.getImageInstance()
        def result = UserPositionAPI.listLastByUser(image.id,BasicInstanceBuilder.user1.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testLastBroadcastByUser() {
        def image = BasicInstanceBuilder.getImageInstance()
        def json = "{image:${image.id},topLeftX:100, topLeftY:100, topRightX: 200, topRightY:100, bottomLeftX: 100, bottomLeftY : 200, bottomRightX : 200; bottomRightY : 200, zoom: 1}"
        def result = UserPositionAPI.create(image.id, json, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        Long creator = JSON.parse(result.data).user

        result = UserPositionAPI.listLastByUser(image.id, creator, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, true)
        assert 200 == result.code
        assert JSON.parse(result.data).id == null

        json = "{image:${image.id},topLeftX:100, topLeftY:100, topRightX: 200, topRightY:100, bottomLeftX: 100, bottomLeftY : 200, bottomRightX : 200; bottomRightY : 200, zoom: 1, broadcast: true}"
        UserPositionAPI.create(image.id, json, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        result = UserPositionAPI.listLastByUser(image.id, creator, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, true)
        assert 200 == result.code
        assert JSON.parse(result.data).id != null
    }

    void testListByImage() {
        def image = BasicInstanceBuilder.getImageInstance()
        def result = UserPositionAPI.listLastByImage(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testListBroadcastByImage() {
        def image = BasicInstanceBuilder.getImageInstance()
        def json = "{image:${image.id},topLeftX:100, topLeftY:100, topRightX: 200, topRightY:100, bottomLeftX: 100, bottomLeftY : 200, bottomRightX : 200; bottomRightY : 200, zoom: 1}"
        UserPositionAPI.create(image.id, json, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        def result = UserPositionAPI.listLastByImage(image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, true)
        assert 200 == result.code
        assert JSON.parse(result.data).users.size() == 0

        json = "{image:${image.id},topLeftX:100, topLeftY:100, topRightX: 200, topRightY:100, bottomLeftX: 100, bottomLeftY : 200, bottomRightX : 200; bottomRightY : 200, zoom: 1, broadcast: true}"
        UserPositionAPI.create(image.id, json, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        result = UserPositionAPI.listLastByImage(image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, true)
        assert 200 == result.code
        assert JSON.parse(result.data).users.size() == 1
    }

    void testList() {
        def image = BasicInstanceBuilder.getImageInstance()
        def json = JSON.parse("{image:${image.id},topLeftX:100, topLeftY:100, topRightX: 200, topRightY:100, bottomLeftX: 100, bottomLeftY : 200, bottomRightX : 200; bottomRightY : 200, zoom: 1}")

        def result = UserPositionAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        Long creator = JSON.parse(result.data).user

        result = UserPositionAPI.listByImage(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = UserPositionAPI.listByImageAndUser(image.id, creator, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = UserPositionAPI.listByImageAndUser(image.id, BasicInstanceBuilder.user1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 0
    }

    void testListAfterThan() {
        def image = BasicInstanceBuilder.getImageInstance()
        def json = JSON.parse("{image:${image.id},topLeftX:100, topLeftY:100, topRightX: 200, topRightY:100, bottomLeftX: 100, bottomLeftY : 200, bottomRightX : 200; bottomRightY : 200, zoom: 1}")

        def result = UserPositionAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        Long created = Long.parseLong(JSON.parse(result.data).created)
        Long creator = JSON.parse(result.data).user

        result = UserPositionAPI.listByImage(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, created)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = UserPositionAPI.listByImageAndUser(image.id, creator, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, created)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 1

        result = UserPositionAPI.listByImage(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, created+1)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 0

        result = UserPositionAPI.listByImageAndUser(image.id, creator, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, created+1)
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == 0
    }

    void testSummarize() {

        def image = BasicInstanceBuilder.getImageInstance()
        def json = JSON.parse("{image:${image.id},topLeftX:100, topLeftY:100, topRightX: 200, topRightY:100, bottomLeftX: 100, bottomLeftY : 200, bottomRightX : 200; bottomRightY : 200, zoom: 1}")

        def result
        (1..3).each {
            result = UserPositionAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
            assert 200 == result.code
        }
        json = JSON.parse("{image:${image.id},topLeftX:150, topLeftY:150, topRightX: 250, topRightY:150, bottomLeftX: 150, bottomLeftY : 250, bottomRightX : 250; bottomRightY : 250, zoom: 1}")
        result = UserPositionAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        Long creator = JSON.parse(result.data).user

        result = UserPositionAPI.summarizeByImage(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def data = JSON.parse(result.data).collection
        assert data.size() == 2
        assert (data[0].frequency == 1 && data[1].frequency == 3) || (data[0].frequency == 3 && data[1].frequency == 1)

        result = UserPositionAPI.summarizeByImageAndUser(image.id,creator, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        data = JSON.parse(result.data).collection
        assert data.size() == 2
        assert (data[0].frequency == 1 && data[1].frequency == 3) || (data[0].frequency == 3 && data[1].frequency == 1)

        result = UserPositionAPI.summarizeByImageAndUser(image.id, BasicInstanceBuilder.user1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        data = JSON.parse(result.data).collection
        assert data.size() == 0
    }

    void testSummarizeAfterThan() {
        def image = BasicInstanceBuilder.getImageInstance()
        def json = JSON.parse("{image:${image.id},topLeftX:100, topLeftY:100, topRightX: 200, topRightY:100, bottomLeftX: 100, bottomLeftY : 200, bottomRightX : 200; bottomRightY : 200, zoom: 1}")

        def result = UserPositionAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Long created = Long.parseLong(JSON.parse(result.data).created)

        result = UserPositionAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = UserPositionAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        json = JSON.parse("{image:${image.id},topLeftX:150, topLeftY:150, topRightX: 250, topRightY:150, bottomLeftX: 150, bottomLeftY : 250, bottomRightX : 250; bottomRightY : 250, zoom: 1}")
        result = UserPositionAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        Long created2 = Long.parseLong(JSON.parse(result.data).created)
        assert 200 == result.code


        result = UserPositionAPI.summarizeByImage(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, created)
        assert 200 == result.code
        def data = JSON.parse(result.data).collection
        assert data.size() == 2
        assert (data[0].frequency == 1 && data[1].frequency == 3) || (data[0].frequency == 3 && data[1].frequency == 1)

        result = UserPositionAPI.summarizeByImage(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, created2)
        assert 200 == result.code
        data = JSON.parse(result.data).collection
        assert data.size() == 1
        assert data[0].frequency == 1

        result = UserPositionAPI.summarizeByImage(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, created2+1)
        assert 200 == result.code
        data = JSON.parse(result.data).collection
        assert data.size() == 0
    }

    void testAddPosition() {
        def image = BasicInstanceBuilder.getImageInstance()
        def json = JSON.parse("{image:${image.id},topLeftX:100, topLeftY:100, topRightX: 200, topRightY:100, bottomLeftX: 100, bottomLeftY : 200, bottomRightX : 200; bottomRightY : 200, zoom: 1}")

        def result = UserPositionAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Long creator = JSON.parse(result.data).user

        result = UserPositionAPI.listLastByUser(image.id,creator,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert creator == JSON.parse(result.data).user
        result = UserPositionAPI.listLastByImage(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
         //same position, user don't move
        result = UserPositionAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testListOnlineFriendsWithOpenedImages() {
        def slice1 = BasicInstanceBuilder.getSliceInstance()
        def project = slice1.project
        def slice2 = BasicInstanceBuilder.getSliceInstanceNotExist(slice1.image, true)
        def user1 = BasicInstanceBuilder.getUserNotExist(true)
        def user2 = BasicInstanceBuilder.getUserNotExist(true)

        BasicInstanceBuilder.getPersistentUserPosition(slice1, user1, true)
        BasicInstanceBuilder.getPersistentUserPosition(slice2, user1, true)
        BasicInstanceBuilder.getPersistentUserPosition(slice2, user2,true)

        def result = UserAPI.listOnline(project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        def json = JSON.parse(result.data)

        assert (json.collection.find{it.id == user1.id})
        assert (json.collection.find{it.id == user2.id})
        assert (json.collection.find{it.id == user1.id}.position.size() == 2)
        assert (json.collection.find{it.id == user2.id}.position.size() == 1)
        assert (json.collection.find{it.id == user1.id}.position.find{it.slice == slice1.id})
        assert (json.collection.find{it.id == user1.id}.position.find{it.slice == slice2.id})
        assert (json.collection.find{it.id == user2.id}.position.find{it.slice == slice2.id})
        assert (json.collection.find{it.id == user2.id}.position.find{it.slice == slice1.id} == null)

        assert 200 == result.code
    }
}
