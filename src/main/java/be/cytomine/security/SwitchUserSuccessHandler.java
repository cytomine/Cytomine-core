package be.cytomine.security;

import be.cytomine.security.jwt.TokenProvider;
import be.cytomine.security.jwt.TokenType;
import be.cytomine.utils.JsonObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * Spring Security success handler, specialized for Ajax requests.
 */
public class SwitchUserSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    TokenProvider tokenProvider;

    Long tokenValidityInSeconds;

    public SwitchUserSuccessHandler(TokenProvider tokenProvider, Long tokenValidityInSeconds) {
        this.tokenProvider = tokenProvider;
        this.tokenValidityInSeconds = tokenValidityInSeconds;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String token = tokenProvider.createToken(authentication, TokenType.IMPERSONATE);

        response.getWriter().println(JsonObject.of(
                "id_token", token,
                "created", new Date().getTime(),
                "validity", new Date(System.currentTimeMillis() + (tokenValidityInSeconds * 1000)).getTime()).toJsonString()
        );

        response.setStatus(HttpServletResponse.SC_OK);
    }
}