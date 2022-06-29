package be.cytomine.repository.image.server;

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

import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.SecUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.Tuple;
import java.util.List;

/**
 * Spring Data JPA repository for the user entity.
 */
@Repository
public interface StorageRepository extends JpaRepository<Storage, Long>, JpaSpecificationExecutor<Storage> {

    List<Storage> findAllByUser(SecUser user);

    @Query(value = "SELECT su.username, su.id, count(uf.id) as files, sum(uf.size) as size " +
            "FROM uploaded_file uf, sec_user su " +
            "WHERE su.id = uf.user_id AND uf.storage_id = :storageId "  +
            "GROUP BY su.username, su.id ", nativeQuery = true)
    List<Tuple> listStatsForUsers(Long storageId);;

}
