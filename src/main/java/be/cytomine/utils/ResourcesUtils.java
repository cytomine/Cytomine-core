package be.cytomine.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class ResourcesUtils {

    @Qualifier("resourceLoader")
    private  static ResourceLoader resourceLoader;

    private static Environment environment;

    public ResourcesUtils(ResourceLoader resourceLoader, Environment environment) {
        this.resourceLoader = resourceLoader;
        this.environment = environment;
    }

    /** Loads the key values properties from resources file in a Map
     *
     * @return
     */
    public static Map<String, String> getPropertiesMap()  {

        Map<String, String> resourcePropertiesMap = new HashMap<String, String>();
        String propertiesFileUri = Optional.ofNullable(environment.getProperty("METADATA_PREFIXES_FILE_URI"))
                .orElse("classpath:metaPrefix.properties");
        try {
            Resource resourceProperties=resourceLoader.getResource(propertiesFileUri);
            Properties properties = new Properties();
            properties.load(resourceProperties.getInputStream());
            for (String key : properties.stringPropertyNames()) {
                resourcePropertiesMap.put(key, properties.get(key).toString());
            }
        }catch (IOException e){
            log.error("Failed to load resources from path "+ propertiesFileUri, e);
        }
        return resourcePropertiesMap;

    }

    /** Loads the values of properties from resources file in a List
     *
     * @return
     */
    public static List<String> getPropertiesValuesList( )  {
        return new ArrayList<String>(getPropertiesMap().values());
    }

}
