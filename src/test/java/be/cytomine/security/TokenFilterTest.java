package be.cytomine.security;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.domain.security.SecUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


class TokenFilterTest {

//    private MockHttpServletResponse response;
//    private MockFilterChain filterChain;
//    private DomainUserDetailsService domainUserDetailsService;
//
//    @BeforeEach
//    void before() {
//        response = new MockHttpServletResponse();
//        filterChain = new MockFilterChain();
//        domainUserDetailsService = Mockito.mock(DomainUserDetailsService.class);
//        SecUser user = BasicInstanceBuilder.given_a_user_not_persisted();
//        String key = "b980f286047ddf3ee6a6c11e3bb2ac4d96e7f3b8";
//        Mockito.when(domainUserDetailsService.getAuthentication(Mockito.eq(key))).thenReturn(
//                new PreAuthenticatedAuthenticationToken(user, key, user.getAuthorities())
//        );
//    }
//
//    @Test
//    void it_should_not_authenticate_basic() throws ServletException, IOException {
//
//        MockHttpServletRequest request = givenRequestWithValidBasicToken();
//        new ApiKeyFilter(domainUserDetailsService).doFilterInternal(request, response, filterChain);
//
//        SecurityContext context = SecurityContextHolder.getContext();
//        Authentication authentication = context.getAuthentication();
//        assertThat(authentication).isNotNull();
//    }
//
//    @Test
//    void it_should_authenticate() throws ServletException, IOException {
//
//        MockHttpServletRequest request = givenRequestWithValidApiKeyToken();
//        new ApiKeyFilter(domainUserDetailsService).doFilterInternal(request, response, filterChain);
//
//        SecurityContext context = SecurityContextHolder.getContext();
//        Authentication authentication = context.getAuthentication();
//        assertThat(authentication).isNotNull();
//        PreAuthenticatedAuthenticationToken authenticationToken = (PreAuthenticatedAuthenticationToken) authentication;
//        assertThat(authenticationToken.getCredentials()).isEqualTo("b980f286047ddf3ee6a6c11e3bb2ac4d96e7f3b8");
//    }
//
//    @Test
//    void it_should_not_authenticate_empty_key() throws ServletException, IOException {
//
//        MockHttpServletRequest request = givenRequestWithEmptyApiKeyToken();
//        new ApiKeyFilter(domainUserDetailsService).doFilterInternal(request, response, filterChain);
//
//        SecurityContext context = SecurityContextHolder.getContext();
//        Authentication authentication = context.getAuthentication();
//        assertThat(authentication).isNull();
//    }
//
//    private MockHttpServletRequest givenRequestWithValidApiKeyToken() {
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        request.addHeader("Authorization", "Api-Key b980f286047ddf3ee6a6c11e3bb2ac4d96e7f3b8");
//        return request;
//    }
//
//    private MockHttpServletRequest givenRequestWithValidBasicToken() {
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        request.addHeader("Authorization", "Basic cmFkaW9sb2dpc3Q6cmFkaW9sb2dpc3Q=");
//        return request;
//    }
//
//    private MockHttpServletRequest givenRequestWithEmptyApiKeyToken() {
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        request.addHeader("Authorization", "Api-Key ");
//        return request;
//    }
}