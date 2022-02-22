package be.cytomine.utils;

import liquibase.pro.packaged.T;
import org.springframework.data.domain.*;

import java.util.List;

public class PageUtils {

    public static <T> Page<T> buildPageFromPageResults(List<T> data, Long max, Long offset, Long total) {
        return new PageImpl<T>(data, new OffsetBasedPageRequest(offset, (max==0 ? Integer.MAX_VALUE : max.intValue()), Sort.unsorted()), total);
    }
}
