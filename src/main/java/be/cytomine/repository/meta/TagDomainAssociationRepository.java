package be.cytomine.repository.meta;

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

import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.meta.Tag;
import be.cytomine.domain.meta.TagDomainAssociation;
import org.springframework.beans.PropertyValues;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

public interface TagDomainAssociationRepository extends JpaRepository<TagDomainAssociation, Long>, JpaSpecificationExecutor<TagDomainAssociation>  {

    long countByTag(Tag tag);

    List<TagDomainAssociation> findAllByTag(Tag tag);

    Optional<TagDomainAssociation> findByTagAndDomainClassNameAndDomainIdent(Tag tag, String domainClassName, Long domainIdent);


    @Override
    @EntityGraph(attributePaths = {"tag"})
    List<TagDomainAssociation> findAll(@Nullable Specification<TagDomainAssociation> spec, Sort sort);

    void deleteAllByDomainIdent(Long id);

    List<TagDomainAssociation> findAllByDomainIdent(Long id);
}
