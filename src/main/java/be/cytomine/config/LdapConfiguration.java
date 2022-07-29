package be.cytomine.config;

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

import be.cytomine.security.ldap.LdapClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class LdapConfiguration {

    @Autowired
    Environment env;

    @Value("${ldap.server}")
    private String server;

    @Value("${ldap.principal}")
    private String principal;

    @Value("${ldap.password}")
    private String password;


    @Bean
    public LdapClient ldapClient() {
        LdapClient ldapClient = new LdapClient(
                server,
                principal,
                password
        );
        return ldapClient;
    }
}