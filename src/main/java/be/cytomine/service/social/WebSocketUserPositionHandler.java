package be.cytomine.service.social;

import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.social.LastUserPosition;
import be.cytomine.exceptions.ServerException;
import be.cytomine.service.CytomineWebSocketHandler;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.image.SliceInstanceService;
import be.cytomine.service.security.SecUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WebSocketUserPositionHandler extends CytomineWebSocketHandler {

    public static Map<ConcurrentWebSocketSessionDecorator, ConcurrentWebSocketSessionDecorator[]> sessionsTracked = new HashMap<>();

    // sessionsTracked key -> "broadcastSessionId/imageId"
    public static Map<String, ConcurrentWebSocketSessionDecorator> sessionsBroadcast = new HashMap<>();

    // sessions key -> "userId"
    public static Map<String, ConcurrentWebSocketSessionDecorator[]> sessions = new HashMap<>();

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

        String userId = session.getAttributes().get("userId").toString();
        String imageId = session.getAttributes().get("imageId").toString();
        Boolean broadcast = Boolean.parseBoolean(session.getAttributes().get("broadcast").toString());

        if(broadcast){
            ConcurrentWebSocketSessionDecorator sessionDecorator = new ConcurrentWebSocketSessionDecorator(session, 1000, 8192);
            sessionsBroadcast.put(userId+"/"+imageId, sessionDecorator);
        }

        log.debug("Established user position WebSocket connection {}", session.getId());
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        if(Boolean.parseBoolean(session.getAttributes().get("broadcast").toString())){
            String userAndImageId = session.getAttributes().get("userId").toString() + '/' + session.getAttributes().get("imageId").toString();
            ConcurrentWebSocketSessionDecorator broadcastSession = sessionsBroadcast.get(userAndImageId);
            removeFromBroadcastSession(broadcastSession);
            removeFromTrackerSessions(broadcastSession);
        }
        else{
            removeSessionFromTrackerSessions(session);
        }

        removeFromSessions(session);
        log.debug("Closing user position WebSocket connection from {}", session.getRemoteAddress());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        String broadcasterId = message.getPayload().toString();
        String followerId = session.getAttributes().get("userId").toString();
        String imageId = session.getAttributes().get("imageId").toString();

        ConcurrentWebSocketSessionDecorator broadcastSession = sessionsBroadcast.get(broadcasterId+"/"+imageId);
        addToTrackedSessions(broadcastSession, sessions.get(followerId));
        moveFollower(Long.parseLong(broadcasterId), Long.parseLong(imageId), session);
    }

    private void addToTrackedSessions(ConcurrentWebSocketSessionDecorator broadcastSession, ConcurrentWebSocketSessionDecorator[] trackingSessions){
        if (this.sessionsTracked.containsKey(broadcastSession)) {
            ConcurrentWebSocketSessionDecorator[] newSessions = this.sessionsTracked.get(broadcastSession);

            for (ConcurrentWebSocketSessionDecorator newSession : trackingSessions) {
                newSessions = addSession(newSessions, newSession);
            }

            this.sessionsTracked.replace(broadcastSession, this.sessionsTracked.get(broadcastSession), newSessions);
        } else {
            this.sessionsTracked.put(broadcastSession, trackingSessions);
        }
    }

    private void moveFollower(Long userId, Long imageId, WebSocketSession session){

        // TODO : Uncomment to bypass authentication (websocket are not longer authenticated)
        // Comment for tests
        List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken("admin", "adminPassword", authorities);
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

        ImageInstance imageInstance = imageInstanceService.get(imageId);
        SecUser secUser = secUserService.get(userId);
        Optional<LastUserPosition> lastPosition = userPositionService.lastPositionByUser(imageInstance, null, secUser, false);
        if(lastPosition.isPresent()){
            TextMessage position = new TextMessage(lastPosition.get().toJsonObject().toJsonString());
            sendPosition(session, position);
        }
    }

    public void sendPositionToFollowers(String userId, String imageId, String position) throws ServerException {
        ConcurrentWebSocketSessionDecorator broadcastSession = this.sessionsBroadcast.get(userId+"/"+imageId);
        ConcurrentWebSocketSessionDecorator[] sessions = this.sessionsTracked.get(broadcastSession);
        if(sessions != null){
            sendPosition(sessions, position);
        }
    }

    private void sendPosition(ConcurrentWebSocketSessionDecorator[] sessions, String position){
        TextMessage message = new TextMessage(position);
        for(ConcurrentWebSocketSessionDecorator s : sessions){
            sendPosition(s, message);
        }
    }

    private void sendPosition(WebSocketSession session, TextMessage position){
        super.sendWebSocketMessage(session, position);
    }

    public List<String> getSessionsUserIds(ConcurrentWebSocketSessionDecorator[] sessions){
        List<String> userIds = new ArrayList<>();
        for(ConcurrentWebSocketSessionDecorator s : sessions){
            String userId = getSessionUserId(s);
            if(!userId.isEmpty()){
                userIds.add(userId);
            }
        }
        return userIds;
    }

    private String getSessionUserId(ConcurrentWebSocketSessionDecorator session){
        String userId = "";
            for(Map.Entry<String, ConcurrentWebSocketSessionDecorator[]> entry : this.sessions.entrySet()){
            List<String> entryIds = Arrays.stream(entry.getValue()).map(value -> value.getId()).toList();
            if(entryIds.contains(session.getId())){
                userId = entry.getKey();
                break;
            }
        }
        return userId;
    }

    private void removeSessionFromTrackerSessions(WebSocketSession session) {
        log.debug("Remove this tracking session from tracked sessions");
        for (Map.Entry<ConcurrentWebSocketSessionDecorator, ConcurrentWebSocketSessionDecorator[]> entry : this.sessionsTracked.entrySet()) {

            List<String> trackingSessionsIds = Arrays.stream(entry.getValue()).map(value -> value.getId()).toList();
            if (trackingSessionsIds.contains(session.getId())) {
                removeSessionFromTrackerSessions(session, entry.getKey());
            }
        }
    }

    private void removeSessionFromTrackerSessions(WebSocketSession session, ConcurrentWebSocketSessionDecorator trackedSession){
        ConcurrentWebSocketSessionDecorator[] oldSessions = this.sessionsTracked.get(trackedSession);
        ConcurrentWebSocketSessionDecorator[] newSessions = removeSession(oldSessions, session);
        this.sessionsTracked.replace(trackedSession, oldSessions, newSessions);
    }

    private void removeFromSessions(WebSocketSession session) {
        for (Map.Entry<String, ConcurrentWebSocketSessionDecorator[]> entry : this.sessions.entrySet()) {
            List<String> sessionsIds = Arrays.stream(entry.getValue()).map(value -> value.getId()).toList();

            if (sessionsIds.contains(session.getId())) {
                ConcurrentWebSocketSessionDecorator[] oldSessions = this.sessions.get(entry.getKey());
                ConcurrentWebSocketSessionDecorator[] newSessions = removeSession(oldSessions, session);
                this.sessions.replace(entry.getKey(), oldSessions, newSessions);
            }
        }
    }

    private void removeFromTrackerSessions(ConcurrentWebSocketSessionDecorator broadcastSession){
        log.debug("Remove this broadcast session from tracked sessions");
        ConcurrentWebSocketSessionDecorator[] sessionDecorators = this.sessionsTracked.get(broadcastSession);
        if(sessionDecorators != null){
            sendNotifications(List.of(sessionDecorators));
        }
        this.sessionsTracked.remove(broadcastSession);
    }

    private void removeFromBroadcastSession(ConcurrentWebSocketSessionDecorator broadcastSession){
        log.debug("Remove this broadcast session from broadcast sessions");
        for(Map.Entry<String, ConcurrentWebSocketSessionDecorator> entry : this.sessionsBroadcast.entrySet()){
            if(entry.getValue().getId().equals(broadcastSession.getId())){
                this.sessionsBroadcast.remove(entry.getKey());
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

    private void sendNotifications(List<ConcurrentWebSocketSessionDecorator> sessionDecorators){
        for(ConcurrentWebSocketSessionDecorator sessionDecorator : sessionDecorators){
            if(sessionDecorator.isOpen()){
                try{
                    sessionDecorator.sendMessage(new TextMessage("stop-track"));
                }catch (IOException e){
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

