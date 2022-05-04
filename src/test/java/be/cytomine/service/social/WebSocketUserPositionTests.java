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
        connectTwoSessions(sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));

        webSocketUserPositionHandler.afterConnectionEstablished(session);
        assertThat(webSocketUserPositionHandler.sessions.get("54").length).isEqualTo(2);
        assertThat(webSocketUserPositionHandler.sessions.get("89").length).isEqualTo(1);
    }

    @Test
    public void add_track_session_to_not_tracked_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectTwoSessions(sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));

        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89/imageId")).isNull();
        webSocketUserPositionHandler.handleMessage(session, new TextMessage("no-action/89/imageId"));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89/imageId").length).isEqualTo(1);
    }

    @Test
    public void add_track_session_to_already_tracked_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectTwoSessions(sessionDecorator);
        initTrackedSession("89/imageId", sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));

        webSocketUserPositionHandler.handleMessage(session, new TextMessage("no-action/89/imageId"));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89/imageId").length).isEqualTo(2);
    }

    @Test
    public void add_some_track_sessions_to_already_tracked_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectTwoSessions(sessionDecorator);
        initTrackedSession("89/imageId", sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        // Simulate that user is connected to Cytomine with 3 browsers (3 sessions)
        ConcurrentWebSocketSessionDecorator[] sessionsDecorator = {sessionDecorator, sessionDecorator, sessionDecorator};
        WebSocketUserPositionHandler.sessions.put("54", sessionsDecorator);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));

        webSocketUserPositionHandler.handleMessage(session, new TextMessage("no-action/89/imageId"));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89/imageId").length).isEqualTo(4);
    }

    @Test
    public void remove_tracking_sessions_from_tracked_sessions() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectTwoSessions(sessionDecorator);
        initTrackedSession("89/imageId", sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));
        when(session.getId()).thenReturn("1234");
        when(sessionDecorator.getId()).thenReturn("1234");

        webSocketUserPositionHandler.handleMessage(session, new TextMessage("stop-track/89/imageId"));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89/imageId").length).isEqualTo(0);
    }

    @Test
    public void remove_broadcasting_sessions_from_tracked_sessions() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectTwoSessions(sessionDecorator);
        initTrackedSession("89/imageId", sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "89"));

        webSocketUserPositionHandler.handleMessage(session, new TextMessage("stop-broadcast/89/imageId"));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89/imageId")).isNull();
    }

    @Test
    public void update_position_of_tracked_user_send_message_works() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        connectSession(session);

        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));
        webSocketUserPositionHandler.handleMessage(session, new TextMessage("no-action/89/imageId"));

        when(session.isOpen()).thenReturn(true);
        doNothing().when(session).sendMessage(new TextMessage("position"));
        assertDoesNotThrow(() -> webSocketUserPositionHandler.userHasMoved("89", "imageId", "position"));
        verify(session, times(1)).sendMessage(new TextMessage("position"));
    }

    @Test
    public void update_position_of_not_tracked_user_do_nothing(){
        assertDoesNotThrow(() -> webSocketUserPositionHandler.userHasMoved("54", "imageId","position"));
    }

    @Test
    public void update_position_of_tracked_user_send_message_fails() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        connectSession(session);

        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));
        webSocketUserPositionHandler.handleMessage(session, new TextMessage("no-action/89/imageId"));

        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("1234");
        doThrow(IOException.class).when(session).sendMessage(new TextMessage("position"));
        ServerException exception = assertThrows(ServerException.class, () -> webSocketUserPositionHandler.userHasMoved("89", "imageId", "position"));
        assertThat(exception.getMessage()).isEqualTo("Failed to send message to session : 1234");
    }

    private void connectSession(WebSocketSession session ){
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));
        webSocketUserPositionHandler.afterConnectionEstablished(session);
    }

    private void connectTwoSessions(ConcurrentWebSocketSessionDecorator sessionDecorator){
        ConcurrentWebSocketSessionDecorator[] sessionsDecorator = {sessionDecorator};
        WebSocketUserPositionHandler.sessions.put("54", sessionsDecorator);
        WebSocketUserPositionHandler.sessions.put("89", sessionsDecorator);
        assertThat(WebSocketUserPositionHandler.sessions.get("54").length).isEqualTo(1);
        assertThat(WebSocketUserPositionHandler.sessions.get("89").length).isEqualTo(1);
    }

    private void initTrackedSession(String trackedSessionId, ConcurrentWebSocketSessionDecorator sessionDecorator){
        webSocketUserPositionHandler.sessionsTracked.put(trackedSessionId, new ConcurrentWebSocketSessionDecorator[]{sessionDecorator});
        assertThat(webSocketUserPositionHandler.sessionsTracked.get(trackedSessionId).length).isEqualTo(1);
    }


}