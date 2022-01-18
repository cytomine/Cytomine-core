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
import be.cytomine.processing.structure.ConfusionMatrix
import be.cytomine.security.UserJob

/**
 * Software that suggest term for each annotation from a projecy
 */
class RetrievalSuggestedTermJobService {

    static transactional = false
    def jobService
    def algoAnnotationTermService

    def listAVGEvolution(UserJob userJob) {
        //Get all project userJob
        List userJobs = jobService.getAllLastUserJob(userJob?.job?.project,userJob?.job?.software)
        return algoAnnotationTermService.listAVGEvolution(userJobs, userJob?.job?.project)
    }

    double computeAVG(def userJob) {
       return algoAnnotationTermService.computeAVG(userJob)
   }

   double computeAVGAveragePerClass(def userJob) {
        return  algoAnnotationTermService.computeAVGAveragePerClass(userJob)
  }

    ConfusionMatrix computeConfusionMatrix(List<Term> projectTerms, def userJob) {
       return algoAnnotationTermService.computeConfusionMatrix(projectTerms,userJob)
   }

    Double computeRate(Job job) {
        if(job.rate==-1 && job.status==Job.SUCCESS) {
            try {
            job.rate = computeAVG(UserJob.findByJob(job))
            }catch(Exception e) {
                log.warn "computeRate is null:"+e.toString()
                job.rate = 0
            }
        }
        return job.rate
    }       
}
