package be.cytomine.service;

import be.cytomine.exceptions.ServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

@Slf4j
public abstract class CytomineWebSocketHandler extends TextWebSocketHandler {

    public Map<Object, ConcurrentWebSocketSessionDecorator[]> afterConnectionEstablished(WebSocketSession session, Map<Object, ConcurrentWebSocketSessionDecorator[]> sessions) {
        ConcurrentWebSocketSessionDecorator sessionDecorator = new ConcurrentWebSocketSessionDecorator(session, 1000, 8192);
        String userID = session.getAttributes().get("userID").toString();
        if(sessions.keySet().contains(userID)){
            ConcurrentWebSocketSessionDecorator[] oldSessions = sessions.get(userID);
            ConcurrentWebSocketSessionDecorator[] newSessions = addSession(oldSessions, sessionDecorator);
            sessions.replace(userID, oldSessions, newSessions);
        }else{
            ConcurrentWebSocketSessionDecorator[] newSessions = {sessionDecorator};
            sessions.put(userID, newSessions);
        }
        return sessions;
    }

    protected ConcurrentWebSocketSessionDecorator[] addSession(ConcurrentWebSocketSessionDecorator[] oldSessions, ConcurrentWebSocketSessionDecorator newSession){
        ConcurrentWebSocketSessionDecorator[] newSessions = new ConcurrentWebSocketSessionDecorator[oldSessions.length + 1];
        for(int i=0; i < oldSessions.length; i++){
            newSessions[i] = oldSessions[i];
        }
        newSessions[newSessions.length - 1] = newSession;
        return newSessions;
    }

    protected void sendWebSocketMessage(ConcurrentWebSocketSessionDecorator s, TextMessage message) throws ServerException {
        if(s.isOpen()){
            try {
                s.sendMessage(message);
                log.info("Has send WebSocket message to session : " + s.getId());
            } catch (IOException e) {
                throw new ServerException("Failed to send message to session : " + s.getId());
            }
        }
    }
}
