package be.cytomine.utils;

import be.cytomine.domain.security.SecUser;
import org.apache.commons.codec.binary.Base64;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.stream.Stream;

public class SecurityUtils {
    public static String generateKeys(String method, String content_md5, String content_type, String date, String queryString, String path, SecUser user) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String canonicalHeaders = method + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n";
        String canonicalExtensionHeaders = "";
        String canonicalResource = path + queryString;
        String messageToSign = canonicalHeaders + canonicalExtensionHeaders + canonicalResource;

        String key = user.getPrivateKey();
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
        // get an hmac_sha1 Mac instance and initialize with the signing key
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        // compute the hmac on input data bytes
        byte[] rawHmac = mac.doFinal(new String(messageToSign.getBytes(), "UTF-8").getBytes());

        // base64-encode the hmac
        byte[] signatureBytes = Base64.encodeBase64(rawHmac);

        String signature = new String(signatureBytes);
        return signature;
    }

    /**
     * Get the login of the current user.
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }


    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                getAuthorities(authentication).noneMatch("ANONYMOUS"::equals);
    }

    /**
     * If the current user has a specific authority (security role).
     * @param authority the authority to check.
     * @return true if the current user has the authority, false otherwise.
     */
    public static boolean isCurrentUserInRole(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                getAuthorities(authentication).anyMatch(authority::equals);
    }

    private static Stream<String> getAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority);
    }

//    /**
//     * Execute a closure with the current authentication. Assumes that there's an authentication in the
//     * http session and that the closure is running in a separate thread from the web request, so the
//     * context and authentication aren't available to the standard ThreadLocal.
//     *
//     * @param closure the code to run
//     * @return the closure's return value
//     */
//    public static Object doWithAuth(@SuppressWarnings("rawtypes") final Closure closure) {
//        boolean set = false;
//        if (SecurityContextHolder.getContext().getAuthentication() == null && SecurityRequestHolder.getRequest() != null) {
//            HttpSession httpSession = SecurityRequestHolder.getRequest().getSession(false);
//            SecurityContext securityContext = null;
//            if (httpSession != null) {
//                securityContext = (SecurityContext)httpSession.getAttribute(
//                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
//                if (securityContext != null) {
//                    SecurityContextHolder.setContext(securityContext);
//                    set = true;
//                }
//            }
//        }
//
//        try {
//            return closure.call();
//        }
//        finally {
//            if (set) {
//                SecurityContextHolder.clearContext();
//            }
//        }
//    }

    public static void doWithAuth(ApplicationContext applicationContext, final String username, @SuppressWarnings("rawtypes") final Runnable executable) {
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication();
        reauthenticate(applicationContext, username, null);

        try {
            executable.run();
        }
        finally {
            if (previousAuth == null) {
                SecurityContextHolder.clearContext();
            }
            else {
                SecurityContextHolder.getContext().setAuthentication(previousAuth);
            }
        }
    }

    public static void reauthenticate(ApplicationContext applicationContext, final String username, final String password) {
        UserDetailsService userDetailsService = getBean(applicationContext,"userDetailsService");
        UserCache userCache = getBean(applicationContext,"userCache");

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userDetails, password == null ? userDetails.getPassword() : password, userDetails.getAuthorities()));
        userCache.removeUserFromCache(username);
    }

    private static <T> T getBean(ApplicationContext applicationContext, final String name) {
        return (T)applicationContext.getBean(name);
    }
}
