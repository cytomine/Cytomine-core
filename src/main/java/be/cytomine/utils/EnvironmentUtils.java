package be.cytomine.utils;

import org.springframework.core.env.Environment;

import java.util.Arrays;

public class EnvironmentUtils {

    public static boolean isTest(Environment environment) {
        return Arrays.asList(environment.getActiveProfiles()).contains("test");
    }

}
