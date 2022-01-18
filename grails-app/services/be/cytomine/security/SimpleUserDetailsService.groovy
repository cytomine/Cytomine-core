package be.cytomine.security

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

import grails.plugin.springsecurity.userdetails.GormUserDetailsService
import grails.plugin.springsecurity.userdetails.GrailsUser
import org.springframework.dao.DataAccessException
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.transaction.annotation.Transactional

class SimpleUserDetailsService extends GormUserDetailsService {

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username, boolean loadRoles) throws UsernameNotFoundException, DataAccessException {
        GrailsUser grailsUser
        SecUser.withTransaction {

            SecUser user = SecUser.findByUsernameIlike(username)

            def authorities = []

            def auth = SecUserSecRole.findAllBySecUser(user).collect{new GrantedAuthorityImpl(it.secRole.authority)}
            //by default, we remove the role_admin for the current session
            authorities.addAll(auth.findAll{it.authority!="ROLE_ADMIN"})

            grailsUser = new GrailsUser(user.username, user.password, user.enabled, !user.accountExpired,
                    !user.passwordExpired, !user.accountLocked,
                    authorities, user.id)
        }
        return grailsUser
    }
}