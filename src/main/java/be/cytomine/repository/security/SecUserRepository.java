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

import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.service.dto.JobLayerDTO;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface SecUserRepository extends JpaRepository<SecUser, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = "roles")
    Optional<SecUser> findById(String id);

    @EntityGraph(attributePaths = "roles")
    Optional<SecUser> findByUsernameLikeIgnoreCase(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<SecUser> findByPublicKey(String publicKey);

    @Query("select distinct secUser " +
            "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, SecUser as secUser "+
            "where aclObjectId.objectId = :projectId " +
            "and aclEntry.aclObjectIdentity = aclObjectId.id " +
            "and aclEntry.mask = 16 " +
            "and aclEntry.sid = aclSid.id " +
            "and aclSid.sid = secUser.username " +
            "and secUser.class = 'be.cytomine.domain.security.User'")
    List<SecUser> findAllAdminsByProjectId(Long projectId);


    default List<SecUser> findAllUsersByProjectId(Long projectId) {
        return findAllUsersByContainer(projectId);
    }

    default List<SecUser> findAllUsersByStorageId(Long storageId) {
        return findAllUsersByContainer(storageId);
    }

    @Query("select distinct secUser " +
            "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, User as secUser "+
            "where aclObjectId.objectId = :containerId " +
            "and aclEntry.aclObjectIdentity = aclObjectId.id " +
            "and aclEntry.sid = aclSid.id " +
            "and aclSid.sid = secUser.username " +
            "and secUser.class = 'be.cytomine.domain.security.User'")
    List<SecUser> findAllUsersByContainer(Long containerId);


//     @Query(value = "SELECT DISTINCT u.id as id, u.username as username, " +
//             "s.name as softwareName, s.software_version as softwareVersion, " +
//             "j.created as created, u.job_id as job " +
//             "FROM annotation_index ai " +
//             "RIGHT JOIN slice_instance si ON ai.slice_id = si.id " +
//             "RIGHT JOIN sec_user u ON ai.user_id = u.id " +
//             "RIGHT JOIN job j ON j.id = u.job_id " +
//             "RIGHT JOIN software_project sp ON sp.software_id = j.software_id " +
//             "RIGHT JOIN software s ON s.id = sp.software_id " +
//             "WHERE si.image_id = :imageInstanceId " +
//             "AND sp.project_id = :projectInstanceId " +
//             "ORDER BY j.created", nativeQuery = true)
//     List<JobLayerDTO> findAllUserJob(Long imageInstanceId, Long projectInstanceId);



    @Query(value = "SELECT DISTINCT sec_user.id \n" +
                " FROM acl_object_identity, acl_entry,acl_sid, sec_user \n" +
                " WHERE acl_object_identity.object_id_identity = :domainId\n" +
                " AND acl_entry.acl_object_identity=acl_object_identity.id\n" +
                " AND acl_entry.sid = acl_sid.id " +
                " AND acl_sid.sid = sec_user.username " +
                " AND sec_user.class = 'be.cytomine.domain.security.User'", nativeQuery = true)
    List<Long> findAllAllowedUserIdList(Long domainId);

    List<SecUser> findAllByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = "roles")
    Optional<SecUser> findByPublicKeyAndEnabled(String accessKey, boolean enabled);

    @Query(value = "select distinct secUser " +
            "from AclSid as aclSid, AclEntry as aclEntry, SecUser as secUser "+
            "where aclEntry.aclObjectIdentity in (select  aclEntry.aclObjectIdentity from AclEntry as aclEntry where aclEntry.sid.id = :sidId) " +
            "and aclEntry.sid = aclSid and aclSid.sid = secUser.username and aclSid.id <> :sidId")
    List<SecUser> findAllSecUsersSharingAccesToSameProject(Long sidId);

    @Query(value = "SELECT id FROM acl_sid WHERE sid = :username", nativeQuery = true)
    Long getAclSidFromUsername(String username);

    default List<SecUser> findAllSecUsersSharingAccesToSameProject(String username) {
        Long aclId = getAclSidFromUsername(username);
        return findAllSecUsersSharingAccesToSameProject(aclId);
    }


}
