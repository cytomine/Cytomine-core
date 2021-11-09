package be.cytomine.security

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
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
import be.cytomine.test.HttpClient
import be.cytomine.test.Infos
import be.cytomine.test.http.UserAPI
import be.cytomine.test.http.UserRoleAPI
import grails.converters.JSON
import org.apache.commons.codec.binary.Base64
import org.apache.http.Header

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat

class LoginWithAPIKeyTests extends SecurityTestsAbstract {

    void testLoginWithValidAPIKey() {
        User user1 = BasicInstanceBuilder.getUser1()
        user1.apiEnabled = true
        BasicInstanceBuilder.saveDomain(user1)
        String URL = Infos.CYTOMINEURL + "/api/user/current.json"
        HttpClient client = new HttpClient();
        client.connect(URL)

        def headers  = generateSignature(user1, "GET", URL, "", "application/json,*/*")
        headers.put("accept", "application/json,*/*")
        client.get(headers)
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect()
        assert 200 == code
        assert JSON.parse(response)["username"]=="user1"
    }

    void testLoginWithValidAPIKeyForDisabledUser() {
        User user1 = BasicInstanceBuilder.getUser1()
        user1.apiEnabled = false
        BasicInstanceBuilder.saveDomain(user1)
        String URL = Infos.CYTOMINEURL + "/api/user/current.json"
        HttpClient client = new HttpClient();
        client.connect(URL)

        def headers  = generateSignature(user1, "GET", URL, "", "application/json,*/*")
        headers.put("accept", "application/json,*/*")
        client.get(headers)
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect()
        assert 401 == code
    }

    void testLoginWithValidAPIKeyForDisabledUserButSuperadmin() {
        User user1 = BasicInstanceBuilder.getSuperAdmin("testapikey", "testapikey")
        user1.apiEnabled = false // expect to be ignored as superadmin
        BasicInstanceBuilder.saveDomain(user1)
        String URL = Infos.CYTOMINEURL + "/api/user/current.json"
        HttpClient client = new HttpClient();
        client.connect(URL)

        def headers  = generateSignature(user1, "GET", URL, "", "application/json,*/*")
        headers.put("accept", "application/json,*/*")
        client.get(headers)
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect()
        assert 200 == code
        assert JSON.parse(response)["username"]=="testapikey"
    }

    void testDefaultAPIKeyValidityForNewUser() {
        User user1 = BasicInstanceBuilder.getUserNotExist()
        def result = UserAPI.create(user1.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        user1 = result.data
        String URL = Infos.CYTOMINEURL + "/api/user/current.json"
        HttpClient client = new HttpClient();
        client.connect(URL)

        def headers  = generateSignature(user1, "GET", URL, "", "application/json,*/*")
        headers.put("accept", "application/json,*/*")
        client.get(headers)
        int code = client.getResponseCode()
        client.disconnect()
        assert 401 == code
    }

    void testLoginWithBadAPIKey() {
        User user1 = BasicInstanceBuilder.getUser1()
        user1.apiEnabled = true
        BasicInstanceBuilder.saveDomain(user1)
        String URL = Infos.CYTOMINEURL + "/api/user/current.json"
        HttpClient client = new HttpClient();
        client.connect(URL)
        def headers  = generateSignature(user1, "GET", URL, "", "application/json,*/*")
        headers.put("authorization", "CYTOMINE " + user1.getPublicKey() + ":blablablablablabla"); // alter authorization signature
        headers.put("accept", "application/json,*/*")
        client.get(headers)
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect()
        assert 401 == code
    }


    public TreeMap<String, String>  generateSignature(User user, String action, String url, String contentType, String accept) throws IOException {
        String host = Infos.CYTOMINEURL
        url = url.replace(host, "");
        url = url.replace("http://" + host, "");
        url = url.replace("https://" + host, "");

        TreeMap<String, String> headers = new TreeMap<String, String>();
        headers.put("accept", accept);
        headers.put("date", getActualDateStr());

        log.debug("AUTHORIZE: " + action + "\\n\\n" + contentType + "\\n" + headers.get("date") + "\n");

        String canonicalHeaders = action + "\n\n" + contentType + "\n" + headers.get("date") + "\n";

        String messageToSign = canonicalHeaders + url;

        log.debug("publicKey=" + user.publicKey);
        log.debug("privateKey=" + user.privateKey);
        log.debug("messageToSign=" + messageToSign);


        SecretKeySpec privateKeySign = new SecretKeySpec(user.privateKey.getBytes(), "HmacSHA1");

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(privateKeySign);
            byte[] rawHmac = mac.doFinal(new String(messageToSign.getBytes(), "UTF-8").getBytes());
            String  signature = rawHmac.encodeBase64().toString()
            headers.put("authorization", "CYTOMINE " + user.getPublicKey() + ":" + signature);
            return headers
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }

    }

    public static String getActualDateStr()  {
        Date today = Calendar.getInstance().getTime();
        return new SimpleDateFormat("%E, %d %M %Y %H:%M:%S +0000").format(today);
    }

}
