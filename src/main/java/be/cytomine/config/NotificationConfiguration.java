package be.cytomine.config;

import lombok.Data;

import java.util.List;

@Data
public class NotificationConfiguration {

    private String email;

    private String password;

    private String smtpHost;

    private String smtpPort;
}
