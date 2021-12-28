package be.cytomine.config;

import lombok.Data;

@Data
public class NotificationConfiguration {

    private String email;

    private String password;

    private String smtpHost;

    private String smtpPort;
}
