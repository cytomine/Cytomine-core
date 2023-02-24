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


import be.cytomine.security.SimpleUserDetailsService
import be.cytomine.spring.CustomAjaxAwareAuthenticationEntryPoint
import be.cytomine.spring.CustomDefaultRedirectStrategy
import be.cytomine.spring.CustomSavedRequestAwareAuthenticationSuccessHandler
import be.cytomine.web.CytomineMultipartHttpServletRequest
import be.cytomine.PatchedInterceptUrlMapFilterInvocationDefinition
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.web.authentication.AjaxAwareAuthenticationSuccessHandler
import grails.util.Holders
import org.springframework.cache.ehcache.EhCacheFactoryBean
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler

//import grails.plugin.springsecurity.SpringSecurityUtils
// Place your Spring DSL code here
beans = {
    logoutEventListener(LogoutEventListener)

    'apiAuthentificationFilter'(cytomine.web.APIAuthentificationFilters) {
        // properties
    }
    'multipartResolver'(CytomineMultipartHttpServletRequest) {
        // Max in memory 100kbytes
        maxInMemorySize=10240

        //100Gb Max upload size
        maxUploadSize=102400000000


    }
    springConfig.addAlias "springSecurityService", "springSecurityCoreSpringSecurityService"
    
    def config = SpringSecurityUtils.securityConfig


    redirectStrategy(CustomDefaultRedirectStrategy) {
        contextRelative = true
    }
    successRedirectHandler(CustomSavedRequestAwareAuthenticationSuccessHandler) {
        alwaysUseDefaultTargetUrl = false
        useReferer = false
        //defaultTargetUrl = '/'
    }

    authenticationEntryPoint(CustomAjaxAwareAuthenticationEntryPoint, '/') {
        grailsApplication = ref('grailsApplication')
        ajaxLoginFormUrl = '/login/authAjax'
        forceHttps = false
        useForward = false
        portMapper = ref('portMapper')
        portResolver = ref('portResolver')
    }

    authenticationSuccessHandler(AjaxAwareAuthenticationSuccessHandler) {
        requestCache = ref('requestCache')
        defaultTargetUrl = Holders.getGrailsApplication().config.grails.UIURL?: Holders.getGrailsApplication().config.grails.serverURL ?: '/'
        alwaysUseDefaultTargetUrl = false
        targetUrlParameter = 'spring-security-redirect'
        ajaxSuccessUrl = SpringSecurityUtils.securityConfig.successHandler.ajaxSuccessUrl
        useReferer = false
        redirectStrategy = ref('redirectStrategy')
    }

    logoutSuccessHandler(SimpleUrlLogoutSuccessHandler) {
        defaultTargetUrl = Holders.getGrailsApplication().config.grails.UIURL?: Holders.getGrailsApplication().config.grails.serverURL ?: '/'
    }


    userDetailsService(SimpleUserDetailsService)

    ehcacheAclCache(EhCacheFactoryBean) {
        cacheManager = ref('aclCacheManager')
        cacheName = 'aclCache'
        overflowToDisk = false
    }

    currentRoleServiceProxy(org.springframework.aop.scope.ScopedProxyFactoryBean) {
        targetBeanName = 'currentRoleService'
        proxyTargetClass = true
    }

    def conf = SpringSecurityUtils.securityConfig
    objectDefinitionSource(be.cytomine.PatchedInterceptUrlMapFilterInvocationDefinition) {
        if (conf.rejectIfNoRule instanceof Boolean) {
            rejectIfNoRule = conf.rejectIfNoRule
        }
    }
}
