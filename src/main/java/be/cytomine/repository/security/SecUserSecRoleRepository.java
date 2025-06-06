package be.cytomine.repository.security;

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

import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.User;
import be.cytomine.domain.security.SecUserSecRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface SecUserSecRoleRepository extends JpaRepository<SecUserSecRole, Long>, JpaSpecificationExecutor<SecUserSecRole> {

    @Query("select distinct s.secRole from SecUserSecRole s where s.secUser = ?1")
    Set<SecRole> findAllRoleByUser(User user);

    List<SecUserSecRole> findAllBySecUser(User user);

    Optional<SecUserSecRole> findBySecUserAndSecRole(User user, SecRole secRole);
}
