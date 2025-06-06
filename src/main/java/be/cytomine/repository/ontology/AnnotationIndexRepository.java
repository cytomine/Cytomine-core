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

import be.cytomine.domain.image.SliceInstance;
import be.cytomine.domain.ontology.AnnotationIndex;
import be.cytomine.domain.security.User;
import be.cytomine.dto.annotation.AnnotationIndexLightDTO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnnotationIndexRepository extends JpaRepository<AnnotationIndex, Long>, JpaSpecificationExecutor<AnnotationIndex>  {

    @Query( value = "SELECT user_id as user, slice_id as slice ,count_annotation as countAnnotation,count_reviewed_annotation as countReviewedAnnotation " +
            " FROM annotation_index " +
            " WHERE slice_id = :slice", nativeQuery = true)
    List<AnnotationIndexLightDTO> findAllLightBySliceInstance(long slice);


    Optional<AnnotationIndexLightDTO> findOneBySliceAndUser(SliceInstance slice, User user);

    List<AnnotationIndexLightDTO> findAllBySlice(SliceInstance slice);

    void deleteAllBySlice(SliceInstance sliceInstance);

    void deleteAllByUser(User user);

    Optional<AnnotationIndexLightDTO> findOneBySliceInAndUser(List<SliceInstance> slices, User user);

    List<AnnotationIndexLightDTO> findAllBySliceIn(List<SliceInstance> slices);
}
