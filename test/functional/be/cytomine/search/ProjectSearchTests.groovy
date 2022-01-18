package be.cytomine.search

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

import be.cytomine.project.Project
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.*
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class ProjectSearchTests {

    //bounds
    void testGetBounds() {
        Project p1 = BasicInstanceBuilder.getProjectNotExist(true)
        Project p2 = BasicInstanceBuilder.getProjectNotExist(true)
        BasicInstanceBuilder.getUserAnnotationNotExist(p1, true)
        String login = BasicInstanceBuilder.getRandomString()
        String pwd = BasicInstanceBuilder.getRandomString()
        User user = BasicInstanceBuilder.getAdmin(login, pwd)
        ProjectAPI.addUserProject(p1.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        ProjectAPI.addUserProject(p2.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        p1 = p1.refresh()

        def result = ProjectAPI.getBounds(login, pwd)
        assert 200 == result.code
        def json = JSON.parse(result.data)

        assert json.containsKey("numberOfImages")
        assert json.containsKey("numberOfAnnotations")
        assert json.containsKey("numberOfJobAnnotations")
        assert json.containsKey("numberOfReviewedAnnotations")


        def annots = [p1.countAnnotations, p2.countAnnotations]

        assert json.numberOfAnnotations.min == annots.min()
        assert json.numberOfAnnotations.max == annots.max()
    }

    //search
    void testGetSearch(){
        Project p1 = BasicInstanceBuilder.getProjectNotExist(true)
        p1.name = "T2"
        p1.save(flush: true)
        BasicInstanceBuilder.getUserAnnotationNotExist(p1, true)
        Project p2 = BasicInstanceBuilder.getProjectNotExist(true)
        p2.name = "S2"
        p2.save(flush: true)
        Project p3 = BasicInstanceBuilder.getProjectNotExist(true)
        p3.name = "S_intermediate_2_end"
        p3.save(flush: true)
        p1 = p1.refresh()
        p2 = p2.refresh()
        p3 = p3.refresh()

        User user = BasicInstanceBuilder.getAdmin(Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        ProjectAPI.addUserProject(p1.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        ProjectAPI.addUserProject(p2.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        ProjectAPI.addUserProject(p3.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)


        def result = ProjectAPI.list(Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size > 1
        Long id1 = json.collection[0].id
        Long id2 = json.collection[1].id

        //project.refresh()

        //name[ilike], ontology[in], currentUserContributor, currentUserManager, numberOfImages[lte, gte], membersCount[lte, gte], numberOfAnnotations[lte, gte],
        // numberOfJobAnnotations[lte, gte], numberOfReviewedAnnotations[lte, gte]

        def searchParameters = [[operator : "lte", field : "numberOfJobAnnotations", value:1]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size > 1

        searchParameters = [[operator : "like", field : "name", value:"S2"]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() >= 1
        assert ProjectAPI.containsInJSONList(p2.id,json)

        searchParameters = [[operator : "like", field : "name", value:"S*2"]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() >= 2
        assert ProjectAPI.containsInJSONList(p2.id,json)
        assert ProjectAPI.containsInJSONList(p3.id,json)

        searchParameters = [[operator : "like", field : "name", value:"S%2"]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() >= 2
        assert ProjectAPI.containsInJSONList(p2.id,json)
        assert ProjectAPI.containsInJSONList(p3.id,json)

        searchParameters = [[operator : "like", field : "name", value:"T2"]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ProjectAPI.containsInJSONList(p1.id,json)

        searchParameters = [[operator : "gte", field : "numberOfAnnotations", value:1]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ProjectAPI.containsInJSONList(p1.id,json)

        searchParameters = [[operator : "lte", field : "membersCount", value:10]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 400 == result.code

        result = ProjectAPI.list(searchParameters, true, true, true, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ProjectAPI.containsInJSONList(p1.id,json)

        searchParameters = [[operator : "lte", field : "membersCount", value:10], [operator : "gte", field : "membersCount", value:1]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 400 == result.code

        result = ProjectAPI.list(searchParameters, true, true, true, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size >= 2
        assert ProjectAPI.containsInJSONList(p1.id,json)

        searchParameters = [[operator : "in", field : "ontology_id", value:"null"]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        searchParameters = [[operator : "in", field : "ontology_id", value:"null,"+p1.ontology.id]]

        result = ProjectAPI.list(searchParameters, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray


        Project p4 = BasicInstanceBuilder.getProjectNotExist(true)
        p4.name = "T&test=5"
        p4.save(flush: true)
        p4 = p4.refresh()

        searchParameters = [[operator : "like", field : "name", value:"T&test=5"]]

        result = ProjectAPI.list(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert !ProjectAPI.containsInJSONList(p1.id,json)
        assert ProjectAPI.containsInJSONList(p4.id,json)


        searchParameters = [[operator : "like", field : "name", value:"T';DELETE FROM amqp_queue_config;SELECT * FROM project WHERE name LIKE 'T%X';--"]]

        result = ProjectAPI.list(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code // if multiple queries, error is returned. If 200 ==> OK
    }

    //pagination
    void testListProject() {
        BasicInstanceBuilder.getProjectNotExist(true)
        BasicInstanceBuilder.getProjectNotExist(true)

        def result = ProjectAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size > 1

        result = ProjectAPI.list(2,0,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 2
        Long id1 = json.collection[0].id
        Long id2 = json.collection[1].id

        result = ProjectAPI.list(1,0,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = ProjectAPI.list(1,1,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2


        result = ProjectAPI.list(true, true, true, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        size = json.size
        assert size > 1
        id1 = json.collection[0].id
        id2 = json.collection[1].id

        result = ProjectAPI.list(true, true, true, 1,0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1

        result = ProjectAPI.list(true, true, true, 1,1,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
    }

    void testListProjectByUser() {

        Project p1 = BasicInstanceBuilder.getProjectNotExist(true)
        Project p2 = BasicInstanceBuilder.getProjectNotExist(true)
        User user = BasicInstanceBuilder.getAdmin(Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        ProjectAPI.addUserProject(p1.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        ProjectAPI.addUserProject(p2.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)


        def result = ProjectAPI.listByUser(user.id, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size > 1
        long id1 = json.collection[0].id
        long id2 = json.collection[1].id

        result = ProjectAPI.listByUser(user.id, 1,0, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = ProjectAPI.listByUser(user.id, 1,1, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2

/* TODO
        result = ProjectAPI.listByUserLight(user.id,'creator',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        size = json.size
        assert size > 1
        id1 = json.collection[0].id
        id2 = json.collection[1].id

        result = ProjectAPI.listByUserLight(user.id,'admin',1,0,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = ProjectAPI.listByUserLight(user.id,'user',1,1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2
        */
    }





/* TODO

    void testListProjectByOntology() {
        Ontology ontology = BasicInstanceBuilder.getOntology()
        def result = ProjectAPI.listByOntology(ontology.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size > 1
        long id1 = json.collection[0].id
        long id2 = json.collection[1].id

        result = ProjectAPI.listByOntology(ontology.id, 1,0,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = ProjectAPI.listByOntology(ontology.id, 1,1,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2


    }

    void testListProjectBySoftware() {
        Software software = BasicInstanceBuilder.getSoftware()
        User user = BasicInstanceBuilder.getUser()
        def result = ProjectAPI.listBySoftware(software.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size > 1
        long id1 = json.collection[0].id
        long id2 = json.collection[1].id

        result = ProjectAPI.listBySoftware(software.id, 1,0,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = ProjectAPI.listBySoftware(software.id, 1,1,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2
    }
*/

}
