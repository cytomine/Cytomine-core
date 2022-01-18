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

import be.cytomine.processing.Job
import be.cytomine.project.Project
import be.cytomine.security.User
import be.cytomine.security.UserJob
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.JobAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class JobSearchTests {


    //search
    void testGetSearch(){
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        Job j1 = BasicInstanceBuilder.getJobNotExist(true, project)
        User u = BasicInstanceBuilder.getAdmin(Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        UserJob uj = BasicInstanceBuilder.getUserJobNotExist(j1, u, true)
        j1.software.name = "T"
        Date old = new Date().parse("dd.MM.yyy", '18.05.1988')
        j1.created = old
        j1.status = 7
        j1.save()
        Job j2 = BasicInstanceBuilder.getJobNotExist(true, j1.software, project)
        Job j3 = BasicInstanceBuilder.getJobNotExist(true, BasicInstanceBuilder.getSoftwareNotExist(true), project)
        j1 = j1.refresh()

        def result = JobAPI.listBySoftwareAndProject(j1.software.id,j1.project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,false)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size == 2


        def searchParameters = [[operator : "equals", field : "softwareName", value:"T"]]

        result = JobAPI.list(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 2
        assert JobAPI.containsInJSONList(j1.id,json)
        assert JobAPI.containsInJSONList(j2.id,json)

        searchParameters = [[operator : "in", field : "softwareName", value:"T"]]

        result = JobAPI.list(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 2
        assert JobAPI.containsInJSONList(j1.id,json)
        assert JobAPI.containsInJSONList(j2.id,json)

        searchParameters = [[operator : "lte", field : "created", value: new Date()]]

        result = JobAPI.listBySoftwareAndProject(j1.software.id,j1.project.id, 0, 0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 2

        searchParameters = [[operator : "lte", field : "created", value: old]]

        result = JobAPI.listBySoftwareAndProject(j1.software.id,j1.project.id, 0, 0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert JobAPI.containsInJSONList(j1.id,json)

        searchParameters = [[operator : "in", field : "status", value:7], [operator : "lte", field : "created", value:new Date()]]

        result = JobAPI.listBySoftwareAndProject(j1.software.id,j1.project.id, 0, 0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert JobAPI.containsInJSONList(j1.id,json)


        searchParameters = [[operator : "in", field : "username", value:"admin"]]

        result = JobAPI.listBySoftwareAndProject(j1.software.id,j1.project.id, 0, 0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert JobAPI.containsInJSONList(j1.id,json)


        searchParameters = [[operator : "in", field : "username", value:"admin,admin"]]

        result = JobAPI.listBySoftwareAndProject(j1.software.id,j1.project.id, 0, 0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 1
        assert JobAPI.containsInJSONList(j1.id,json)
    }


    //pagination
    void testListJob() {
        BasicInstanceBuilder.getJob()
        BasicInstanceBuilder.getJobNotExist(true, BasicInstanceBuilder.getJob().software, BasicInstanceBuilder.getJob().project)
        def result = JobAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size > 1
        Long id1 = json.collection[0].id
        Long id2 = json.collection[1].id

        result = JobAPI.list(1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert size == json.size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = JobAPI.list(1, 1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert size == json.size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2
    }

    void testListJobBySoftwareAndProjectWithCredential() {
        Job job = BasicInstanceBuilder.getJob()
        BasicInstanceBuilder.getJobNotExist(true, job.software, job.project)

        def result = JobAPI.listBySoftwareAndProject(job.software.id,job.project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,false)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size > 1
        Long id1 = json.collection[0].id
        Long id2 = json.collection[1].id

        result = JobAPI.listBySoftwareAndProject(job.software.id,job.project.id, 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,false)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert size == json.size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = JobAPI.listBySoftwareAndProject(job.software.id,job.project.id, 1, 1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,false)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert size == json.size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2
    }

    void testListJobBySoftwareAndProjectWithCredentialLight() {
        Job job = BasicInstanceBuilder.getJob()
        BasicInstanceBuilder.getJobNotExist(true, job.software, job.project)

        def result = JobAPI.listBySoftwareAndProject(job.software.id,job.project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,true)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size > 1
        Long id1 = json.collection[0].id
        Long id2 = json.collection[1].id

        result = JobAPI.listBySoftwareAndProject(job.software.id,job.project.id, 1, 0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,true)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert size == json.size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = JobAPI.listBySoftwareAndProject(job.software.id,job.project.id, 1, 1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,true)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert size == json.size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2
    }

    //sort
    void testSortJob() {
        Job job = BasicInstanceBuilder.getJobNotExist(true, BasicInstanceBuilder.getSoftwareNotExist(true), BasicInstanceBuilder.getProjectNotExist(true))
        Date old = new Date().parse("dd.MM.yyy", '20.05.1988')
        job.created = old
        job.status = 7
        job.save()

        BasicInstanceBuilder.getJobNotExist(true, job.software, job.project)

        def result = JobAPI.list("created", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        Long id1 = json.collection[0].id
        Long id2 = json.collection[-1].id

        result = JobAPI.list("created", "desc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        assert json.collection[0].id == id2
        assert json.collection[-1].id == id1

        result = JobAPI.listBySoftwareAndProject(job.software.id, job.project.id, "created", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, false)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        id1 = json.collection[0].id
        id2 = json.collection[-1].id

        result = JobAPI.listBySoftwareAndProject(job.software.id, job.project.id, "created", "desc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, false)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        assert json.collection[0].id == id2
        assert json.collection[-1].id == id1


        result = JobAPI.list("softwareName", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1

        String name=  json.collection[0].softwareName

        result = JobAPI.list("softwareName", "desc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        assert name == json.collection[-1].softwareName

        result = JobAPI.list("username", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1

        result = JobAPI.list("username", "desc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1


        result = JobAPI.list("status", "asc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1

        int status = json.collection[0].status

        result = JobAPI.list("status", "desc", Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() > 1
        assert status != json.collection[0].status
    }
}
