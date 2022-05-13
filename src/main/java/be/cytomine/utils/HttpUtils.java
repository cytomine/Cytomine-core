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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class HttpUtils {

    public static String getContentFromUrl(String weburl) throws IOException {
//        Scanner s = new Scanner(new URL(url).openStream(), "UTF-8");
//        StringBuffer data = new StringBuffer();
//        while(s.hasNext()) {
//            data.append(s.next());
//        }
//        return data.toString();
        URL url = new URL(weburl);
        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();
        return new String(in.readAllBytes());
    }


}
