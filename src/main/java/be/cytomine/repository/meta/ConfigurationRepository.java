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

import be.cytomine.domain.meta.Configuration;
import be.cytomine.domain.meta.ConfigurationReadingRole;
import be.cytomine.security.ldap.LdapConfigurationInterface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

import static be.cytomine.service.meta.ConfigurationService.*;

public interface ConfigurationRepository extends JpaRepository<Configuration, Long>, JpaSpecificationExecutor<Configuration>, LdapConfigurationInterface {


    List<Configuration> findAllByReadingRole(ConfigurationReadingRole role);

    List<Configuration> findAllByReadingRoleIn(List<ConfigurationReadingRole> role);

    Optional<Configuration> findByKey(String key);

    @Override
    default String getServer() {
        return getLdapValue(CONFIG_KEY_LDAP_SERVER);
    }

    @Override
    default String getPrincipal() {
        return getLdapValue(CONFIG_KEY_LDAP_PRINCIPAL);
    }

    @Override
    default String getPassword() {
        return getLdapValue(CONFIG_KEY_LDAP_PASSWORD);
    }

    default String getLdapValue(String key) {
       return this.findByKey(key)
                .map(Configuration::getValue)
                .orElse("");
    }
}
