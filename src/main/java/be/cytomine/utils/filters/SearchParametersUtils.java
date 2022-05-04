package be.cytomine.utils.filters;

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

import be.cytomine.api.controller.utils.RequestParams;
import org.apache.commons.lang3.EnumUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SearchParametersUtils {


    private static String SEARCH_PARAM_EQUALS = "equals";
    private static String SEARCH_PARAM_LIKE = "like";
    private static String SEARCH_PARAM_ILIKE = "ilike";
    private static String SEARCH_PARAM_IN = "in";

    static List<String> equalsOperators = List.of(SEARCH_PARAM_EQUALS);
    static List<String> likeOperators = List.of(SEARCH_PARAM_LIKE);
    static List<String> ilikeOperators = List.of(SEARCH_PARAM_ILIKE);
    static List<String> equalsAndLikeOperators = List.of(SEARCH_PARAM_EQUALS, SEARCH_PARAM_LIKE);
    static List<String> equalsAndIlikeOperators = List.of(SEARCH_PARAM_EQUALS, SEARCH_PARAM_ILIKE);
    static List<String> likeAndIlikeOperators = List.of(SEARCH_PARAM_LIKE, SEARCH_PARAM_ILIKE);
    static List<String> equalsAndLikeAndIlikeOperators = List.of(SEARCH_PARAM_EQUALS, SEARCH_PARAM_LIKE, SEARCH_PARAM_ILIKE);

    private static final String paramRegex = ".+\\[.+\\]";


    public static List<SearchParameterEntry> getSearchParameters(RequestParams requestParams) {
        List<SearchParameterEntry> searchParameterEntries = new ArrayList<>();
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            if (entry.getKey().matches(paramRegex)) {
                String[] tmp = entry.getKey().split("\\[");
                String operator = tmp[1].substring(0,tmp[1].length()-1);
                String field = tmp[0];

                Object values = entry.getValue();
                if(operator.equals(SEARCH_PARAM_IN)) {
                    if(values.toString().contains(",")) {
                        values = new ArrayList<String>(Arrays.asList(values.toString().split(",")));
                    }
                } else if(operator.equals(SEARCH_PARAM_LIKE) || operator.equals(SEARCH_PARAM_ILIKE)) {
                    values = values.toString().replaceAll("\\*","%");
                    if (values.toString().startsWith("%")) {
                        values = "%"+ values + "%";
                    }
                } else

                if(values instanceof List) {
                    values = ((ArrayList<?>) values).stream().map(x -> URLDecoder.decode(x.toString(), StandardCharsets.UTF_8)).collect(Collectors.toList());
                } else {
                    values = URLDecoder.decode(values.toString(), StandardCharsets.UTF_8);
                }

//                String sqlOperator = (operator.equals(SEARCH_PARAM_EQUALS)) ? "=" : operator;

                if(EnumUtils.isValidEnum(SearchOperation.class, operator)) {
                    SearchParameterEntry searchParameterEntry =
                            new SearchParameterEntry(field, SearchOperation.valueOf(operator), values);
//                    searchParameterEntry.set(sqlOperator);
                    searchParameterEntries.add(searchParameterEntry);
                }
            }
        }
        return searchParameterEntries;
    }
}
