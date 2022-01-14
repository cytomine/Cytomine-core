package be.cytomine.security;

import be.cytomine.domain.security.User;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

public class TokenKeyFilter extends OncePerRequestFilter {

    private final Logger log = LoggerFactory.getLogger(TokenKeyFilter.class);

    private final DomainUserDetailsService domainUserDetailsService;

    private final UserRepository userRepository;


    public TokenKeyFilter(DomainUserDetailsService domainUserDetailsService, UserRepository userRepository) {
        this.domainUserDetailsService = domainUserDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //log with token id TODO: auth with authWithToken
        boolean token = false; //tryAPIAUhtentificationWithToken(request, response);
        if(!token) {
            //with signature (in header)
            tryAPIAuthentification(request, response);
        }

        filterChain.doFilter(request, response);



        //
//        String apiKey = extractApiKey(request);
//
//        if (apiKey == null) {
//            log.debug("Not authenticating the request because no api key was found");
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        //ApiKeyAuthenticationToken authenticationToken = new ApiKeyAuthenticationToken(apiKey);
//
//        try {
//            log.info("Authenticating the request with api key {}", partialApiKey(apiKey));
//            SecurityContext context = SecurityContextHolder.createEmptyContext();
//            context.setAuthentication(domainUserDetailsService.getAuthentication(apiKey));
//            SecurityContextHolder.setContext(context);
//
//            filterChain.doFilter(request, response);
//        } catch (AuthenticationException failed) {
//            SecurityContextHolder.clearContext();
//            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid Authorization code");
//            this.logger.debug("Authentication request for failed: " + failed);
//        }
    }

    /**
     * http://code.google.com/apis/storage/docs/reference/v1/developer-guidev1.html#authentication
     */
    private boolean tryAPIAuthentification(HttpServletRequest request, HttpServletResponse response) {
        if (request.getHeader("date") == null) {
            return false;
        }
        if (request.getHeader("host") == null) {
            return false;
        }
        String authorization = request.getHeader("authorization");
        if (authorization == null) {
            return false;
        }
        if (!authorization.startsWith("CYTOMINE") || !authorization.contains(" ") || !authorization.contains(":")) {
            return false;
        }
        try {

            String content_md5 = (request.getHeader("content-MD5") != null) ? request.getHeader("content-MD5") : "";

            String content_type = (request.getHeader("content-type") != null) ? request.getHeader("content-type") : "";
            content_type = (request.getHeader("Content-Type") != null) ? request.getHeader("Content-Type") : content_type;
            String date = (request.getHeader("date") != null) ? request.getHeader("date") : "";

            String queryString = (request.getQueryString() != null) ? "?" + request.getQueryString() : "";

            String path = request.getRequestURI().toString(); // TODO; forwardUri?
            // original URI Request

            String accessKey = authorization.substring(authorization.indexOf(" ") + 1, authorization.indexOf(":"));
            String authorizationSign = authorization.substring(authorization.indexOf(":") + 1);

            Optional<User> user = userRepository.findByPublicKeyAndEnabled(accessKey,true);

            if (user.isEmpty()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return false;
            } else {
                String signature = SecurityUtils.generateKeys(request.getMethod(),content_md5, content_type,date,queryString,path,user.get());
                if (authorizationSign.equals(signature)) {
                    this.reauthenticate(user.get().getUsername(), null);
                    return true;
                } else {
                    // the java client does not set content-type, so we override the header to application/json BEFORE this authentication.
                    // So the client thinks content-type is "" while spring boot set it to application/json. In order to match the client signature, we generate it
                    // with an empty value.
                    // => it would be better to improve the java client to set a valid content type.
                    String signatureWithEmptyContentType = SecurityUtils.generateKeys(request.getMethod(),content_md5, "",date,queryString,path,user.get());
                    if (authorizationSign.equals(signatureWithEmptyContentType)) {
                        this.reauthenticate(user.get().getUsername(), null);
                        return true;
                    }


                    return false;
                }
            }



        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
//
//    private boolean tryAPIAUhtentificationWithToken(ServletRequest request, ServletResponse response) {
//        String tokenKey = request.getParameter("tokenKey");
//
//        if(tokenKey!=null) {
//            String username = request.getParameter("username");
//            SecUser user = secUserRepository.findByUsernameLikeIgnoreCase(username).get(); //we are not logged, we bypass the service
//            AuthWithToken authToken = AuthWithToken.findByTokenKeyAndUser(tokenKey, user)
//            //check first if a entry is made for this token
//            if (authToken && authToken.isValid())  {
//                this.reauthenticate(user.getUsername(), null);
//                return true;
//            } else {
//                return false;
//            }
//        } else {
//            return false;
//        }
//    }

    /**
     * Rebuild an Authentication for the given username and register it in the security context.
     * Typically used after updating a user's authorities or other auth-cached info.
     * <p/>
     * Also removes the user from the user cache to force a refresh at next login.
     *
     * @param username the user's login name
     * @param password optional
     */
    public void reauthenticate(final String username, final String password) {
        UserDetailsService userDetailsService = this.domainUserDetailsService;

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userDetails, password == null ? userDetails.getPassword() : password, userDetails.getAuthorities()));
        //userCache.removeUserFromCache(username); TODO?
    }




//    private String extractApiKey(HttpServletRequest request) {
//        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
//
//        if (authorization != null) {
//            log.debug("Authorization header is \"{}\"", partialAuthorizationHeader(authorization));
//            String[] authorizationParts = authorization.split(" ");
//            log.debug("Type is \"{}\"", authorizationParts[0]);
//            if (isApiKey(authorizationParts)) {
//                if (authorizationParts.length > 1) {
//                    log.debug("Returning api key \"{}\"", partialApiKey(authorizationParts[1]));
//                    return authorizationParts[1];
//                } else {
//                    return "";
//                }
//            }
//            log.debug("Authorization is not an api-key");
//            return null;
//        }
//        log.debug("Authorization header is missing. No api key is provided");
//        return null;
//    }
//
//    private boolean isApiKey(String[] authorizationParts) {
//        return authorizationParts.length > 0 && authorizationParts[0].equalsIgnoreCase("api-key");
//    }
//
//    private static String partialApiKey(String apiKey) {
//        return safelyPartialize(apiKey, 5);
//    }
//
//    private static String partialAuthorizationHeader(String authorizationHeader) {
//        return safelyPartialize(authorizationHeader, 13);
//    }
//
//    private static String safelyPartialize(String s, int partLength) {
//        return s != null ?
//                s.substring(0, min(partLength, s.length())) + "..." :
//                null;
//    }
}
