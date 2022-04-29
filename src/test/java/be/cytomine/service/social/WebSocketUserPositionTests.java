package be.cytomine.service.social;

import be.cytomine.BasicInstanceBuilder;
import be.cytomine.CytomineCoreApplication;
import be.cytomine.domain.notification.Notification;
import be.cytomine.domain.notification.NotificationUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ServerException;
import be.cytomine.repository.notification.NotificationUserRepository;
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
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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

    @Autowired
    NotificationUserRepository notificationUserRepository;

    @AfterEach
    public void resetSessions(){
        webSocketUserPositionHandler.sessions = new HashMap<>();
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

        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89")).isNull();
        webSocketUserPositionHandler.handleMessage(session, new TextMessage("89"));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89").length).isEqualTo(1);
    }

    @Test
    public void add_track_session_to_already_tracked_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectTwoSessions(sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));
        webSocketUserPositionHandler.sessionsTracked.put("89", new ConcurrentWebSocketSessionDecorator[]{sessionDecorator});

        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89").length).isEqualTo(1);
        webSocketUserPositionHandler.handleMessage(session, new TextMessage("89"));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89").length).isEqualTo(2);
    }

    @Test
    public void add_some_track_sessions_to_already_tracked_user() {
        ConcurrentWebSocketSessionDecorator sessionDecorator = mock(ConcurrentWebSocketSessionDecorator.class);
        connectTwoSessions(sessionDecorator);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userID", "54"));
        webSocketUserPositionHandler.sessionsTracked.put("89", new ConcurrentWebSocketSessionDecorator[]{sessionDecorator});

        // Simulate that user is connected to Cytomine with 3 browsers (3 sessions)
        ConcurrentWebSocketSessionDecorator[] sessionsDecorator = {sessionDecorator, sessionDecorator, sessionDecorator};
        WebSocketUserPositionHandler.sessions.put("54", sessionsDecorator);

        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89").length).isEqualTo(1);
        webSocketUserPositionHandler.handleMessage(session, new TextMessage("89"));
        assertThat(webSocketUserPositionHandler.sessionsTracked.get("89").length).isEqualTo(4);
    }


    private void connectTwoSessions(ConcurrentWebSocketSessionDecorator sessionDecorator){
        ConcurrentWebSocketSessionDecorator[] sessionsDecorator = {sessionDecorator};
        WebSocketUserPositionHandler.sessions.put("54", sessionsDecorator);
        WebSocketUserPositionHandler.sessions.put("89", sessionsDecorator);
        assertThat(WebSocketUserPositionHandler.sessions.get("54").length).isEqualTo(1);
        assertThat(WebSocketUserPositionHandler.sessions.get("89").length).isEqualTo(1);
    }
}