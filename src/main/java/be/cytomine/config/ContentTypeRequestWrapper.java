package be.cytomine.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * The cytomine java client does not set any content-type header.
 * It seems that by default, spring receives 'application/octet-stream' which is not compatible to automatically parse JSON data.
 * So if the request has a ".json" string in its path AND if the header is null or equal to application/octet-stream, then we return a application/json contenttype
 */
public class ContentTypeRequestWrapper extends HttpServletRequestWrapper {

    public ContentTypeRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getContentType() {
        return super.getContentType();
    }

    @Override
    public String getHeader(String name) {
        try {
            if (name.equalsIgnoreCase("content-type")
                    && (super.getHeader(name)==null || super.getHeader(name).equalsIgnoreCase("application/octet-stream")) &&
                    super.getRequestURL().toString().contains(".json")) {
                return "application/json";
            }
        } catch (Exception ignored) {

        }

        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> headerNames = Collections.list(super.getHeaderNames());
        if (!headerNames.contains("content-type")) {
            headerNames.add("content-type");
            return Collections.enumeration(headerNames);
        }

        return super.getHeaderNames();
    }

    @Override
    public Enumeration <String> getHeaders(String name) {
        try {
            if (name.equalsIgnoreCase("content-type")
                    && (super.getHeader(name)==null || super.getHeader(name).equalsIgnoreCase("application/octet-stream")) &&
                    super.getRequestURL().toString().contains(".json")) {
                return Collections.enumeration(Collections.singletonList("application/json"));
            }
        } catch (Exception ignored) {

        }
        return super.getHeaders(name);

    }

}
