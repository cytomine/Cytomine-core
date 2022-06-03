package be.cytomine.config;

import be.cytomine.domain.image.ImageInstance;
import be.cytomine.domain.project.Project;
import be.cytomine.exceptions.ObjectNotFoundException;
import be.cytomine.repository.image.ImageInstanceRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repository.security.AclRepository;
import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.service.image.ImageInstanceService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.service.social.WebSocketUserPositionHandler;
import be.cytomine.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static be.cytomine.config.security.JWTFilter.resolveToken;
import static org.springframework.security.acls.domain.BasePermission.READ;

@Configuration
@EnableWebSocket
@Transactional
public class WebSocketConfiguration implements WebSocketConfigurer {

    @Autowired
    WebSocketUserPositionHandler webSocketUserPositionHandler;

    @Autowired
    TokenProvider tokenProvider;

    @Autowired
    ImageInstanceRepository imageInstanceRepository;

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    ProjectRepository projectRepository;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketUserPositionHandler,"/user-position/*/*/*").setAllowedOrigins("*").addInterceptors(idInterceptor());
    }

    @Bean
    @Transactional
    public HandshakeInterceptor idInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, org.springframework.web.socket.WebSocketHandler wsHandler, Map<String, Object> attributes) throws MalformedURLException, UnsupportedEncodingException {
                String path = request.getURI().getPath();
                String[] splitPath = path.split("/");

                String userId = splitPath[splitPath.length - 3];
                String imageId = splitPath[splitPath.length - 2];
                String broadcast = splitPath[splitPath.length - 1];

                attributes.put("userId", userId);
                attributes.put("imageId", imageId);
                attributes.put("broadcast", broadcast);

                Map<String, String> params = StringUtils.splitQuery(request.getURI().toURL());

                Authentication authentication = tokenProvider.getAuthentication(resolveToken(params.get("Authorization")));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                securityACLService.checkIsCurrentUserSameUser(Long.parseLong(userId));

                Long projectId = projectRepository.findByProjectIdByImageInstanceId(Long.parseLong(imageId));

                securityACLService.check(projectId, Project.class, READ);

                return true;
            }
            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, org.springframework.web.socket.WebSocketHandler wsHandler, Exception exception) {}
        };
    }
}
