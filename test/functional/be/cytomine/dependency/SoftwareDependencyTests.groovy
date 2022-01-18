package be.cytomine.dependency

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
import be.cytomine.processing.JobData
import be.cytomine.processing.Software
import be.cytomine.processing.SoftwareProject
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.JobAPI
import be.cytomine.test.http.SoftwareAPI

class SoftwareDependencyTests {

    void testJobDependency() {
        //create a job with job data and log
        def dependentDomain = createJobWithDependency()
        def job = dependentDomain.first()
        BasicInstanceBuilder.checkIfDomainsExist(dependentDomain)

        //try to delete job
        assert (200 == JobAPI.delete(job.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code)

        //check if all dependency are not aivalable
        BasicInstanceBuilder.checkIfDomainsNotExist(dependentDomain)
/*
        //undo op (re create)
        assert (200 == JobAPI.undo(Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code)


        //check if all dependency are aivalable
        BasicInstanceBuilder.checkIfDomainsExist(dependentDomain)

        //redo op (re-delete)
        assert (200 == TermAPI.redo(Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code)

        //check if all dependency are not aivalable
        BasicInstanceBuilder.checkIfDomainsNotExist(dependentDomain)*/
    }

    void testSoftwareDependency() {
        //create a software with job and software_project
        def dependentDomain = createSoftwareWithDependency()
        def software = dependentDomain.first()
        BasicInstanceBuilder.checkIfDomainsExist(dependentDomain)

        //try to delete software
        assert (200 == SoftwareAPI.delete(software.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code)

        //check if all dependency are not aivalable
        BasicInstanceBuilder.checkIfDomainsNotExist(dependentDomain)

        //TODO test undo redo
    }

    void testSoftwareDeepDependency() {
        //create a software with job and software_project
        def dependentDomain = createSoftwareWithDependency(true)
        def software = dependentDomain.first()
        BasicInstanceBuilder.checkIfDomainsExist(dependentDomain)

        //try to delete software
        assert (200 == SoftwareAPI.delete(software.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code)

        //check if all dependency are not aivalable
        BasicInstanceBuilder.checkIfDomainsNotExist(dependentDomain)

        //TODO test undo redo
    }

    private def createJobWithDependency() {
        Job job =  BasicInstanceBuilder.getJobNotExist(true)
        JobData data =  BasicInstanceBuilder.getJobDataNotExist(job, true)

        def log = BasicInstanceBuilder.getAttachedFileNotExist()
        log.domainClassName = job.class.name
        log.domainIdent = job.id
        log.filename = "log.out"
        log.save(flush: true)

        return [job, data,log]
    }

    private def createSoftwareWithDependency(boolean deep = false) {
        Software software =  BasicInstanceBuilder.getSoftwareNotExist(true)
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        SoftwareProject sp =  BasicInstanceBuilder.getSoftwareProjectNotExist(software,project, true)
        Job job =  BasicInstanceBuilder.getJobNotExist(true, software, project)

        def result = [software, sp, job]
        if(deep) {
            JobData data =  BasicInstanceBuilder.getJobDataNotExist(job, true)
            result << data

            def log = BasicInstanceBuilder.getAttachedFileNotExist()
            log.domainClassName = job.class.name
            log.domainIdent = job.id
            log.filename = "log.out"
            log.save(flush: true)
            result << log
        }

        return result
    }

}
