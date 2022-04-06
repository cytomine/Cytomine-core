package be.cytomine.config.security;

import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.security.jwt.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Locale;

import static be.cytomine.security.jwt.TokenType.SHORT_TERM;
import static org.springframework.jmx.export.naming.IdentityNamingStrategy.TYPE_KEY;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
public class JWTFilter extends GenericFilterBean {

    public static final String AUTHORIZATION_HEADER = "Authorization";

    private final TokenProvider tokenProvider;

    public JWTFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String jwt = resolveToken(httpServletRequest);

        if (StringUtils.hasText(jwt)) {
            Jws<Claims> claimsJws = this.tokenProvider.decodeToken(jwt);
            if (claimsJws!=null) {
                if (isShortTermToken(claimsJws) && !((HttpServletRequest) servletRequest).getMethod().toUpperCase(Locale.ROOT).equals("GET")) {
                    throw new ForbiddenException("Short term token can only be use with GET request");
                }

                Authentication authentication = this.tokenProvider.getAuthentication(jwt, claimsJws.getBody());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        bearerToken = request.getParameter(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isShortTermToken(Jws<Claims> claimsJws) {
        return claimsJws.getBody().containsKey(TYPE_KEY) && claimsJws.getBody().get(TYPE_KEY).equals(SHORT_TERM.toString());
    }
}
