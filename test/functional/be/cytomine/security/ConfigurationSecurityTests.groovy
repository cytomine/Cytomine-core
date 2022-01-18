package be.cytomine.security

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

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.http.ConfigurationAPI
import be.cytomine.meta.Configuration
import grails.converters.JSON

class ConfigurationSecurityTests extends SecurityTestsAbstract{


    void testConfigurationSecurityForCytomineAdmin() {

        //Get user1
        User user1 = getUser1()

        //Get admin user
        User admin = getUserAdmin()

        Configuration configuration = BasicInstanceBuilder.getConfigurationNotExist(false)
        //check if admin user can access/update/delete
        def result = ConfigurationAPI.create(configuration.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        def json = JSON.parse(result.data).configuration
        assert (200 == ConfigurationAPI.show(json.key,SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN).code)
        assert (200 == ConfigurationAPI.update(json.key,configuration.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN).code)
        assert (200 == ConfigurationAPI.delete(json.key,SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN).code)
    }

    void testConfigurationSecurityForCytomineGuest() {

        //Get user1
        User guest = getGuest1()

        //Get admin user
        User admin = getUserAdmin()

        Configuration configuration = BasicInstanceBuilder.getConfigurationNotExist(false)

        def result = ConfigurationAPI.create(configuration.encodeAsJSON(),SecurityTestsAbstract.GUEST1,SecurityTestsAbstract.GPASSWORD1)
        assert 403 == result.code
        result = ConfigurationAPI.create(configuration.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        def json = JSON.parse(result.data).configuration
        assert (200 == ConfigurationAPI.show(json.key,SecurityTestsAbstract.GUEST1,SecurityTestsAbstract.GPASSWORD1).code)
        assert (403 == ConfigurationAPI.update(json.key,configuration.encodeAsJSON(),SecurityTestsAbstract.GUEST1,SecurityTestsAbstract.GPASSWORD1).code)
        assert (403 == ConfigurationAPI.delete(json.key,SecurityTestsAbstract.GUEST1,SecurityTestsAbstract.GPASSWORD1).code)

        configuration.readingRole = Configuration.Role.USER
        result = ConfigurationAPI.update(json.key, configuration.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        json = JSON.parse(result.data).configuration

        assert (403 == ConfigurationAPI.show(json.key,SecurityTestsAbstract.GUEST1,SecurityTestsAbstract.GPASSWORD1).code)
        assert (403 == ConfigurationAPI.update(json.key,configuration.encodeAsJSON(),SecurityTestsAbstract.GUEST1,SecurityTestsAbstract.GPASSWORD1).code)
        assert (403 == ConfigurationAPI.delete(json.key,SecurityTestsAbstract.GUEST1,SecurityTestsAbstract.GPASSWORD1).code)

        configuration.readingRole = Configuration.Role.ALL
        result = ConfigurationAPI.update(json.key, configuration.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        json = JSON.parse(result.data).configuration

        assert (200 == ConfigurationAPI.show(json.key,SecurityTestsAbstract.GUEST1,SecurityTestsAbstract.GPASSWORD1).code)
        assert (403 == ConfigurationAPI.update(json.key,configuration.encodeAsJSON(),SecurityTestsAbstract.GUEST1,SecurityTestsAbstract.GPASSWORD1).code)
        assert (403 == ConfigurationAPI.delete(json.key,SecurityTestsAbstract.GUEST1,SecurityTestsAbstract.GPASSWORD1).code)
    }

    void testConfigurationSecurityForCytomineUser() {

        getUser2()
        //Get admin user
        User admin = getUserAdmin()

        Configuration configuration = BasicInstanceBuilder.getConfigurationNotExist(false)

        def result = ConfigurationAPI.create(configuration.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code
        result = ConfigurationAPI.create(configuration.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        def json = JSON.parse(result.data).configuration
        assert (200 == ConfigurationAPI.show(json.key,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        assert (403 == ConfigurationAPI.update(json.key,configuration.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        assert (403 == ConfigurationAPI.delete(json.key,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)

        configuration.readingRole = Configuration.Role.USER
        result = ConfigurationAPI.update(json.key, configuration.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        json = JSON.parse(result.data).configuration

        assert (200 == ConfigurationAPI.show(json.key,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        assert (403 == ConfigurationAPI.update(json.key,configuration.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        assert (403 == ConfigurationAPI.delete(json.key,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
    }

}
