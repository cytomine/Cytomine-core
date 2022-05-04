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

import java.util.*;
import java.util.stream.Collectors;

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
        this.sessions = super.afterConnectionEstablished(session, this.sessions);
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
        String payload = message.getPayload().toString();

        if(payload.equals("stop-track")){
            removeFromTrackerSessions(session);
        }
        else if(payload.equals("stop-broadcast")){
            removeFromBroadcastSession(session);
        }
        else if(sessionsTracked.keySet().contains(payload)){
            ConcurrentWebSocketSessionDecorator[] oldSessions = sessionsTracked.get(payload);
            ConcurrentWebSocketSessionDecorator[] newSessions = oldSessions;;
            for(ConcurrentWebSocketSessionDecorator newSession : trackSessions){
                newSessions = addSession(newSessions, newSession);
            }
            sessionsTracked.replace(payload, oldSessions, newSessions);
        }
        else{
            sessionsTracked.put(payload, trackSessions);
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

    private void removeFromTrackerSessions(WebSocketSession session){
        String userID = getSessionUserId(session, sessionsTracked);
        if(!userID.isEmpty()){
            ConcurrentWebSocketSessionDecorator[] oldSessions = sessionsTracked.get(userID);
            ConcurrentWebSocketSessionDecorator[] newSessions = removeSession(oldSessions, session);
            sessionsTracked.replace(userID, oldSessions, newSessions);
        }
    }

    private void removeFromBroadcastSession(WebSocketSession session){
        String userID = getSessionUserId(session, sessions);
        if(!userID.isEmpty()){
            sendNotification(List.of(sessionsTracked.get(userID)));
            sessionsTracked.remove(userID);
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

    private String getSessionUserId(WebSocketSession session, Map<Object, ConcurrentWebSocketSessionDecorator[]> sessionsList){
        String userId = "";
        for(Map.Entry<Object, ConcurrentWebSocketSessionDecorator[]> entry : sessionsList.entrySet()){
            List<String> entryIds = Arrays.stream(entry.getValue()).map(value -> value.getId()).collect(Collectors.toList());
            if(entryIds.contains(session.getId())){
                userId = entry.getKey().toString();
                break;
            }
        }
        return userId;
    }

    // TODO : Send notification via future notification system (lvl importance of 2 to bypass queue)
    private void sendNotification(List<ConcurrentWebSocketSessionDecorator> decoratorsSession){
        /*for (Map.Entry<Object, ConcurrentWebSocketSessionDecorator[]> entry : sessions.entrySet()) {
            for(ConcurrentWebSocketSessionDecorator s : entry.getValue()){
                if(decoratorsSession.contains(s)){
                }
            }
        }*/
    }
}

