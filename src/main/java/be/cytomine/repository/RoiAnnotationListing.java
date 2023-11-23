package be.cytomine.repository;

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

import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class RoiAnnotationListing extends AnnotationListing {

    public RoiAnnotationListing(EntityManager entityManager) {
        super(entityManager);
    }

    public String getDomainClass() {
        return "be.cytomine.domain.processing.RoiAnnotation";
    }

    @Override
    LinkedHashMap<String, AvailableColumns> getAvailableColumn() {
        LinkedHashMap<String, AvailableColumns> map = new LinkedHashMap<>();

        AvailableColumns basic = new AvailableColumns();
        basic.put("id", "a.id");
        map.put("basic", basic);

        AvailableColumns meta = new AvailableColumns();
        meta.put("created", "extract(epoch from a.created)*1000");
        meta.put("updated", "extract(epoch from a.updated)*1000");

        meta.put("image", "a.image_id");
        meta.put("slice", "a.slice_id");
        meta.put("project", "a.project_id");
        meta.put("user", "a.user_id");

        meta.put("cropURL", "#cropURL");
        meta.put("smallCropURL", "#smallCropURL");
        meta.put("url", "#url");
        meta.put("imageURL", "#imageURL");
        map.put("meta", meta);

        AvailableColumns wkt = new AvailableColumns();
        wkt.put("location", "a.wkt_location");
        wkt.put("geometryCompression", "a.geometry_compression");
        map.put("wkt", wkt);


        AvailableColumns gis = new AvailableColumns();
        gis.put("area", "area");
        gis.put("areaUnit", "area_unit");
        gis.put("perimeter", "perimeter");
        gis.put("perimeterUnit", "perimeter_unit");
        gis.put("x", "ST_X(ST_centroid(a.location))");
        gis.put("y", "ST_Y(ST_centroid(a.location))");
        map.put("gis", gis);

        AvailableColumns image = new AvailableColumns();
        image.put("originalFilename", "ai.original_filename");
        image.put("instanceFilename", "COALESCE(ii.instance_filename, ai.original_filename)");
        map.put("image", image);

        AvailableColumns slice = new AvailableColumns();
        slice.put("channel", "asl.channel");
        slice.put("zStack", "asl.z_stack");
        slice.put("time", "asl.time");
        map.put("slice", slice);

        AvailableColumns user = new AvailableColumns();
        user.put("creator", "u.username");
        user.put("lastname", "u.lastname");
        user.put("firstname", "u.firstname");
        map.put("user", user);

        return map;
    }

    /**
     * Generate SQL string for FROM
     * FROM depends on data to print (if image name is aksed, need to join with imageinstance+abstractimage,...)
     */
    String getFrom() {

        String from = "FROM roi_annotation a ";
        String where = "WHERE true\n";

        if (columnsToPrint.contains("user")) {
            from += "INNER JOIN sec_user u ON a.user_id = u.id ";
        }

        if (columnsToPrint.contains("image")) {
            from += "INNER JOIN image_instance ii ON a.image_id = ii.id INNER JOIN abstract_image ai ON ii.base_image_id = ai.id ";
        }

        if (columnsToPrint.contains("slice")) {
            from += "INNER JOIN slice_instance si ON a.slice_id = si.id INNER JOIN abstract_slice asl ON si.base_slice_id = asl.id ";
        }

        if (tags != null) {
            from += " LEFT OUTER JOIN tag_domain_association tda ON a.id = tda.domain_ident AND tda.domain_class_name = '" + getDomainClass() + "' ";
        }

        return from + "\n" + where;
    }

    String createOrderBy() {
        if (kmeansValue < 3) {
            return "";
        }
        if (orderBy != null && !orderBy.isEmpty()) {
            return "ORDER BY " + orderBy.entrySet().stream().map(x -> x.getKey() + " " + x.getValue()).collect(Collectors.joining(", "));
        } else {
            return "ORDER BY a.id desc";
        }
    }

    String buildExtraRequest() {
        columnsToPrint.remove("term");
        return "";
    }

    String getNotReviewedOnlyConst() {
        return "";
    }

}
