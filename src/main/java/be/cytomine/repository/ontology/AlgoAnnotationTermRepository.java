package be.cytomine.repository.ontology;

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

import be.cytomine.domain.ontology.*;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.UserJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AlgoAnnotationTermRepository extends JpaRepository<AlgoAnnotationTerm, Long>, JpaSpecificationExecutor<AlgoAnnotationTerm>  {
    default List<AlgoAnnotationTerm> findAllByAnnotation(AnnotationDomain annotation) {
        return findAllByAnnotationIdent(annotation.getId());
    }
    List<AlgoAnnotationTerm> findAllByAnnotationIdent(Long id);

    long countByProject(Project project);

    Optional<AlgoAnnotationTerm> findByAnnotationIdentAndTermAndUserJob(Long annotationId, Term term, UserJob userJob);

    Optional<AlgoAnnotationTerm> findByAnnotationIdentAndTerm(Long annotationId, Term term);

    long countByTerm(Term term);

    long countByExpectedTerm(Term term);

    List<AlgoAnnotationTerm> findAllByUserJob(UserJob user);
}
