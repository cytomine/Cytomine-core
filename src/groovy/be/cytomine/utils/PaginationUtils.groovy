package be.cytomine.utils

import be.cytomine.Exception.WrongArgumentException

class PaginationUtils {

    public static def convertListToPage(def list, String sortColumn, String sortDirection, Long maxItems, Long offset) {
        println "$list - $sortColumn - $sortDirection - $maxItems - $offset"
        def results = [:]

        def listSorted = sortByProperty(list, sortColumn, sortDirection)

        int max = (maxItems > 0) ? maxItems : Integer.MAX_VALUE

        results.collection = listSorted.subList(Math.min((int)offset, listSorted.size()), Math.min((int)offset+(int)max, listSorted.size()))
        results.size = listSorted.size()
        results.offset = offset
        results.perPage = Math.min(max, results.size)
        results.totalPages = Math.ceil(results.size / max)

        return results
    }

    public static def sortByProperty(def list, String sortColumn, String sortDirection) {
        if (sortDirection.equals('asc')) {
            list = list.sort { a,b -> a[sortColumn] <=> b[sortColumn] }
        } else if (sortDirection.equals('desc')) {
            list = list.sort { a,b -> b[sortColumn] <=> a[sortColumn] }
        } else {
            throw new WrongArgumentException("Cannot sort with direction ${sortDirection}")
        }
        return list
    }
}
