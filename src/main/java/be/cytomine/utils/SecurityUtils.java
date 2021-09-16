package be.cytomine.utils;

import be.cytomine.domain.security.SecUser;
import org.apache.commons.codec.binary.Base64;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
}
