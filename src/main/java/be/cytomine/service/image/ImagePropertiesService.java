package be.cytomine.service.image;

import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.meta.Property;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.service.middleware.ImageServerService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class ImagePropertiesService {

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ImageServerService imageServerService;

    @Autowired
    EntityManager entityManager;

    public static List<ImagePropertiesValue> KEYS = keys();
    
    
    static List<ImagePropertiesValue> keys() {

        Function parseInt = x -> Integer.parseInt(x.toString());
        Function parseDouble = x -> Double.parseDouble(x.toString());
        Function parseString = x -> x;


        return List.of(
               new ImagePropertiesValue("width", "cytomine.width", parseInt),
                new ImagePropertiesValue("height", "cytomine.height", parseInt),
                new ImagePropertiesValue("depth", "cytomine.depth", parseInt),
                new ImagePropertiesValue("duration", "cytomine.duration", parseInt),
                new ImagePropertiesValue("channels", "cytomine.channels", parseInt),
                new ImagePropertiesValue("physicalSizeX", "cytomine.physicalSizeX", parseDouble),
                new ImagePropertiesValue("physicalSizeY", "cytomine.physicalSizeY", parseDouble),
                new ImagePropertiesValue("physicalSizeZ", "cytomine.physicalSizeZ", parseDouble),
                new ImagePropertiesValue("fps", "cytomine.fps", parseDouble),
                new ImagePropertiesValue("bitDepth", "cytomine.bitdepth", parseInt),
                new ImagePropertiesValue("colorspace", "cytomine.colorspace", parseString),
                new ImagePropertiesValue("magnification", "cytomine.magnification", parseInt)
            );
    }

    public void clear(AbstractImage image) {
        List<String> propertyKeys = keys().stream().map(x -> x.getName()).collect(Collectors.toList());
        propertyRepository.deleteAllByDomainIdentAndKeyIn(image.getId(), propertyKeys);
    }



    public void populate(AbstractImage image) throws IOException {
        Map<String, Object> properties = imageServerService.properties(image);
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey().trim();
            String value = entry.getValue()!=null? entry.getValue().toString().trim() : null;
            if (value!=null) {
                Optional<Property> optionalProperty = propertyRepository.findByDomainIdentAndKey(image.getId(), key);
                if (optionalProperty.isEmpty()) {
                    Property property = new Property();
                    property.setKey(key);
                    property.setValue(value);
                    property.setDomainIdent(image.getId());
                    property.setDomainClassName(image.getClass().getName());
                    propertyRepository.save(property);
                }
            }
        }
    }

    public void extractUseful(AbstractImage image) throws IOException, IllegalAccessException {
        for (ImagePropertiesValue key : keys()) {
            Property property = propertyRepository.findByDomainIdentAndKey(image.getId(), key.getName()).orElse(null);
            if (property!=null) {
                Field field = ReflectionUtils.findField(AbstractImage.class, key.getKey());
                field.setAccessible(true);
                field.set(image, key.getParser().apply(property.getValue()));
            } else {
                log.info("No property " + key.getName() + " for abstract image " + image.getId());
            }

        }
        entityManager.persist(image);
    }

    public void regenerate(AbstractImage image) throws IOException, IllegalAccessException {
        clear(image);
        populate(image);
        extractUseful(image);
    }
}


@Data
@AllArgsConstructor
class ImagePropertiesValue {
    String key;
    String name;
    Function parser;
}