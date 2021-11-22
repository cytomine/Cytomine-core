package be.cytomine.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class HttpUtils {

    public static String getContentFromUrl(String url) throws IOException {
        Scanner s = new Scanner(new URL(url).openStream());
        String data = s.toString();
        return data;
    }
}
