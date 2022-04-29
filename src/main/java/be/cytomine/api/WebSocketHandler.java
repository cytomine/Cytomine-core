package be.cytomine.service.notification;

import be.cytomine.domain.notification.Notification;
import be.cytomine.domain.notification.NotificationUser;
import be.cytomine.exceptions.ServerException;
import be.cytomine.repository.notification.NotificationRepository;
import be.cytomine.repository.notification.NotificationUserRepository;
import be.cytomine.service.CytomineWebSocketHandler;
import be.cytomine.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Component
public class WebSocketNotificationHandler extends CytomineWebSocketHandler {

    @Autowired
    private NotificationUserRepository notificationUserRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    public static Integer TIME_DIFF_SECONDS = 600;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        super.afterConnectionEstablished(session);

        String userID = session.getAttributes().get("userID").toString();
        List<String> messages = getNotSendNotificationsMessage(userID);
        for(String message : messages){
            sendNotification(userID, message);
        }
        log.info("Established notification WebSocket connection {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        remove(session);
        log.info("Closing notification WebSocket connection from {}", session.getRemoteAddress());
    }

    public void sendNotification(String userID, String message) throws IOException {
        for (ConcurrentWebSocketSessionDecorator s : sessions.get(userID)) {
            if(s.isOpen()){
                s.sendMessage(new TextMessage(message));
                log.info("Sending a notification : " + message + " to : " + userID);
            }
        }
    }

    public void sendGlobalNotification(String message) throws IOException {
        for(Map.Entry<Object, ConcurrentWebSocketSessionDecorator[]> s : sessions.entrySet()){
            sendNotification(s.getKey().toString(), message);
        }
    }

    private void remove(WebSocketSession session){
        for(Map.Entry<Object, ConcurrentWebSocketSessionDecorator[]> entry : sessions.entrySet()){
            for(ConcurrentWebSocketSessionDecorator s : entry.getValue()){

                if(s.getId().equals(session.getId())){
                    String userID = entry.getKey().toString();
                    ConcurrentWebSocketSessionDecorator[] oldSessions = sessions.get(userID);
                    ConcurrentWebSocketSessionDecorator[] newSessions = removeSession(oldSessions, session);
                    sessions.replace(userID, oldSessions, newSessions);
                }
            }
        }
    }

    private ConcurrentWebSocketSessionDecorator[] removeSession(ConcurrentWebSocketSessionDecorator[] oldSessions, WebSocketSession oldSession){
        ConcurrentWebSocketSessionDecorator[] newSessions = new ConcurrentWebSocketSessionDecorator[oldSessions.length - 1];
        for(int i=0; i < oldSessions.length; i++){
            if(!oldSessions[i].getId().equals(oldSession.getId())){
                newSessions[i] = oldSessions[i];
            }
        }
        return newSessions;
    }
}

