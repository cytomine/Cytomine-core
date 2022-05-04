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

    // sessionsTracked key format -> "userId/imageId"
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
        // Message format is : "action/userId/imageId"
        String[] payload = message.getPayload().toString().split("/");
        String payloadAction = payload[0];
        String userAndImageId = payload[1] + "/" + payload[2];

        if(payloadAction.equals("stop-track")){
            removeFromTrackerSessions(session, userAndImageId);
        }
        else if(payloadAction.equals("stop-broadcast")){
            removeFromBroadcastSession(userAndImageId);
        }else if(sessionsTracked.keySet().contains(userAndImageId)){
            ConcurrentWebSocketSessionDecorator[] oldSessions = sessionsTracked.get(userAndImageId);
            ConcurrentWebSocketSessionDecorator[] newSessions = oldSessions;;
            for(ConcurrentWebSocketSessionDecorator newSession : trackSessions){
                newSessions = addSession(newSessions, newSession);
            }
            sessionsTracked.replace(userAndImageId, oldSessions, newSessions);
        }
        else{
            sessionsTracked.put(userAndImageId, trackSessions);
        }
    }

    public void userHasMoved(String userId, String imageId, String position) throws ServerException {
        try{
            ConcurrentWebSocketSessionDecorator[] trackSessions = sessionsTracked.get(userId+"/"+imageId);
            TextMessage message = new TextMessage(position);
            for(ConcurrentWebSocketSessionDecorator s : trackSessions){
                super.sendWebSocketMessage(s, message);
            }
        }catch (NullPointerException e){
            log.info("User id : "+ userId +" on image id :"+ imageId +" is not followed");
        }
    }

    private void removeFromTrackerSessions(WebSocketSession session, String userAndImageId){
        ConcurrentWebSocketSessionDecorator[] oldSessions = sessionsTracked.get(userAndImageId);
        ConcurrentWebSocketSessionDecorator[] newSessions = removeSession(oldSessions, session);
        sessionsTracked.replace(userAndImageId, oldSessions, newSessions);
    }

    private void removeFromBroadcastSession(String userAndImageId){
        sendNotification(List.of(sessionsTracked.get(userAndImageId)));
        sessionsTracked.remove(userAndImageId);
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

