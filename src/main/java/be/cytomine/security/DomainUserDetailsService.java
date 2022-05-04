package be.cytomine.security;

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
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repository.security.SecUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
@RequiredArgsConstructor
public class DomainUserDetailsService implements UserDetailsService {

    private final Logger log = LoggerFactory.getLogger(DomainUserDetailsService.class);

    private final SecUserRepository secUserRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);

        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
        return secUserRepository.findByUsernameLikeIgnoreCase(lowercaseLogin)
                .map(user -> createSpringSecurityUser(lowercaseLogin, user))
                .orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database"));

    }
    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String lowercaseLogin, SecUser user) {
        if (!user.getEnabled()) {
            throw new ForbiddenException("User " + lowercaseLogin + " was not permitted");
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                !user.getAccountExpired(),
                !user.getPasswordExpired(),
                !user.getAccountLocked(),
                user.getRoles().stream().map(x -> new SimpleGrantedAuthority(x.getAuthority())).collect(Collectors.toList())
        );
    }
}
