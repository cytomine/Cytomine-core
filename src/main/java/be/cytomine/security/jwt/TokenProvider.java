package be.cytomine.security.jwt;

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

import be.cytomine.config.properties.ApplicationProperties;
import be.cytomine.exceptions.AuthenticationException;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.utils.StringUtils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TokenProvider {

    private static final String ORIGINAL_USER = "original";
    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private static final String AUTHORITIES_KEY = "auth";

    private static final String TYPE_KEY = "type";

    private static final String INVALID_JWT_TOKEN = "Invalid JWT token.";

    private Key key;

    private final JwtParser jwtParser;

    private final long tokenValidityInMilliseconds;

    private final long tokenValidityInMillisecondsForRememberMe;

    private final long tokenValidityInMillisecondsForShortTerm;

    private final SecUserRepository secUserRepository;

    public TokenProvider(ApplicationProperties applicationProperties, SecUserRepository secUserRepository) {
        byte[] keyBytes;
        String secret = applicationProperties.getAuthentication().getJwt().getSecret();
        keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
        this.secUserRepository = secUserRepository;
        this.tokenValidityInMilliseconds =
                1000 * applicationProperties.getAuthentication().getJwt().getTokenValidityInSeconds();
        this.tokenValidityInMillisecondsForRememberMe =
                1000 * applicationProperties.getAuthentication().getJwt().getTokenValidityInSecondsForRememberMe();
        this.tokenValidityInMillisecondsForShortTerm =
                1000 * applicationProperties.getAuthentication().getJwt().getTokenValidityInSecondsForShortTerm();
    }

    public String createToken(Authentication authentication, TokenType tokenType) {
        String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity = null;
        if (tokenType == TokenType.REMEMBER_ME) {
            validity = new Date(now + this.tokenValidityInMillisecondsForRememberMe);
        } else if (tokenType == TokenType.SESSION || tokenType == TokenType.IMPERSONATE) {
            validity = new Date(now + this.tokenValidityInMilliseconds);
        } else if (tokenType == TokenType.SHORT_TERM) {
            validity = new Date(now + this.tokenValidityInMillisecondsForShortTerm);
        } else {
            throw new IllegalArgumentException("Token type " + tokenType + " not valid");
        }
        JwtBuilder builder = Jwts
                .builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .claim(TYPE_KEY, tokenType.name())
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity);
        if (tokenType==TokenType.IMPERSONATE) {
            authentication.getAuthorities().stream().filter(x -> x instanceof SwitchUserGrantedAuthority).findFirst().ifPresent(x -> {
                builder.claim(ORIGINAL_USER, ((User)((SwitchUserGrantedAuthority)x).getSource().getPrincipal()).getUsername());
            });
        }
        return builder.compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        return getAuthentication(token, claims);
    }

    public Authentication getAuthentication(String token, Claims claims) {
        Collection<? extends GrantedAuthority> authorities;

        if (claims.get(TYPE_KEY)!=null && !claims.get(TYPE_KEY).equals(TokenType.IMPERSONATE.name())) {
            authorities = Arrays
                    .stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                    .filter(auth -> !auth.trim().isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            authorities = Arrays
                    .stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                    .filter(auth -> !auth.trim().isEmpty())
                    .map(x -> new SwitchUserGrantedAuthority(x, new UsernamePasswordAuthenticationToken(claims.get(ORIGINAL_USER), "unknown", new ArrayList<>())))
                    .collect(Collectors.toList());
        }

        User principal = new User(claims.getSubject(), "", authorities);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(principal, token, authorities);
        usernamePasswordAuthenticationToken.setDetails(secUserRepository.findByUsernameLikeIgnoreCase(claims.getSubject()).orElseThrow(() -> new AuthenticationException("User " + claims.getSubject() + " not found")));
        return usernamePasswordAuthenticationToken;
    }

    public boolean validateToken(String authToken) {
        return decodeToken(authToken)!=null;
    }

    public Jws<Claims> decodeToken(String authToken) {
        try {
            return jwtParser.parseClaimsJws(authToken);
        } catch (ExpiredJwtException e) {
            log.trace(INVALID_JWT_TOKEN, e);
        } catch (UnsupportedJwtException e) {
            log.trace(INVALID_JWT_TOKEN, e);
        } catch (MalformedJwtException e) {
            log.trace(INVALID_JWT_TOKEN, e);
        } catch (SignatureException e) {
            log.trace(INVALID_JWT_TOKEN, e);
        } catch (IllegalArgumentException e) {
            log.error("Token validation error {}", e.getMessage());
        }
        log.debug("token refused: "+ StringUtils.obscurify(authToken, 3));
        return null;
    }

}
