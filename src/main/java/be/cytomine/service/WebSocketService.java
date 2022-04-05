package be.cytomine.service;

import be.cytomine.api.WebSocketHandler;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Service
public class WebSocketService {

    private final String SINGLE_NOTIFICATION_MESSAGE = "A notification has been created for user : ";
    private final String BROADCAST_NOTIFICATION_MESSAGE = "A notification has been created for both";

    private final Logger log = (Logger) LoggerFactory.getLogger(this.getClass());

    @Autowired
    private WebSocketHandler webSocketHandler;

    public void sendNotification(String userID) throws IOException {
        webSocketHandler.sendNotification(userID, SINGLE_NOTIFICATION_MESSAGE + userID);
    }

    public void sendGlobalNotification() throws IOException {
        webSocketHandler.sendGlobalNotification(BROADCAST_NOTIFICATION_MESSAGE);
    }

    public void spamUser(String userID, int n) {
        for(int t=0; t<5 ; t++) {
            String message = "Sending message with thread " + t;
            new Thread(() -> {
                for(int i=0; i<n; i++){
                    try {
                        webSocketHandler.sendNotification(userID, message);
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.error("Error during spamming of userB");
                    }
                }
            }).start();
        }
    }
    public String getUserMessage(String userID) {
        return "HTTP Pooling message for user " + userID;
    }
}
