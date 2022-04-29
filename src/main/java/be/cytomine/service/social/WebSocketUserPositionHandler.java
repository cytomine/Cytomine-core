package be.cytomine.service.social;

import be.cytomine.service.CytomineWebSocketHandler;
import ch.qos.logback.classic.Logger;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WebSocketUserPositionHandler extends CytomineWebSocketHandler {

    public static Map<Object, ConcurrentWebSocketSessionDecorator[]> sessionsTracked = new HashMap<>();

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        log.info("Closing user position WebSocket connection from {}", session.getRemoteAddress());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        ConcurrentWebSocketSessionDecorator[] trackSessions = sessions.get(session.getAttributes().get("userID").toString());
        String trackedUserId = message.getPayload().toString();

        if(sessionsTracked.keySet().contains(trackedUserId)){
            ConcurrentWebSocketSessionDecorator[] oldSessions = sessionsTracked.get(trackedUserId);
            ConcurrentWebSocketSessionDecorator[] newSessions = oldSessions;;
            for(ConcurrentWebSocketSessionDecorator newSession : trackSessions){
                newSessions = addSession(newSessions, newSession);
            }
            sessionsTracked.replace(trackedUserId, oldSessions, newSessions);
        }else{
            sessionsTracked.put(trackedUserId, trackSessions);
        }
    }
}

