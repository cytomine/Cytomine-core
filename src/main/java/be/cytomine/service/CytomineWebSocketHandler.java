package be.cytomine.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class CytomineWebSocketHandler extends TextWebSocketHandler {

    ConcurrentWebSocketSessionDecorator session;
    public static Map<Object, ConcurrentWebSocketSessionDecorator[]> sessions = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = new ConcurrentWebSocketSessionDecorator(session, 1000, 8192);
        String userID = this.session.getAttributes().get("userID").toString();

        if(sessions.keySet().contains(userID)){
            ConcurrentWebSocketSessionDecorator[] oldSessions = sessions.get(userID);
            ConcurrentWebSocketSessionDecorator[] newSessions = addSession(oldSessions, this.session);
            sessions.replace(userID, oldSessions, newSessions);
        }else{
            ConcurrentWebSocketSessionDecorator[] newSessions = {this.session};
            sessions.put(userID, newSessions);
        }
        log.info("Established user position WebSocket connection {}", session.getId());
    }

    protected ConcurrentWebSocketSessionDecorator[] addSession(ConcurrentWebSocketSessionDecorator[] oldSessions, ConcurrentWebSocketSessionDecorator newSession){
        ConcurrentWebSocketSessionDecorator[] newSessions = new ConcurrentWebSocketSessionDecorator[oldSessions.length + 1];
        for(int i=0; i < oldSessions.length; i++){
            newSessions[i] = oldSessions[i];
        }
        newSessions[newSessions.length - 1] = newSession;
        return newSessions;
    }
}
