package be.cytomine.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class HttpUtils {

    public static String getContentFromUrl(String url) throws IOException {
        Scanner s = new Scanner(new URL(url).openStream());
        StringBuffer data = new StringBuffer();
        while(s.hasNext()) {
            data.append(s.next());
        }
        return data.toString();
    }


}
