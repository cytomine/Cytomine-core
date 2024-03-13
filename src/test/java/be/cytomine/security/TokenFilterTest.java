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

import jakarta.servlet.ServletException;

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
