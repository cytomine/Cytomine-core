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

public class AlgoAnnotationListing extends AnnotationListing {
    public AlgoAnnotationListing(EntityManager entityManager) {
        super(entityManager);
    }

    public String getDomainClass() {
        return "be.cytomine.domain.ontology.AlgoAnnotation";
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
        meta.put("nbComments", "a.count_comments");
        meta.put("countReviewedAnnotations", "a.count_reviewed_annotations");
        meta.put("reviewed", "(a.count_reviewed_annotations>0)");
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


        AvailableColumns term = new AvailableColumns();
        term.put("term", "aat.term_id");
        term.put("annotationTerms", "aat.id");
        term.put("userTerm", "aat.user_job_id");
        term.put("rate", "aat.rate");
        map.put("term", term);

        AvailableColumns track = new AvailableColumns();
        track.put("track", "atr.track_id");
        track.put("annotationTracks", "atr.id");
        map.put("track", track);

        AvailableColumns imageGroup = new AvailableColumns();
        imageGroup.put("imageGroup", "ig.group_id");
        map.put("imageGroup", imageGroup);

        AvailableColumns group = new AvailableColumns();
        group.put("group", "al.group_id");
        group.put("annotationLinks", "al.id");
        group.put("linkedAnnotations", "al.annotation_ident");
        group.put("linkedImages", "al.image_id");
        group.put("linkedUpdated", "al.updated");
        map.put("group", group);

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
        user.put("software", "s.name");
        user.put("job", "j.id");
        map.put("user", user);

        return map;
    }

    /**
     * Generate SQL string for FROM
     * FROM depends on data to print (if image name is aksed, need to join with imageinstance+abstractimage,...)
     */
    String getFrom() {
        String from = "FROM algo_annotation a ";
        String where = "WHERE true\n";

        if (tags != null) {
            from += " LEFT OUTER JOIN tag_domain_association tda ON a.id = tda.domain_ident AND tda.domain_class_name = '" + getDomainClass() + "' ";
        }

        if (multipleTerm) {
            from += "LEFT OUTER JOIN algo_annotation_term aat ON a.id = aat.annotation_ident ";
            from += "LEFT OUTER JOIN algo_annotation_term aat2 ON a.id = aat2.annotation_ident ";
            where += "AND aat.id <> aat2.id AND aat.term_id <> aat2.term_id AND aat.deleted IS NULL AND aat2.deleted IS NULL ";
        } else if ((noTerm || noAlgoTerm) && !(term != null || terms != null)) {
            from = from + " LEFT JOIN (SELECT * from algo_annotation_term x where true + " + (users != null ? "and x.user_job_id IN (" + joinValues(users) + ")" : "") + " and x.deleted IS NULL) aat ON a.id = aat.annotation_ident ";
            where = where + " AND (aat.id IS NULL OR aat.deleted IS NOT NULL) \n";

        } else if (columnsToPrint.contains("term")) {
            from += "LEFT JOIN algo_annotation_term aat ON a.id = aat.annotation_ident ";
            where += "AND aat.deleted IS NULL ";
        }

        if (columnsToPrint.contains("track")) {
            from += "LEFT OUTER JOIN annotation_track atr ON a.id = atr.annotation_ident ";
        }

        if (columnsToPrint.contains("imageGroup")) {
            from += "LEFT JOIN (SELECT * FROM image_group_image_instance WHERE deleted IS NULL) ig ON a.image_id = ig.image_id ";
        }

        if (columnsToPrint.contains("group") || annotationGroup != null || annotationGroups != null) {
            from += "LEFT OUTER JOIN (SELECT * FROM annotation_link WHERE deleted IS NULL) al1 ON al1.annotation_ident = a.id ";
            from += "LEFT OUTER JOIN (SELECT * FROM annotation_link WHERE deleted IS NULL) al ON al.group_id = al1.group_id ";
        }

        if (columnsToPrint.contains("image") || tracks != null || track != null) {
            from += "INNER JOIN image_instance ii ON a.image_id = ii.id INNER JOIN abstract_image ai ON ii.base_image_id = ai.id ";
        }

        if (columnsToPrint.contains("slice") || tracks != null || track != null) {
            from += "INNER JOIN slice_instance si ON a.slice_id = si.id INNER JOIN abstract_slice asl ON si.base_slice_id = asl.id ";
        }

        if (columnsToPrint.contains("user")) {
            // TODO: to refactor if job table is deleted
            from += "INNER JOIN sec_user u ON a.user_id = u.id INNER JOIN job j ON u.job_id = j.id INNER JOIN software s ON j.software_id = s.id ";
        }

        return from + "\n" + where;
    }

    String buildExtraRequest() {
        return "";
    }

    String getTermConst() {
        if (term != null) {
            addIfMissingColumn("term");
            return " AND ((aat.term_id = " + term + " AND aat.deleted IS NULL)" + ((noTerm) ? " OR aat.term_id IS NULL" : "") + ")\n";
        } else {
            return "";
        }
    }

    String getTermsConst() {
        if (terms != null) {
            addIfMissingColumn("term");
            return "AND ((aat.term_id IN (" + joinValues(terms) + ") AND aat.deleted IS NULL)" + ((noTerm) ? " OR aat.term_id IS NULL" : "") + ")\n";
        } else {
            return "";
        }
    }


    String getUserConst() {
        return (user != null ? "AND a.user_id = " + user + "\n" : "");
    }

    String getUsersConst() {
        return (users != null ? "AND a.user_id IN (" + joinValues(users) + ")\n" : "");
    }

    String getNotReviewedOnlyConst() {
        return (notReviewedOnly ? "AND a.count_reviewed_annotations=0\n" : "");
    }

    String createOrderBy() {
        if (kmeansValue < 3) return "";
        if (orderBy != null && !orderBy.isEmpty()) {
            return "ORDER BY " + orderBy.entrySet().stream().map(x -> x.getKey() + " " + x.getValue()).collect(Collectors.joining(", "));
        } else {
            String order = (track != null || tracks != null) ? "rank asc" : "a.id desc ";
            return "ORDER BY " + order + ((term != null || terms != null || columnsToPrint.contains("term")) ? ", aat.term_id " : "") + ((track != null || tracks != null || columnsToPrint.contains("track")) ? ", atr.track_id " : "");
        }
    }
}
