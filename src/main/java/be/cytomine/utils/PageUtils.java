package be.cytomine.utils;

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

import liquibase.pro.packaged.T;
import org.springframework.data.domain.*;

import java.util.ArrayList;
import java.util.List;

public class PageUtils {

    public static <T> Page<T> buildPageFromPageResults(List<T> data, Long max, Long offset, Long total) {
        return new PageImpl<T>(data, new OffsetBasedPageRequest(offset, (max==0 ? Integer.MAX_VALUE : max.intValue()), Sort.unsorted()), total);
    }
    public static <T> Page<T> buildPageFromListResults(List<T> data, Long maxParameter, Long offsetParameter) {
        Long offset = offsetParameter != null ? offsetParameter : 0;
        Long max = (maxParameter != null && maxParameter!=0) ? maxParameter : Integer.MAX_VALUE;
        List subList;
        if (offset >= data.size()) {
            subList = new ArrayList();
        } else {
            int maxForCollection = (int)Math.min(data.size() - offset, max);
            subList = data.subList(offset.intValue(),offset.intValue() + maxForCollection);
        }
        return new PageImpl<T>(subList, new OffsetBasedPageRequest(offset, max.intValue(), Sort.unsorted()), data.size());
    }
}
