package be.cytomine.service.social;

import be.cytomine.exceptions.ServerException;
import be.cytomine.service.CytomineWebSocketHandler;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.security.SecUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WebSocketUserPositionHandler extends CytomineWebSocketHandler {

    public static Map<Object, ConcurrentWebSocketSessionDecorator[]> sessionsTracked = new HashMap<>();
    public static Map<Object, ConcurrentWebSocketSessionDecorator[]> sessions = new HashMap<>();

    @Autowired
    UserPositionService userPositionService;

    @Autowired
    ImageInstanceService imageInstanceService;

    @Autowired
    SliceInstanceService sliceInstanceService;

    @Autowired
    SecUserService secUserService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions = super.afterConnectionEstablished(session, sessions);
        log.info("Established user position WebSocket connection {}", session.getId());
    }
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

    public void userHasMoved(String userID, String position) throws ServerException {
        try{
            ConcurrentWebSocketSessionDecorator[] trackSessions = sessionsTracked.get(userID);
            TextMessage message = new TextMessage(position);
            for(ConcurrentWebSocketSessionDecorator s : trackSessions){
                super.sendWebSocketMessage(s, message);
            }
        }catch (NullPointerException e){
            log.info("User id : "+ userID +" is not followed");
        }

    }
}

