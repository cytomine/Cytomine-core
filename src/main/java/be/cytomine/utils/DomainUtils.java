package be.cytomine.utils;

import be.cytomine.domain.CytomineDomain;

import java.util.List;
import java.util.stream.Collectors;

public class DomainUtils {
    public static List<Long> extractIds(List<? extends CytomineDomain> domains) {
        return domains.stream().map(CytomineDomain::getId).collect(Collectors.toList());
    }
}
