package be.cytomine.utils

import be.cytomine.meta.AttachedFile
import be.cytomine.meta.Configuration
import be.cytomine.meta.Description

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

import be.cytomine.CytomineDomain
import be.cytomine.image.*
import be.cytomine.image.server.*
import be.cytomine.laboratory.*
import be.cytomine.ontology.*
import be.cytomine.processing.*
import be.cytomine.project.*
import be.cytomine.search.*
import be.cytomine.security.*
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.DomainAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

/*
* Will test the Resource Naming Convention for Restful app
* */
class RestUrlTests {

    def lowercase = [
            [clazz:Storage, filters : []],
            [clazz:AbstractImage, filters : []],
            [clazz:UploadedFile, filters : []],
            [clazz:Sample, filters : []],
            [clazz:AlgoAnnotation, filters : []],
            [clazz:Ontology, filters : []],
            [clazz:Relation, filters : []],
            [clazz:ReviewedAnnotation, filters : []],
            [clazz:Term, filters : []],
            [clazz:UserAnnotation, filters : []],
            [clazz:ImageFilter, filters : []],
            [clazz:ImageFilterProject, filters : []],
            [clazz:Job, filters : []],
            //[clazz:Job, filters : [Project]],
            //[clazz:Job, filters : [Software]],
            [clazz:JobData, filters : []],
            [clazz:JobParameter, filters : []],
            //[clazz:JobParameter, filters : [Job]],
            [clazz:Software, filters : []],
            [clazz:SoftwareProject, filters : []],
            [clazz:Discipline, filters : []],
            [clazz:Project, filters : []],
            [clazz:SearchEngineFilter, filters : []],
            [clazz:Group, filters : []],
            [clazz:User, filters : []],
            [clazz:AttachedFile, filters: []],
            [clazz:Configuration, filters: []],
            [clazz:Description, filters: []],
            [clazz:News, filters : []]
    ]
    def snake_case = [
            [clazz:SoftwareParameter, filters : []],
            [clazz:SoftwareUserRepository, filters : []],
            [clazz:ParameterConstraint, filters : []],
            [clazz:SoftwareParameterConstraint, filters : [SoftwareParameter]],
    ]


    public void testMain() {
        lowercase.each {
            String URL = getLowerCaseURL(it)
            testUrl(URL)
        }
        snake_case.each {
            String URL = getSnakeCaseURL(it)
            testUrl(URL)
        }
    }

    private String getLowerCaseURL(def testCase){
        String URL = testCase.clazz.simpleName.toLowerCase()
        if(!testCase.filters.isEmpty()){
            // only 1 filter is currently tested
            assert testCase.filters.size() == 1
            CytomineDomain domain = BasicInstanceBuilder."get${testCase.filters[0].simpleName}NotExist"(true)
            URL = testCase.filters[0].simpleName.toLowerCase() +"/" + domain.id + "/" + URL
        }
        URL = Infos.CYTOMINEURL + "api/" + URL +".json"

        println "URL is $URL"
        return URL
    }

    private String getSnakeCaseURL(def testCase){
        String URL = testCase.clazz.simpleName.replaceAll( /([A-Z])/, /_$1/ ).toLowerCase().replaceAll( /^_/, '' )
        if(!testCase.filters.isEmpty()){
            // only 1 filter is currently tested
            assert testCase.filters.size() == 1
            CytomineDomain domain = BasicInstanceBuilder."get${testCase.filters[0].simpleName}NotExist"(true)
            URL = testCase.filters[0].simpleName.replaceAll( /([A-Z])/, /_$1/ ).toLowerCase().replaceAll( /^_/, '' ) +"/" + domain.id + "/" + URL
        }
        URL = Infos.CYTOMINEURL + "api/" + URL +".json"

        println "URL is $URL"
        return URL
    }

    private void testUrl(String URL){
        def result = DomainAPI.doGET(URL, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }
}
