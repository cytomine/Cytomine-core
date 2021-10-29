package be.cytomine.service.image;

import be.cytomine.domain.image.AbstractImage;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ImagePropertiesService {


    static List<ImagePropertiesValue> KEYS = keys();
    
    
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

    //TODO: :-)

//    @Transactional
//    public void clear(AbstractImage image) {
//        List<String> propertyKeys = keys().stream().map(x -> x.getName()).collect(Collectors.toList());
//        Property.findAllByDomainIdentAndKeyInList(image.id, propertyKeys)?.each {
//            it.delete()
//        }
//    }
//
//    def populate(AbstractImage image) {
//        try {
//            def properties = imageServerService.properties(image)
//            properties.each {
//                String key = it?.key?.toString()?.trim()
//                String value = it?.value?.toString()?.trim()
//                if (key && value) {
//                    def property = Property.findByDomainIdentAndKey(image.id, key)
//                    if (!property) {
//                        log.info("New property: $key => $value for abstract image $image")
//                        property = new Property(key: key, value: value, domainIdent: image.id, domainClassName: image.class.name)
//                        property.save(failOnError: true)
//                    }
//                }
//            }
//        } catch(Exception e) {
//            log.error(e)
//        }
//    }
//
//    def extractUseful(AbstractImage image) {
//        keys().each { k, v ->
//                def property = Property.findByDomainIdentAndKey(image.id, v.name)
//            if (property)
//                image[k] = v.parser(property.value)
//            else
//                log.info "No property ${v.name} for abstract image $image"
//
//            image.save(flush: true, failOnError: true)
//        }
//    }
//
//    def regenerate(AbstractImage image) {
//        clear(image)
//        populate(image)
//        extractUseful(image)
//    }
}


@Data
@AllArgsConstructor
class ImagePropertiesValue {
    String key;
    String name;
    Function parser;
}