package be.cytomine.service;

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
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.repository.security.SecUserSecRoleRepository;
import be.cytomine.utils.WeakConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CurrentRoleService {

    @Autowired
    private SecUserSecRoleRepository secUserSecRoleRepository;

    public Map<String, Date> currentAdmins = new WeakConcurrentHashMap<>(120 * 60 * 1000); //admin session = 120 min max

    public void clearAllAdminSession() {
        currentAdmins.clear();
    }

    /**
     * Active an admin session for a user
     * (by default user with ROLE_ADMIN are connected as ROLE_USER)
     * @param user
     */
    public void activeAdminSession(SecUser user) {
        if(hasCurrentUserAdminRole(user)) {
            currentAdmins.put(user.getUsername(), new Date());
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            List<GrantedAuthority> authorities = new ArrayList<>(auth.getAuthorities());
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(auth.getPrincipal(),auth.getCredentials(),authorities);
            newAuth.setDetails(user);
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        } else {
            throw new ForbiddenException("You are not an admin!");
        }
    }

    /**
     * Disable admin session for a user
     * @param user
     */
    public void closeAdminSession(SecUser user) {
        if(hasCurrentUserAdminRole(user)) {
            currentAdmins.remove(user.getUsername());
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(auth.getAuthorities());
            authorities.removeIf(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
            Authentication newAuth = new UsernamePasswordAuthenticationToken(auth.getPrincipal(),auth.getCredentials(),authorities);
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        } else {
            throw new ForbiddenException("You are not an admin!");
        }
    }

    /**
     * Get all user roles (even disabled role)
     */
    public Set<SecRole> findRealRole(SecUser user) {
        return user.getRoles();
    }

    /**
     * Get all active roles
     */
    public Set<SecRole> findCurrentRole(SecUser user) {
        Set<SecRole> roles = findRealRole(user);
        boolean isSuperAdmin = roles.stream().anyMatch(role -> role.getAuthority().equals("ROLE_SUPER_ADMIN"));
        //role super admin don't need to open a admin session, so we don't remove the role admin from the current role
        //log.info "isSuperAdmin=$isSuperAdmin isAdmin=$isAdmin"
        if(!currentAdmins.containsKey(user.getUsername()) && !isSuperAdmin) {
            roles = roles.stream().filter(role -> !role.getAuthority().equals("ROLE_ADMIN")).collect(Collectors.toSet());
        }
        return roles;
    }

    public Set<String> findCurrentAuthorities(SecUser user) {
        return findCurrentRole(user).stream().map(SecRole::getAuthority).collect(Collectors.toSet());
    }

    public Set<String> findRealAuthorities(SecUser user) {
        return findRealRole(user).stream().map(SecRole::getAuthority).collect(Collectors.toSet());
    }

    /**
     * Check if user is admin (with admin session opened)
     */
    public boolean isAdminByNow(SecUser user) {
        Set<String> authorities = findCurrentAuthorities(user);
        return authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_SUPER_ADMIN");
    }

    public boolean isUserByNow(SecUser user) {
        Set<String> authorities = findCurrentAuthorities(user);
        return authorities.contains("ROLE_USER");
    }

    public boolean isGuestByNow(SecUser user) {
        Set<String> authorities = findCurrentAuthorities(user);
        return authorities.contains("ROLE_GUEST");
    }

    /**
     * Check if user is admin (with admin session closed or opened)
     */
    public boolean isAdmin(SecUser user) {
        return findRealAuthorities(user).contains("ROLE_ADMIN");
    }
    public boolean isUser(SecUser user) {
        return findRealAuthorities(user).contains("ROLE_USER");
    }
    public boolean isGuest(SecUser user) {
        return findRealAuthorities(user).contains("ROLE_GUEST");
    }

    public boolean hasCurrentUserAdminRole(SecUser user) {
        Set<String> authorities = findRealRole(user).stream().map(SecRole::getAuthority).collect(Collectors.toSet());
        return authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_SUPER_ADMIN");
    }

}
