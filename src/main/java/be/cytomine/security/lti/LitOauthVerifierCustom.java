package be.cytomine.security.lti;

import be.cytomine.config.properties.ApplicationProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.oauth.*;
import net.oauth.server.OAuthServlet;
import org.imsglobal.lti.launch.LtiError;
import org.imsglobal.lti.launch.LtiLaunch;
import org.imsglobal.lti.launch.LtiOauthVerifier;
import org.imsglobal.lti.launch.LtiVerificationResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
@AllArgsConstructor
public class LitOauthVerifierCustom extends LtiOauthVerifier {

    private final String url;

    @Override
    public LtiVerificationResult verify(HttpServletRequest request, String privateKey) {
        log.debug("url = {}", url);
        OAuthMessage oam = OAuthServlet.getMessage(request, url);
        String oauth_consumer_key;
        try {
            oauth_consumer_key = oam.getConsumerKey();
        } catch (IOException e) {
            return new LtiVerificationResult(false, LtiError.BAD_REQUEST, "Unable to find consumer key in message");
        }
        log.debug("oauth_consumer_key = {}", oauth_consumer_key);
        OAuthValidator oav = new SimpleOAuthValidator();
        OAuthConsumer cons = new OAuthConsumer(null, oauth_consumer_key, privateKey, null);
        OAuthAccessor acc = new OAuthAccessor(cons);
        log.debug("privateKey = {}", privateKey);
        try {
            oav.validateMessage(oam, acc);
        } catch (OAuthException  | IOException | java.net.URISyntaxException e) {
            log.error("Cannot validate LTI", e);
            return new LtiVerificationResult(false, LtiError.BAD_REQUEST, "Failed to validate: " + e.getLocalizedMessage());
        }
        return new LtiVerificationResult(true, new LtiLaunch(request));
    }




}
