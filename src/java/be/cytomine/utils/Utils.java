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

import org.joda.time.DateTime;

import java.util.*;
import org.joda.time.DateTime;

public class Utils {

    /**
     * Add seconds to the actual date
     * @param secondes Number of seconds to add
     * @return Date equal to now + secondes
     */
    public static Date getDatePlusSecond(int secondes) {
        return new DateTime().plusSeconds(secondes).toDate();
    }

    /**
     * Substract seconds to the actual date
     * @param secondes Number of seconds to substract
     * @return Date equal to now - secondes
     */
    public static Date getDateMinusSecond(int secondes) {
        return new DateTime().minusSeconds(secondes).toDate();
    }

    /**
     * Comparator method allowing to sort a TreeMap by its values desc (not by its keys asc)
     */
    public static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValuesDesc(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
                new Comparator<Map.Entry<K, V>>() {
                    public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                        return e2.getValue().compareTo(e1.getValue());
                    }
                });
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
}
