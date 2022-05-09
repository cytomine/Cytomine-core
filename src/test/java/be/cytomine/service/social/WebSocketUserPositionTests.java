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
        webSocketUserPositionHandler.sessions = new HashMap<>();
        webSocketUserPositionHandler.sessionsTracked = new HashMap<>();
    }

    @Test
    public void create_session_for_not_connected_user() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));

        assertThat(webSocketUserPositionHandler.sessions.get("54")).isNull();
        webSocketUserPositionHandler.afterConnectionEstablished(session);
        assertThat(webSocketUserPositionHandler.sessions.get("54")).isNotEmpty();
    }

    @Test
    public void add_session_for_already_connected_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectSession(sessionDecorator, "54");
        connectSession(sessionDecorator, "89");

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));

        webSocketUserPositionHandler.afterConnectionEstablished(session);
        assertThat(webSocketUserPositionHandler.sessions.get("54").length).isEqualTo(2);
        assertThat(webSocketUserPositionHandler.sessions.get("89").length).isEqualTo(1);
    }

    @Test
    public void add_track_session_to_not_tracked_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectSession(sessionDecorator, "54");

        String userAndImageId = initTrackedUserAndImage();

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));

        assertThat(webSocketUserPositionHandler.sessionsTracked.get(userAndImageId)).isNull();
        webSocketUserPositionHandler.handleMessage(session, new TextMessage("no-action/"+userAndImageId));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get(userAndImageId).length).isEqualTo(1);
    }

    @Test
    public void add_track_session_to_already_tracked_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectSession(sessionDecorator, "54");

        String userAndImageId = initTrackedUserAndImage();
        initTrackedSession(userAndImageId, sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));

        webSocketUserPositionHandler.handleMessage(session, new TextMessage("no-action/"+userAndImageId));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get(userAndImageId).length).isEqualTo(2);
    }

    @Test
    public void add_some_track_sessions_to_already_tracked_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);

        String userId = builder.given_a_user().getId().toString();
        String userAndImageId = initTrackedUserAndImage(userId);
        initTrackedSession(userAndImageId, sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        // Simulate that user is connected to Cytomine with 3 browsers (3 sessions)
        ConcurrentWebSocketSessionDecorator[] sessionsDecorator = {sessionDecorator, sessionDecorator, sessionDecorator};
        WebSocketUserPositionHandler.sessions.put(userId, sessionsDecorator);
        when(session.getAttributes()).thenReturn(Map.of("userID", userId));

        webSocketUserPositionHandler.handleMessage(session, new TextMessage("no-action/"+userAndImageId));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get(userAndImageId).length).isEqualTo(4);
    }

    @Test
    public void remove_tracking_sessions_from_tracked_sessions() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectSession(sessionDecorator, "54");

        String userAndImageId = initTrackedUserAndImage();
        initTrackedSession(userAndImageId, sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));
        when(session.getId()).thenReturn("1234");
        when(sessionDecorator.getId()).thenReturn("1234");

        webSocketUserPositionHandler.handleMessage(session, new TextMessage("stop-track/"+userAndImageId));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get(userAndImageId).length).isEqualTo(0);
    }

    @Test
    public void remove_broadcasting_sessions_from_tracked_sessions() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectSession(sessionDecorator, "89");

        String userAndImageId = initTrackedUserAndImage();
        initTrackedSession(userAndImageId, sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "89"));

        webSocketUserPositionHandler.handleMessage(session, new TextMessage("stop-broadcast/"+userAndImageId));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get(userAndImageId)).isNull();
    }

    @Test
    public void update_position_of_tracked_user_send_message_works() throws IOException {
        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;

        WebSocketSession session = mock(WebSocketSession.class);
        connectSession(session, userId);

        when(session.getAttributes()).thenReturn(Map.of("userID", userId));
        webSocketUserPositionHandler.handleMessage(session, new TextMessage("no-action/"+userAndImageId));

        when(session.isOpen()).thenReturn(true);
        doNothing().when(session).sendMessage(new TextMessage("position"));
        assertDoesNotThrow(() -> webSocketUserPositionHandler.userHasMoved(userId, imageInstanceId, "position"));
        verify(session, times(1)).sendMessage(new TextMessage("position"));
    }

    @Test
    public void update_position_of_not_tracked_user_do_nothing(){
        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        assertDoesNotThrow(() -> webSocketUserPositionHandler.userHasMoved(userId, imageInstanceId,"position"));
    }

    @Test
    public void update_position_of_tracked_user_send_message_fails() throws IOException {
        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        String userAndImageId = userId+"/"+imageInstanceId;

        WebSocketSession session = mock(WebSocketSession.class);
        connectSession(session, userId);

        when(session.getAttributes()).thenReturn(Map.of("userID", userId));
        webSocketUserPositionHandler.handleMessage(session, new TextMessage("no-action/"+userAndImageId));

        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("1234");
        doThrow(IOException.class).when(session).sendMessage(new TextMessage("position"));
        ServerException exception = assertThrows(ServerException.class, () -> webSocketUserPositionHandler.userHasMoved(userId, imageInstanceId, "position"));
        assertThat(exception.getMessage()).isEqualTo("Failed to send message to session : 1234");
    }

    @Test
    public void remove_session_if_connection_closed() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("1");
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        when(sessionDecorator.getId()).thenReturn("1");

        String userId = builder.given_a_user().getId().toString();
        connectSession(session, userId);
        initTrackedSession(userId+"/imageId", sessionDecorator);

        assertThat(webSocketUserPositionHandler.sessions.get(userId).length).isEqualTo(1);
        assertThat(webSocketUserPositionHandler.sessionsTracked.get(userId+"/imageId").length).isEqualTo(1);

        webSocketUserPositionHandler.afterConnectionClosed(session, CloseStatus.NO_STATUS_CODE);

        assertThat(webSocketUserPositionHandler.sessions.get(userId).length).isEqualTo(0);
        assertThat(webSocketUserPositionHandler.sessionsTracked.get(userId+"/imageId").length).isEqualTo(0);
    }


    private void connectSession(WebSocketSession session, String userId){
        when(session.getAttributes()).thenReturn(Map.of("userID", userId));
        webSocketUserPositionHandler.afterConnectionEstablished(session);
    }

    private String initTrackedUserAndImage(String userId){
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        return userId+"/"+imageInstanceId;
    }

    private String initTrackedUserAndImage(){
        String userId = builder.given_a_user().getId().toString();
        String imageInstanceId = builder.given_an_image_instance().getId().toString();
        return userId+"/"+imageInstanceId;
    }

    private void initTrackedSession(String trackedSessionId, ConcurrentWebSocketSessionDecorator sessionDecorator){
        webSocketUserPositionHandler.sessionsTracked.put(trackedSessionId, new ConcurrentWebSocketSessionDecorator[]{sessionDecorator});
        assertThat(webSocketUserPositionHandler.sessionsTracked.get(trackedSessionId).length).isEqualTo(1);
    }
}