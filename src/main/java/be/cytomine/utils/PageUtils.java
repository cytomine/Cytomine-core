package be.cytomine.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class PageUtils {

    public static Pageable buildPage(Long offset, Long max) {
        if (offset == null || max == null) {
            return Pageable.unpaged();
        }
        return buildPage(offset.intValue(), max.intValue());
    }

    public static Pageable buildPage(Integer offset, Integer max) {
        if (offset == null || max == null || max == 0) {
            return Pageable.unpaged();
        }
        return PageRequest.of(offset, max);
    }
}
