package be.cytomine.test.http

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

import be.cytomine.test.Infos

class StatsAPI  extends DomainAPI {

    static def statTerm(Long id, String username, String password, Long startDate=null, Long endDate=null) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/term.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "")
        return doGET(URL, username, password)
    }

    static def statUser(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/user.json"
        return doGET(URL, username, password)
    }

    static def statTermSlide(Long id, String username, String password, Long startDate=null, Long endDate=null) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/termslide.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "")
        return doGET(URL, username, password)
    }

    static def statUserSlide(Long id, String username, String password, Long startDate=null, Long endDate=null) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/userslide.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "")
        return doGET(URL, username, password)
    }

    static def statUserAnnotations(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/userannotations.json"
        return doGET(URL, username, password)
    }

    static def statAnnotationEvolution(Long id, String username, String password, Long startDate=null, Long endDate=null, Long term=null) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/annotationevolution.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "") +
                (term ? "&term=$term" : "")
        return doGET(URL, username, password)
    }

    static def statAlgoAnnotationEvolution(Long id, String username, String password, Long startDate=null, Long endDate=null, Long term=null) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/algoannotationevolution.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "") +
                (term ? "&term=$term" : "")
        return doGET(URL, username, password)
    }

    static def statReviewedAnnotationEvolution(Long id, String username, String password, Long startDate=null, Long endDate=null, Long term=null) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/reviewedannotationevolution.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "") +
                (term ? "&term=$term" : "")
        return doGET(URL, username, password)
    }

    static def statAnnotationActionEvolution(Long id, String username, String password, Long startDate=null, Long endDate=null) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/annotationactionsevolution.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "")
        return doGET(URL, username, password)
    }

    static def statImageConsultationEvolution(Long id, String username, String password, Long startDate=null, Long endDate=null) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/imageconsultationsevolution.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "")
        return doGET(URL, username, password)
    }

    static def statProjectConnectionEvolution(Long id, String username, String password, Long startDate=null, Long endDate=null) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/connectionsevolution.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "")
        return doGET(URL, username, password)
    }

    static def statAnnotationTermedByProject(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/term/" + id + "/project/stat.json"
            return doGET(URL, username, password)
    }

    static def totalProjects(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/total/project.json"
        return doGET(URL, username, password)
    }

    static def totalUsers(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/total/user.json"
        return doGET(URL, username, password)
    }

    static def totalNumberOfConnectionsByProject(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/total/project/connections.json"
        return doGET(URL, username, password)
    }

    static def statsOfCurrentActions(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/stats/currentStats.json"
        return doGET(URL, username, password)
    }

    static def statUsedStorage(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/stats/imageserver/total.json"
        return doGET(URL, username, password)
    }
}