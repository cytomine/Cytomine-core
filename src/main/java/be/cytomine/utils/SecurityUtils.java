package be.cytomine.utils;

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
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.security.current.CurrentUser;
import be.cytomine.security.current.FullCurrentUser;
import be.cytomine.security.current.PartialCurrentUser;
import org.apache.commons.codec.binary.Base64;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

// TODO IAM: refactor/remove
public class SecurityUtils {
    public static String generateKeys(String method, String content_md5, String content_type, String date, String queryString, String path, SecUser user) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String canonicalHeaders = method + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n";
        String canonicalExtensionHeaders = "";
        String canonicalResource = path + queryString;
        String messageToSign = canonicalHeaders + canonicalExtensionHeaders + canonicalResource;

        String key = user.getPrivateKey();
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
        // get an hmac_sha1 Mac instance and initialize with the signing key
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        // compute the hmac on input data bytes
        byte[] rawHmac = mac.doFinal(new String(messageToSign.getBytes(), "UTF-8").getBytes());

        // base64-encode the hmac
        byte[] signatureBytes = Base64.encodeBase64(rawHmac);

        String signature = new String(signatureBytes);
        return signature;
    }

    public static Optional<CurrentUser> getSecurityCurrentUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        return Optional.ofNullable(extractCurrentUser(securityContext.getAuthentication()));
    }

    private static CurrentUser extractCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getDetails() instanceof SecUser) {
            FullCurrentUser fullCurrentUser = new FullCurrentUser();
            fullCurrentUser.setUser((SecUser)authentication.getDetails());
            return fullCurrentUser;
        } else if (authentication.getPrincipal() instanceof String) {
            PartialCurrentUser partialCurrentUser = new PartialCurrentUser();
            partialCurrentUser.setUsername((String)authentication.getPrincipal());
            return partialCurrentUser;
        } else if (authentication.getPrincipal() instanceof UserDetails) {
            PartialCurrentUser partialCurrentUser = new PartialCurrentUser();
            partialCurrentUser.setUsername(((UserDetails) authentication.getPrincipal()).getUsername());
            return partialCurrentUser;
        }
        return null;
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                getAuthorities(authentication).noneMatch("ROLE_ANONYMOUS"::equals);
    }

    private static Stream<String> getAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority);
    }


    public static void doWithAuth(ApplicationContext applicationContext, final String username, @SuppressWarnings("rawtypes") final Runnable executable) {
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication();
        reauthenticate(applicationContext, username, null);

        try {
            executable.run();
        }
        finally {
            if (previousAuth == null) {
                SecurityContextHolder.clearContext();
            }
            else {
                SecurityContextHolder.getContext().setAuthentication(previousAuth);
            }
        }
    }

    public static void reauthenticate(ApplicationContext applicationContext, final String username, final String password) {
        UserDetailsService userDetailsService = getBean(applicationContext,"userDetailsService");
        //UserCache userCache = getBean(applicationContext,"userCache");

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userDetails, password == null ? userDetails.getPassword() : password, userDetails.getAuthorities()));
        //userCache.removeUserFromCache(username);
    }

    private static <T> T getBean(ApplicationContext applicationContext, final String name) {
        return (T)applicationContext.getBean(name);
    }
}
