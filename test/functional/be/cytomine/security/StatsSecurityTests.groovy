package be.cytomine.security

import be.cytomine.ontology.Term

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
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.StatsAPI
import be.cytomine.test.http.TermAPI
import grails.converters.JSON

class StatsSecurityTests extends SecurityTestsAbstract {

    void testStatsSecurityForCytomineAdmin() {

        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        def result

        result = StatsAPI.statTerm(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = StatsAPI.statUser(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = StatsAPI.statTermSlide(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = StatsAPI.statUserSlide(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = StatsAPI.statUserAnnotations(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = StatsAPI.statAnnotationEvolution(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = StatsAPI.statAlgoAnnotationEvolution(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = StatsAPI.statReviewedAnnotationEvolution(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = StatsAPI.statAnnotationActionEvolution(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = StatsAPI.statProjectConnectionEvolution(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = StatsAPI.statImageConsultationEvolution(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        Term term  = BasicInstanceBuilder.getTermNotExist(true);
        result = StatsAPI.statAnnotationTermedByProject(term.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = StatsAPI.totalProjects(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = StatsAPI.totalUsers(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }
    void testStatsSecurityForGoodUser() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        def result

        User user = BasicInstanceBuilder.getUser2();
        ProjectAPI.addUserProject(project.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        result = StatsAPI.statTerm(project.id, "user2", "password")
        assert 200 == result.code
        result = StatsAPI.statUser(project.id, "user2", "password")
        assert 200 == result.code
        result = StatsAPI.statTermSlide(project.id, "user2", "password")
        assert 200 == result.code
        result = StatsAPI.statUserSlide(project.id, "user2", "password")
        assert 200 == result.code
        result = StatsAPI.statUserAnnotations(project.id, "user2", "password")
        assert 200 == result.code
        result = StatsAPI.statAnnotationEvolution(project.id, "user2", "password")
        assert 200 == result.code
        result = StatsAPI.statAlgoAnnotationEvolution(project.id, "user2", "password")
        assert 200 == result.code
        result = StatsAPI.statReviewedAnnotationEvolution(project.id, "user2", "password")
        assert 200 == result.code
        result = StatsAPI.statAnnotationActionEvolution(project.id, "user2", "password")
        assert 200 == result.code
        result = StatsAPI.statProjectConnectionEvolution(project.id, "user2", "password")
        assert 200 == result.code
        result = StatsAPI.statImageConsultationEvolution(project.id, "user2", "password")
        assert 200 == result.code

        result = StatsAPI.totalProjects("user2", "password")
        assert 403 == result.code
        result = StatsAPI.totalUsers("user2", "password")
        assert 403 == result.code
    }
    void testStatsSecurityForBadUser() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        def result

        BasicInstanceBuilder.getUser2();

        result = StatsAPI.statTerm(project.id, "user2", "password")
        assert 403 == result.code
        result = StatsAPI.statUser(project.id, "user2", "password")
        assert 403 == result.code
        result = StatsAPI.statTermSlide(project.id, "user2", "password")
        assert 403 == result.code
        result = StatsAPI.statUserSlide(project.id, "user2", "password")
        assert 403 == result.code
        result = StatsAPI.statUserAnnotations(project.id, "user2", "password")
        assert 403 == result.code
        result = StatsAPI.statAnnotationEvolution(project.id, "user2", "password")
        assert 403 == result.code
        result = StatsAPI.statAlgoAnnotationEvolution(project.id, "user2", "password")
        assert 403 == result.code
        result = StatsAPI.statReviewedAnnotationEvolution(project.id, "user2", "password")
        assert 403 == result.code
        result = StatsAPI.statAnnotationActionEvolution(project.id, "user2", "password")
        assert 403 == result.code
        result = StatsAPI.statProjectConnectionEvolution(project.id, "user2", "password")
        assert 403 == result.code
        result = StatsAPI.statImageConsultationEvolution(project.id, "user2", "password")
        assert 403 == result.code

        Term term  = BasicInstanceBuilder.getTermNotExist(true);
        result = StatsAPI.statAnnotationTermedByProject(term.id, "user2", "password")
        assert 403 == result.code

        result = StatsAPI.totalProjects("user2", "password")
        assert 403 == result.code
        result = StatsAPI.totalUsers("user2", "password")
        assert 403 == result.code
    }
    void testStatsSecurityForNonUser() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        def result

        result = StatsAPI.statTerm(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
        result = StatsAPI.statUser(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
        result = StatsAPI.statTermSlide(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
        result = StatsAPI.statUserSlide(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
        result = StatsAPI.statUserAnnotations(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
        result = StatsAPI.statAnnotationEvolution(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
        result = StatsAPI.statAlgoAnnotationEvolution(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
        result = StatsAPI.statReviewedAnnotationEvolution(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
        result = StatsAPI.statAnnotationActionEvolution(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
        result = StatsAPI.statProjectConnectionEvolution(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
        result = StatsAPI.statImageConsultationEvolution(project.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code

        Term term  = BasicInstanceBuilder.getTermNotExist(true);
        result = StatsAPI.statAnnotationTermedByProject(term.id, Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code

        result = StatsAPI.totalProjects(Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
        result = StatsAPI.totalUsers(Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
    }
}
