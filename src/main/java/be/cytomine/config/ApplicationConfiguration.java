package be.cytomine.config;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Getter
@Setter
public class ApplicationConfiguration {

    private String version;

    private String serverURL;

    private CytomineConfiguration cytomine;

    private NotificationConfiguration notification;

    private String storagePath;

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

    private String softwareSources;

    private Boolean useHTTPInternally;

}