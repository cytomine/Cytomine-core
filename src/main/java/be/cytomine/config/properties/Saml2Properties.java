package be.cytomine.config.properties;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Saml2Properties {
    private String attributeFile;
//  //  private Map<String, String> attributes = new HashMap<>();
//
//    public void setAttributeFile(String attributeFile) {
//        this.attributeFile = attributeFile;
//    }
//
//    public String getAttribute(String attributeName)  {
//        Properties properties = new Properties();
//        try (InputStream inputStream = getClass().getResourceAsStream(attributeFile)) {
//            properties.load(inputStream);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        return properties.getProperty(attributeName);
//    }
}
