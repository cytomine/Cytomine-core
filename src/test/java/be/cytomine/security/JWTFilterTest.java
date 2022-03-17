package be.cytomine.security;

import be.cytomine.config.ApplicationConfiguration;
import be.cytomine.config.security.JWTFilter;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.security.jwt.TokenType;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JWTFilterTest {

    private TokenProvider tokenProvider;

    private JWTFilter jwtFilter;

    @BeforeEach
    public void setup() {
        ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        String base64Secret = "fd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8";
        applicationConfiguration.getAuthentication().getJwt().setSecret(base64Secret);
        applicationConfiguration.getAuthentication().getJwt().setTokenValidityInSeconds(60000L);
        applicationConfiguration.getAuthentication().getJwt().setTokenValidityInSecondsForRememberMe(60000L);
        applicationConfiguration.getAuthentication().getJwt().setTokenValidityInSecondsForShortTerm(300L);

        tokenProvider = new TokenProvider(applicationConfiguration);
//        ReflectionTestUtils.setField(tokenProvider, "key", Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret)));
//        ReflectionTestUtils.setField(tokenProvider, "tokenValidityInMilliseconds", 6000000);
//        ReflectionTestUtils.setField(tokenProvider, "tokenValidityInMillisecondsForRememberMe", 6000000);
//        ReflectionTestUtils.setField(tokenProvider, "tokenValidityInMillisecondsForShortTerm", 300000);
        jwtFilter = new JWTFilter(tokenProvider);
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    void testJWTFilter() throws Exception {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "test-user",
            "test-password",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String jwt = tokenProvider.createToken(authentication, TokenType.SESSION);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("test-user");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials()).hasToString(jwt);
    }

    @Test
    void testJWTFilterShortTermToken() throws Exception {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "test-user",
                "test-password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String jwt = tokenProvider.createToken(authentication, TokenType.SHORT_TERM);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.addParameter(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("test-user");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials()).hasToString(jwt);
    }

    @Test
    void testJWTFilterShortTermTokenWithRequestThatCanModifySomething() throws Exception {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "test-user",
                "test-password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String jwt = tokenProvider.createToken(authentication, TokenType.SHORT_TERM);
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addParameter(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        request.setMethod("POST");
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        Assertions.assertThrows(ForbiddenException.class, () -> {
            jwtFilter.doFilter(request, response, filterChain);
        });
    }

    @Test
    void testJWTFilterInvalidToken() throws Exception {
        String jwt = "wrong_jwt";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void testJWTFilterMissingAuthorization() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void testJWTFilterMissingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Bearer ");
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void testJWTFilterWrongScheme() throws Exception {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "test-user",
            "test-password",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String jwt = tokenProvider.createToken(authentication, TokenType.SESSION);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Basic " + jwt);
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void testSpeed() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "test-user",
                "test-password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        Long start = System.currentTimeMillis();
        int exec = 1000;
        String token = null;
        for (int i = 0; i < exec; i++) {
            token = tokenProvider.createToken(authentication, TokenType.SESSION);
        }
        System.out.println((System.currentTimeMillis()-start));
        for (int i = 0; i < exec; i++) {
            tokenProvider.validateToken(token);
        }
        System.out.println((System.currentTimeMillis()-start));
    }
}
