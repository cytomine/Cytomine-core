package be.cytomine.api.controller;

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

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.config.properties.LtiConsumerProperties;
import be.cytomine.config.properties.LtiProperties;
import be.cytomine.config.security.JWTFilter;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecRole;
import be.cytomine.domain.security.User;
import be.cytomine.dto.LoginVM;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.lti.LtiServiceTests;
import be.cytomine.utils.JsonObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.assertj.core.api.AssertionsForClassTypes;
import org.imsglobal.lti.launch.LtiOauthSigner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
class LoginLtiTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecUserService secUserService;

    @Autowired
    private MockMvc loginControllerMockMvc;

    @Autowired
    private BasicInstanceBuilder builder;

    @Test
    @Transactional
    void authorize_workflow() throws Exception {

        //All parameters are retrieve from https://ltiapps.net/test/tc.php

//        Draft HTTP POST requestclose or Esc Key
//        To: https://ltiapps.net/test/tp.php
//        Content-Type: application/x-www-form-urlencoded
//        Parameters:

        Project project = builder.given_a_project();
        String username = BasicInstanceBuilder.randomString();

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("context_id", "S3294476");
        jsonObject.put("context_label", "ST101");
        jsonObject.put("context_title", "Telecommuncations 101");
        jsonObject.put("context_type", "CourseSection");
        jsonObject.put("custom_context_memberships_url", "https://ltiapps.net/test/tc-memberships.php/context/s22nknojcietue93n55oh23sq3");
        jsonObject.put("custom_context_setting_url", "https://ltiapps.net/test/tc-settings.php/context/s22nknojcietue93n55oh23sq3");
        jsonObject.put("custom_lineitem_url", "https://ltiapps.net/test/tc-outcomes2.php/s22nknojcietue93n55oh23sq3/S3294476/lineitems/dyJ86SiwwA9");
        jsonObject.put("custom_lineitems_url", "https://ltiapps.net/test/tc-outcomes2.php/s22nknojcietue93n55oh23sq3/S3294476/lineitems");
        jsonObject.put("custom_link_memberships_url", "https://ltiapps.net/test/tc-memberships.php/link/s22nknojcietue93n55oh23sq3");
        jsonObject.put("custom_link_setting_url", "https://ltiapps.net/test/tc-settings.php/link/s22nknojcietue93n55oh23sq3");
        jsonObject.put("custom_result_url", "https://ltiapps.net/test/tc-outcomes2.php/s22nknojcietue93n55oh23sq3/S3294476/lineitems/dyJ86SiwwA9/results/29123");
        jsonObject.put("custom_results_url", "https://ltiapps.net/test/tc-outcomes2.php/s22nknojcietue93n55oh23sq3/S3294476/lineitems/dyJ86SiwwA9/results");
        jsonObject.put("custom_system_setting_url", "https://ltiapps.net/test/tc-settings.php/system/s22nknojcietue93n55oh23sq3");
        jsonObject.put("custom_tc_profile_url","https://ltiapps.net/test/tc-profile.php/s22nknojcietue93n55oh23sq3");
        jsonObject.put("ext_ims_lis_basic_outcome_url","https://ltiapps.net/test/tc-ext-outcomes.php");
        jsonObject.put("ext_ims_lis_memberships_id","92ao0h9q7hagcnh0q14kr1mek3:::4jflkkdf9s");
        jsonObject.put("ext_ims_lis_memberships_url","https://ltiapps.net/test/tc-ext-memberships.php");
        jsonObject.put("ext_ims_lis_resultvalue_sourcedids","decimal");
        jsonObject.put("ext_ims_lti_tool_setting_id","s22nknojcietue93n55oh23sq3:::d94gjklf954kj");
        jsonObject.put("ext_ims_lti_tool_setting_url","https://ltiapps.net/test/tc-ext-setting.php");
        jsonObject.put("launch_presentation_css_url","https://ltiapps.net/test/css/tc.css");
        jsonObject.put("launch_presentation_document_target","frame");
        jsonObject.put("launch_presentation_locale","en-GB");
        jsonObject.put("launch_presentation_return_url","https://ltiapps.net/test/tc-return.php");
        jsonObject.put("lis_course_offering_sourcedid","DD-ST101");
//        jsonObject.put("lis_course_section_sourcedid","DD-ST101:C1");
        jsonObject.put("lis_outcome_service_url","https://ltiapps.net/test/tc-outcomes.php");
        jsonObject.put("lis_person_contact_email_primary","jbaird@uni.ac.uk");
        jsonObject.put("lis_person_name_family","Baird");
        jsonObject.put("lis_person_name_full","John Logie Baird");
        jsonObject.put("lis_person_name_given","John");
        jsonObject.put("lis_person_sourcedid", username);
        jsonObject.put("lis_result_sourcedid","s22nknojcietue93n55oh23sq3:::S3294476:::29123:::dyJ86SiwwA9");
        jsonObject.put("lti_message_type","basic-lti-launch-request");
        jsonObject.put("lti_version","LTI-1p0");
        jsonObject.put("oauth_callback","about:blank");

        jsonObject.put("oauth_consumer_key","consumerKey");

        jsonObject.put("oauth_nonce","a1c1c6ed883cb589ddf3a848ccb4afd3");
        jsonObject.put("oauth_signature","cs7ieGAMDMNry3EbWHWsOkI4AsA=");
        jsonObject.put("oauth_signature_method","HMAC-SHA1");

        jsonObject.put("oauth_timestamp", String.valueOf(System.currentTimeMillis()/1000));
        jsonObject.put("oauth_version","1.0");
        jsonObject.put("resource_link_description","Will ET phone home, or not; click to discover more.");
        jsonObject.put("resource_link_id","429785226");
        jsonObject.put("resource_link_title","Phone home");
        jsonObject.put("roles","Instructor");
        jsonObject.put("tool_consumer_info_product_family_code","jisc");
        jsonObject.put("tool_consumer_info_version","1.2");
        jsonObject.put("tool_consumer_instance_contact_email","vle@uni.ac.uk");
        jsonObject.put("tool_consumer_instance_description","A Higher Education establishment in a land far, far away.");
        jsonObject.put("tool_consumer_instance_guid","vle.uni.ac.uk");
        jsonObject.put("tool_consumer_instance_name","University of JISC");
        jsonObject.put("tool_consumer_instance_url","https://vle.uni.ac.uk/");
        jsonObject.put("user_id","29123");
        jsonObject.put("user_image","https://ltiapps.net/test/images/lti.gif");

        jsonObject.put("custom_redirect","#/project/" + project.getId()+ "/images");

        Map<String, String> map = new HashMap<>();
        jsonObject.entrySet().forEach(x -> map.put(x.getKey(), (String)x.getValue()));

        LtiOauthSigner ltiOauthSigner = new LtiOauthSigner();
        Map<String, String> stringStringMap = ltiOauthSigner.signParameters(map, "consumerKey", "secret", "http://localhost:8080/login/loginWithLTI", "POST");
        System.out.println("Signature before: {}" + jsonObject.get("oauth_signature"));
        System.out.println("Signature after: {}" + stringStringMap.get("oauth_signature"));

        List<BasicNameValuePair> nameValuePairs =
                stringStringMap.entrySet().stream().map(x -> new BasicNameValuePair(x.getKey(), (String)x.getValue())).toList();
        String formUrlEncoded = URLEncodedUtils.format(nameValuePairs, Charset.defaultCharset());

        MvcResult mvcResult = loginControllerMockMvc
                .perform(post("/login/loginWithLTI")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .content(formUrlEncoded)).andReturn();
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getStatus()).isEqualTo(302);
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getHeader("Location"))
                .startsWith("http://localhost:8080/#/project/" + project.getId()+ "/images?redirect_token=");
        AssertionsForClassTypes.assertThat(mvcResult.getResponse().getHeader("Authorization"))
                .startsWith("Bearer ");


        User user = userRepository.findByUsernameLikeIgnoreCase(username)
                .orElseThrow(() -> new ObjectNotFoundException("User", username));

        // email is 'username' value @email.com
        assertThat(user.getEmail()).isEqualTo("jbaird@uni.ac.uk");

        // first name is John
        assertThat(user.getFirstname()).isEqualTo("John");

        // last name is Baird
        assertThat(user.getLastname()).isEqualTo("Baird");

        // must have ROLE_USER as he is instructor
        assertThat(user.getRoles().stream().map(SecRole::getAuthority)).contains("ROLE_USER");

        // must be in project as a Admin (instructor)
        assertThat(secUserService.listAdmins(project, false)).contains(user);
    }

}
