package be.cytomine.processing.job

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

import be.cytomine.ontology.Term
import be.cytomine.processing.Job
import be.cytomine.security.UserJob

/**
 * Software that compute suggest term of each annotation of a project
 * It compute prediction every X days (or week,month, year,...) between two dates
 */
class RetrievalEvolutionJobService {

    static transactional = false
    def algoAnnotationTermService

    def listAVGEvolution(Job job) {
        List<UserJob> userJobs = UserJob.findAllByJob(job, [sort : "created", order: "desc"])
        return algoAnnotationTermService.listAVGEvolution(userJobs, job.project)
    }

    def listAVGEvolution(Job job, Term term) {
        List<UserJob> userJobs = UserJob.findAllByJob(job, [sort : "created", order: "desc"])
        return algoAnnotationTermService.listAVGEvolution(userJobs, job.project,term)
    }
}
