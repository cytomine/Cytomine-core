package be.cytomine.service.image;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.domain.image.AbstractImage;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.meta.Property;
import be.cytomine.repository.image.AbstractImageRepository;
import be.cytomine.repository.image.AbstractSliceRepository;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.utils.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class ImagePropertiesService {

    private static Map<String, Integer> PIXEL_TYPE = Map.of("uint8", 8, "int8", 8, "uint16", 16, "int16", 16);

    @Autowired
    AbstractImageRepository abstractImageRepository;
    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    ImageServerService imageServerService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    AbstractSliceRepository abstractSliceRepository;

    public void clear(AbstractImage image) {
        propertyRepository.deleteAllByDomainIdent(image.getId());
    }


    public void populate(AbstractImage image) throws IOException {
        List<Map<String, Object>> properties = imageServerService.rawProperties(image);

        for (Map<String, Object> it : properties) {
            String namespace = it.get("namespace")!=null? (String)it.get("namespace") + ".": "";
            String key = it.get("key")!=null? namespace + (String)it.get("key").toString().trim() : null;
            String value = it.get("value")!=null? (String)it.get("value").toString().trim() : null;

            if (key!=null && value!=null) {
                key = key.replaceAll("\u0000", "");
                value = value.replaceAll("\u0000", "");
                log.debug("Read property: {} => {} for abstract image {}", key, value, image);
                Optional<Property> optionalProperty = propertyRepository.findByDomainIdentAndKey(image.getId(), key);
                if (optionalProperty.isEmpty()) {
                    log.debug("New property: {} => {} for abstract image {}", key, value, image);
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
        extractUseful(image, false);
    }

    public void extractUseful(AbstractImage image, boolean deep) throws IOException, IllegalAccessException {

        try {
            Map<String, Object> properties = imageServerService.properties(image);
            Map<String, Object> imageProperties = (Map<String, Object>)properties.getOrDefault("image", Map.of());
            JsonObject imagePropertiesObject = new JsonObject(imageProperties);


            image.setWidth(imagePropertiesObject.getJSONAttrInteger("width"));
            image.setHeight(imagePropertiesObject.getJSONAttrInteger("height"));
            image.setDepth(imagePropertiesObject.getJSONAttrInteger("depth"));
            image.setDuration(imagePropertiesObject.getJSONAttrInteger("duration"));

            image.setChannels(imagePropertiesObject.getJSONAttrInteger("n_intrinsic_channels"));
            image.setPhysicalSizeX(imagePropertiesObject.getJSONAttrDouble("physical_size_x"));
            image.setPhysicalSizeY(imagePropertiesObject.getJSONAttrDouble("physical_size_y"));
            image.setPhysicalSizeZ(imagePropertiesObject.getJSONAttrDouble("physical_size_z"));

            image.setFps(imagePropertiesObject.getJSONAttrDouble("frame_rate"));

            Map<String, Object> instrument = ((Map<String, Object>)properties.getOrDefault("instrument", Map.of()));
            Map<String, Object> objective = ((Map<String, Object>)instrument.getOrDefault("objective", Map.of()));
            Integer nominal_magnification = (Integer)objective.get("nominal_magnification");

            image.setMagnification(nominal_magnification);

            abstractImageRepository.save(image);

            if (deep) {
                List<Map<String, Object>> channels = (List<Map<String, Object>>)properties.getOrDefault("channels", List.of());
                for (Map<String, Object> channel : channels) {
                    List<AbstractSlice> slices = abstractSliceRepository.findAllByImage(image)
                            .stream().filter(x -> Objects.equals(x.getChannel(), (Integer)channel.get("index"))).toList();
                    for (AbstractSlice slice : slices) {
                        slice.setChannelName((String) channel.get("suggested_name"));
                        slice.setChannelColor((String) channel.get("color"));
                    }
                    abstractSliceRepository.saveAll(slices);
                }
            }
        } catch (Exception e) {
            log.error("Error when extracting metadata", e);
        }
    }

    public void regenerate(AbstractImage image, boolean deep) throws IOException, IllegalAccessException {
        clear(image);
        populate(image);
        extractUseful(image, deep);
    }
}

@Getter
@Setter
@AllArgsConstructor
class ImagePropertiesValue {
    String key;
    String name;
    Function parser;
}
