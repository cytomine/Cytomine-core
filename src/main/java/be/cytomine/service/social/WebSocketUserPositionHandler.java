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

import java.io.IOException;
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
        }
        else if(sessionsTracked.keySet().contains(userAndImageId)){
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

    public List<String> getSessionsUserIds(ConcurrentWebSocketSessionDecorator[] sessions){
        List<String> userIds = new ArrayList<>();
        for(ConcurrentWebSocketSessionDecorator decorator : sessions){
            String userId = getSessionUserId(decorator);
            if(!userId.isEmpty()){
                userIds.add(userId);
            }
        }
        return userIds;
    }

    private String getSessionUserId(ConcurrentWebSocketSessionDecorator session){
        String userId = "";
            for(Map.Entry<Object, ConcurrentWebSocketSessionDecorator[]> entry : sessions.entrySet()){
            List<String> entryIds = Arrays.stream(entry.getValue()).map(value -> value.getId()).collect(Collectors.toList());
            if(entryIds.contains(session.getId())){
                userId = entry.getKey().toString();
                break;
            }
        }
        return userId;
    }


    private void removeFromTrackerSessions(WebSocketSession session, String userAndImageId){
        ConcurrentWebSocketSessionDecorator[] oldSessions = sessionsTracked.get(userAndImageId);
        ConcurrentWebSocketSessionDecorator[] newSessions = removeSession(oldSessions, session);
        sessionsTracked.replace(userAndImageId, oldSessions, newSessions);
    }

    private void removeFromBroadcastSession(String userAndImageId){
        ConcurrentWebSocketSessionDecorator[] sessionDecorators = sessionsTracked.get(userAndImageId);
        if(sessionDecorators != null){
            List<ConcurrentWebSocketSessionDecorator> sessionDecoratorLis = List.of(sessionsTracked.get(userAndImageId));
            sendNotifications(sessionDecoratorLis);
            sessionsTracked.remove(userAndImageId);
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

    private void sendNotifications(List<ConcurrentWebSocketSessionDecorator> sessionDecorators){
        for(ConcurrentWebSocketSessionDecorator sessionDecorator : sessionDecorators){
            if(sessionDecorator.isOpen()){
                try{
                    sessionDecorator.sendMessage(new TextMessage("stop-track"));
                }catch (IOException e){
                    log.error("Session : " + sessionDecorator.getId() + " not found");
                }
            }
        }
        // TODO : Send notification via future notification system (lvl importance of 2 to bypass queue)
        /*for (Map.Entry<Object, ConcurrentWebSocketSessionDecorator[]> entry : sessions.entrySet()) {
            for(ConcurrentWebSocketSessionDecorator s : entry.getValue()){
                if(decoratorsSession.contains(s)){
                }
            }
        }*/
    }
}

