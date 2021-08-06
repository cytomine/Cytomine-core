package be.cytomine.utils

import be.cytomine.Exception.WrongArgumentException

class PaginationUtils {

    public static convertListToPage(def list, String sortColumn, String sortDirection, Long maxItems, Long offset) {
        println "$list - $sortColumn - $sortDirection - $maxItems - $offset"
        def results = [:]

        if (sortDirection.equals('asc')) {
            list = list.sort { a,b -> a[sortColumn] <=> b[sortColumn] }
        } else if (sortDirection.equals('desc')) {
            list = list.sort { a,b -> b[sortColumn] <=> a[sortColumn] }
        } else {
            throw new WrongArgumentException("Cannot sort with direction ${sortDirection}")
        }

        int max = (maxItems > 0) ? maxItems : Integer.MAX_VALUE

        results.collection = list.subList(Math.min((int)offset, list.size()), Math.min((int)offset+(int)max, list.size()))
        results.size = list.size()
        results.offset = offset
        results.perPage = Math.min(max, results.size)
        results.totalPages = Math.ceil(results.size / max)

        return results
        //         responseSuccess([collection : results.data, size:results.total, offset: results.offset, perPage: results.perPage, totalPages: results.totalPages])
    }
}
