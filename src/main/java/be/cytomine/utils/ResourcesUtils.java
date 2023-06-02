package be.cytomine.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.*;

@Slf4j
public class ResourcesUtils {

    /** Loads the key values properties from resources file in a Map
     *
     * @param resourceLoader
     * @param path
     * @return
     */
    public static Map<String, String> getPropertiesMap(ResourceLoader resourceLoader, String path)  {
        Resource resourceProperties=resourceLoader.getResource(path);
        Properties properties = new Properties();
        Map<String, String> resourcePropertiesMap = new HashMap<String, String>();
        try {
            properties.load(resourceProperties.getInputStream());
            for (String key : properties.stringPropertyNames()) {
                resourcePropertiesMap.put(key, properties.get(key).toString());
            }
        }catch (IOException e){
            log.error("Failed to load resources from path "+ path, e);
        }
        return resourcePropertiesMap;

    }

    /** Loads the values of properties from resources file in a List
     *
     * @param resourceLoader
     * @param path
     * @return
     */
    public static List<String> getPropertiesValuesList(ResourceLoader resourceLoader, String path)  {
        Resource resourceProperties=resourceLoader.getResource(path);
        Properties properties = new Properties();
        List<String> resourcePropertiesList = new ArrayList<>();
        try {
            properties.load(resourceProperties.getInputStream());
            for (String key : properties.stringPropertyNames()) {
                resourcePropertiesList.add( properties.get(key).toString());
            }
        }catch (IOException e){
            log.error("Failed to load resources from path "+ path, e);
        }
        return resourcePropertiesList;

    }

}
