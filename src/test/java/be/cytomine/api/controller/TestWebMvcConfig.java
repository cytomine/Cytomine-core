package be.cytomine.api.controller;

import be.cytomine.config.properties.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnabledIf(expression = "#{environment.acceptsProfiles('saml')}", loadContext = true)
public class TestWebMvcConfig implements WebMvcConfigurer {


    @Autowired
    private ApplicationProperties applicationProperties;

    public void addViewControllers(ViewControllerRegistry registry) {
        Map<String, String> shibboleth = applicationProperties.getAuthentication().getSaml2().getShibboleth();
        if (shibboleth != null) {
            registry.addViewController(shibboleth.get("ssoRedirectURI")).setViewName("forward:" + shibboleth.get("ssoURI"));
        }
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        PathPatternParser pathPatternParser = new PathPatternParser();

        configurer.setPathMatcher(antPathMatcher);
        configurer.setPatternParser(pathPatternParser);
    }


    @Bean
    public PathMatcher pathMatcher() {
        return new AntPathMatcher();
    }

    @Bean
    public UrlPathHelper urlPathHelper() {
        return new UrlPathHelper();
    }

    private Map<String, CorsConfiguration> getCorsConfigurations() {
        Map<String, CorsConfiguration> corsConfigurations = new HashMap<>();
        // Add any custom CORS configurations if needed
        return corsConfigurations;
    }

}