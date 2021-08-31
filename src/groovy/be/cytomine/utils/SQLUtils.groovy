package be.cytomine.utils

class SQLUtils {

    static String toCamelCase( String text, boolean capitalized = false ) {
        text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
        return capitalized ? capitalize(text) : text
    }

    static def keysToCamelCase(def m) {
        def newMap = [:]
        m.each { k, v ->
            newMap[toCamelCase(k)] = v
        }
        return newMap
    }

    static String toSnakeCase(String text) {
        return text.replaceAll("(.)(\\p{Upper})", { Object[] it -> "${it[1]}_${it[2]}"}).toLowerCase();
    }
}
