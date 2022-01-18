package be.cytomine.perf

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
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.UserAPI
import be.cytomine.test.http.UserPositionAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by hoyoux on 11.04.16.
 */
class UserPositionPerformanceTests {

    void testPerfPositionListLastByImage() {

        /*def users = [];
        users << [login : Infos.SUPERADMINLOGIN, password : Infos.SUPERADMINPASSWORD]

        def result;
        (1..100).each {
            User userToAdd = BasicInstanceBuilder.getUserNotExist()
            def jsonUser = new JSONObject(userToAdd.encodeAsJSON()).put("password", "password").toString()
            println "jsonUser =" + jsonUser
            result = UserAPI.create(jsonUser.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
            assert 200 == result.code
            users << [login : userToAdd.username, password : "password"]
        }


        def image;
        (1..100).each {
            image = BasicInstanceBuilder.getImageInstance()
            def json = JSON.parse("{image:${image.id},lon:100,lat:100, zoom: 1}")

            users.each {
                result = UserPositionAPI.create(image.id, json.toString(),it.login, it.password)
                assert 200 == result.code
            }
        }

        def times = []
        long begin;
        long end;
        (1..100).each {
            begin = System.currentTimeMillis()
            result = UserPositionAPI.listLastByImage(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
            end = System.currentTimeMillis()

            times << (end-begin)
            assert 200 == result.code
        }
        println "times"
        println "max : "+times.max()+" ms"
        println "avg : "+times.sum()/times.size()+" ms"

        //Current
        //mean : 35 ms , max 1335) for 100 user with 1 position each
        //mean : 23.87 ms , max 165) for 100 user with 10 position each
        //mean : 20 ms , max 111) for 100 user with 100 position each
        //mean : 24.48 ms , max 128 for 200 user with 100 position each

    }


    void testPerfPositionListOnlineWithPositions() {

        def users = [];
        //users << [login : Infos.SUPERADMINLOGIN, password : Infos.SUPERADMINPASSWORD]
        def slice1 = BasicInstanceBuilder.getSliceInstance()
        def project = slice1.project
        def slice2 = BasicInstanceBuilder.getSliceInstanceNotExist(slice1.image, true)

        def result;
        (1..100).each {
            User userToAdd = BasicInstanceBuilder.getUserNotExist(true)
            users << userToAdd
        }


        def image;
        (1..100).each {

            users.each { user ->
                BasicInstanceBuilder.getPersistentUserPosition(slice1, user, true)
                BasicInstanceBuilder.getPersistentUserPosition(slice2, user, true)
            }
        }

        def times = []
        long begin;
        long end;
        (1..100).each {
            begin = System.currentTimeMillis()
            result = UserAPI.listOnline(project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
            end = System.currentTimeMillis()

            times << (end-begin)
            assert 200 == result.code
        }
        println "times"
        println "max : "+times.max()+" ms"
        println "avg : "+times.sum()/times.size()+" ms"

        //Current
        //max : 614 ms
        //avg : 53.62 ms
*/    }
}
