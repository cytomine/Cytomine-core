package be.cytomine.utils;

public class ClassUtils {

    /**
     * Get the class name of an object without package name
     * @param o Object
     * @return Class name (without package) of o
     */
    public static String getClassName(Object o) {
        String name = o.getClass().getName();   //be.cytomine.image.Image
        int exeed = name.indexOf("_\\$\\$_javassist"); //if  be.cytomine.image.Image_$$_javassistxxxx...remove all after  _$$
        if (exeed!=-1) {
            name = name.substring(0,exeed);
        }
        String[] array = name.split("\\.") ; //[be,cytomine,image,Image]
        //log.info array.length
        return array[array.length - 1]; // Image
    }
}
