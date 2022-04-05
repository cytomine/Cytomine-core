package be.cytomine.api;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@Component
public class WebSocketHandler extends TextWebSocketHandler {

    ConcurrentWebSocketSessionDecorator session;

    private static Map<Object, ConcurrentWebSocketSessionDecorator[]> sessions = new HashMap<>();
    private final Logger log = (Logger) LoggerFactory.getLogger(this.getClass());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = new ConcurrentWebSocketSessionDecorator(session, 1000, 8192);
        String userID = this.session.getAttributes().get("userID").toString();

        log.info("Establishing WebSocket connection for user {}", userID);

        if(sessions.keySet().contains(userID)){
            ConcurrentWebSocketSessionDecorator[] oldSessions = sessions.get(userID);
            ConcurrentWebSocketSessionDecorator[] newSessions = addSession(oldSessions, this.session);
            sessions.replace(userID, oldSessions, newSessions);
        }else{
            ConcurrentWebSocketSessionDecorator[] newSessions = {this.session};
            sessions.put(userID, newSessions);
        }
        log.info("Established WebSocket connection {}", this.session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        remove(session);
        log.info("Closing WebSocket connection from {}", session.getRemoteAddress());
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

    private ConcurrentWebSocketSessionDecorator[] addSession(ConcurrentWebSocketSessionDecorator[] oldSessions, ConcurrentWebSocketSessionDecorator newSession){
        ConcurrentWebSocketSessionDecorator[] newSessions = new ConcurrentWebSocketSessionDecorator[oldSessions.length + 1];
        for(int i=0; i < oldSessions.length; i++){
            newSessions[i] = oldSessions[i];
        }
        newSessions[newSessions.length - 1] = newSession;
        return newSessions;
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

