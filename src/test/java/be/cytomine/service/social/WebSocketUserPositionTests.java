package be.cytomine.service.social;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.exceptions.ServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = CytomineCoreApplication.class)
@AutoConfigureMockMvc
@WithMockUser(username = "superadmin")
@ExtendWith(MockitoExtension.class)
public class WebSocketUserPositionTests {

    @Autowired
    BasicInstanceBuilder builder;

    @Autowired
    WebSocketUserPositionHandler webSocketUserPositionHandler;

    @AfterEach
    public void cleanSessions(){
        WebSocketUserPositionHandler.sessions = new HashMap<>();
        WebSocketUserPositionHandler.sessionsTracked = new HashMap<>();
        WebSocketUserPositionHandler.sessionsBroadcast = new HashMap<>();
    }

    @Test
    public void create_session_for_not_connected_user() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes("54", "imageId", "false"));

        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get("54/imageId")).isNull();
        assertThat(WebSocketUserPositionHandler.sessions.get("54")).isNull();
        webSocketUserPositionHandler.afterConnectionEstablished(session);
        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get("54/imageId")).isNull();
        assertThat(WebSocketUserPositionHandler.sessions.get("54")).isNotEmpty();
    }

    @Test
    public void create_broadcast_session_for_not_connected_user() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes("54", "imageId", "true"));

        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get("54/imageId")).isNull();
        assertThat(WebSocketUserPositionHandler.sessions.get("54")).isNull();
        webSocketUserPositionHandler.afterConnectionEstablished(session);
        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get("54/imageId")).isNotNull();
        assertThat(WebSocketUserPositionHandler.sessions.get("54")).isNotEmpty();
    }

    @Test
    public void add_session_for_already_connected_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectSession(sessionDecorator, "54", "imageId", "false");
        connectSession(sessionDecorator, "89", "imageId", "false");
        assertThat(WebSocketUserPositionHandler.sessions.get("54").length).isEqualTo(1);
        assertThat(WebSocketUserPositionHandler.sessions.get("89").length).isEqualTo(1);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes("54", "imageId", "false"));

        webSocketUserPositionHandler.afterConnectionEstablished(session);
        assertThat(WebSocketUserPositionHandler.sessions.get("54").length).isEqualTo(2);
        assertThat(WebSocketUserPositionHandler.sessions.get("89").length).isEqualTo(1);
    }

    @Test
    public void add_track_session_to_not_tracked_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;

        connectSession(sessionDecorator, userId, imageInstanceId,"true");

        // Should have created a broadcast session
        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId)).isNotNull();

        // Ask for follow the broadcast session
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes(userId, imageInstanceId, "false"));
        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId));

        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(1);
    }

    @Test
    public void add_track_session_to_already_tracked_user() {
        ConcurrentWebSocketSessionDecorator followerSession = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator broadcastSession = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;

        connectSession(followerSession, userId, imageInstanceId,"true");
        initFollowingSession(userAndImageId, broadcastSession, followerSession);

        // Ask for follow the broadcast session
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes(userId, imageInstanceId, "false"));
        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId));

        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(2);
    }

    @Test
    public void add_some_track_sessions_to_already_tracked_user() {
        ConcurrentWebSocketSessionDecorator followerSession = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator broadcastSession = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;
        initFollowingSession(userAndImageId, broadcastSession, followerSession);

        // Simulate that user is connected to Cytomine with 3 browsers (3 sessions)
        ConcurrentWebSocketSessionDecorator[] sessionsDecorator = {followerSession, followerSession, followerSession};
        WebSocketUserPositionHandler.sessions.put(userId, sessionsDecorator);

        // Ask for all sessions to follow the broadcast session
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes(userId, imageInstanceId, "false"));
        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId));

        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(4);
    }

    @Test
    public void remove_tracking_sessions_from_tracked_sessions() throws Exception {
        ConcurrentWebSocketSessionDecorator followerSession = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator broadcastSession = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;

        connectSession(followerSession, userId,imageInstanceId, "false");
        initFollowingSession(userAndImageId, broadcastSession, followerSession);

        // Broadcast session should be followed by follower session
        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(1);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes(userId, imageInstanceId, "false"));
        when(session.getId()).thenReturn("1234");
        when(followerSession.getId()).thenReturn("1234");

        // Close the followerSession (by calling afterConnectionClosed with mock session with same session id)
        webSocketUserPositionHandler.afterConnectionClosed(session, CloseStatus.NO_STATUS_CODE);

        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(0);
    }

    @Test
    public void remove_broadcasting_sessions_from_tracked_sessions() throws Exception {
        ConcurrentWebSocketSessionDecorator followerSession = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator broadcastSession = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;

        connectSession(followerSession, userId, imageInstanceId,"false");
        initFollowingSession(userAndImageId, broadcastSession, followerSession);

        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(1);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes(userId, imageInstanceId, "true"));
        when(session.getId()).thenReturn("1234");
        when(broadcastSession.getId()).thenReturn("1234");

        // Close the broadcastSession (by calling afterConnectionClosed with mock session with same session id)
        webSocketUserPositionHandler.afterConnectionClosed(session, CloseStatus.NO_STATUS_CODE);

        assertThat(WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId)).isNull();
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession)).isNull();
    }

    @Test
    public void update_position_of_tracked_user_send_message_works() throws IOException {
        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();

        WebSocketSession session = mock(WebSocketSession.class);
        connectSession(session, userId, imageInstanceId, "false");

        when(session.getAttributes()).thenReturn(sessionAttributes(userId, imageInstanceId, "false"));
        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId));

        when(session.isOpen()).thenReturn(true);
        doNothing().when(session).sendMessage(new TextMessage("position"));
        assertDoesNotThrow(() -> webSocketUserPositionHandler.sendPositionToFollowers(userId, imageInstanceId, "position"));
        verify(session, times(1)).sendMessage(new TextMessage("position"));
    }

    @Test
    public void update_position_of_not_tracked_user_do_nothing(){
        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        assertDoesNotThrow(() -> webSocketUserPositionHandler.sendPositionToFollowers(userId, imageInstanceId,"position"));
    }

    @Test
    public void update_position_of_tracked_user_send_message_fails() throws IOException {
        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();

        WebSocketSession session = mock(WebSocketSession.class);
        connectSession(session, userId, imageInstanceId, "false");

        when(session.getAttributes()).thenReturn(sessionAttributes(userId, imageInstanceId, "false"));
        webSocketUserPositionHandler.handleMessage(session, new TextMessage(userId));

        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("1234");
        doThrow(IOException.class).when(session).sendMessage(new TextMessage("position"));
        ServerException exception = assertThrows(ServerException.class, () -> webSocketUserPositionHandler.sendPositionToFollowers(userId, imageInstanceId, "position"));
        assertThat(exception.getMessage()).isEqualTo("Failed to send message to session : 1234");
    }

    @Test
    public void remove_session_if_connection_closed() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        ConcurrentWebSocketSessionDecorator followerSession = mock(ConcurrentWebSocketSessionDecorator.class);
        ConcurrentWebSocketSessionDecorator broadcastSession = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String userAndImageId = userId+"/imageId";
        connectSession(session, userId, "imageId", "false");
        initFollowingSession(userAndImageId, broadcastSession, followerSession);

        assertThat(WebSocketUserPositionHandler.sessions.get(userId).length).isEqualTo(1);
        ConcurrentWebSocketSessionDecorator createdSession = WebSocketUserPositionHandler.sessionsBroadcast.get(userAndImageId);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(1);

        when(session.getId()).thenReturn("1");
        when(followerSession.getId()).thenReturn("1");
        // Close the session (by calling afterConnectionClosed with mock session with same session id)
        webSocketUserPositionHandler.afterConnectionClosed(session, CloseStatus.NO_STATUS_CODE);

        assertThat(WebSocketUserPositionHandler.sessions.get(userId).length).isEqualTo(0);
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(createdSession).length).isEqualTo(0);
    }


    private void connectSession(WebSocketSession session, String userId, String imageId, String broadcast){
        when(session.getAttributes()).thenReturn(Map.of("userId", userId, "imageId", imageId, "broadcast", broadcast));
        webSocketUserPositionHandler.afterConnectionEstablished(session);
    }

    private void initFollowingSession(String userAndImageId, ConcurrentWebSocketSessionDecorator broadcastSession, ConcurrentWebSocketSessionDecorator followerSession){
        WebSocketUserPositionHandler.sessionsBroadcast.put(userAndImageId, broadcastSession);
        WebSocketUserPositionHandler.sessionsTracked.put(broadcastSession, new ConcurrentWebSocketSessionDecorator[]{followerSession});
        assertThat(WebSocketUserPositionHandler.sessionsTracked.get(broadcastSession).length).isEqualTo(1);
    }

    private Map<String, Object> sessionAttributes(String userId, String imageId, String broadcast){
        return Map.of("userId", userId, "imageId", imageId, "broadcast", broadcast);
    }
}