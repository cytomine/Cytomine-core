package be.cytomine.config.properties;

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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Getter
@Setter
public class ApplicationProperties {

    private String serverId;

    private String version;

    private String serverURL;

    private NotificationProperties notification;

    private String storagePath;

    private CustomUIProperties customUI;

    private AuthenticationProperties authentication = new AuthenticationProperties();

    private AppEngineProperties appEngine;

    private String instanceHostWebsite;

    private String instanceHostSupportMail;

    private String instanceHostPhoneNumber;

    @NotNull
    @NotBlank
    private String adminPassword;

    @NotNull
    @NotBlank
    private String adminEmail;

    @NotNull
    @NotBlank
    private String adminPrivateKey;

    @NotNull
    @NotBlank
    private String adminPublicKey;

    @NotNull
    @NotBlank
    private String superAdminPrivateKey;

    @NotNull
    @NotBlank
    private String superAdminPublicKey;

    private String ImageServerPrivateKey;

    private String ImageServerPublicKey;

    private String rabbitMQPrivateKey;

    private String rabbitMQPublicKey;

    private String defaultLanguage;

    private String internalProxyURL;

    private Annotation annotation;

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }
}

@ToString
class Annotation {
    int maxNumberOfPoint;

    public int getMaxNumberOfPoint() {
        return maxNumberOfPoint;
    }

    public void setMaxNumberOfPoint(int maxNumberOfPoint) {
        this.maxNumberOfPoint = maxNumberOfPoint;
    }
}
