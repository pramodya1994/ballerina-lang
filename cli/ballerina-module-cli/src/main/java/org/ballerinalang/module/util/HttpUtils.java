package org.ballerinalang.module.util;

import org.ballerinalang.module.exception.ModuleCliException;
import org.ballerinalang.net.http.HttpConstants;
import org.wso2.transport.http.netty.contract.Constants;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {

    private static final int DEFAULT_HTTPS_PORT = 443;
    private static final int DEFAULT_HTTP_PORT = 80;

    private HttpUtils() {}

    /**
     * Method is responsible for separate publisher url to host,port and context.
     *
     * @param publisherURL the publisher url.
     * @return map that contains the host,port,context and complete url.
     */
    public static Map<String, String> getURLProperties(String publisherURL) {
        Map<String, String> httpStaticProperties;
        try {
            URL url = new URL(publisherURL);
            httpStaticProperties = new HashMap<>();
            httpStaticProperties.put(Constants.TO, url.getFile());
            String protocol = url.getProtocol();
            String path = url.getPath();
            httpStaticProperties.put(Constants.PROTOCOL, protocol);
            httpStaticProperties.put(Constants.HTTP_HOST, url.getHost());
            httpStaticProperties.put(HttpConstants.PATH, path);
            int port;
            if (Constants.HTTPS_SCHEME.equalsIgnoreCase(protocol)) {
                port = url.getPort() != -1 ? url.getPort() : DEFAULT_HTTPS_PORT;
            } else {
                port = url.getPort() != -1 ? url.getPort() : DEFAULT_HTTP_PORT;
            }
            httpStaticProperties.put(Constants.HTTP_PORT, Integer.toString(port));
            httpStaticProperties.put("REQUEST_URL", url.toString());
        } catch (MalformedURLException e) {
            throw ModuleCliUtil.createModuleCliException("malformed URL: " + publisherURL);
        }
        return httpStaticProperties;
    }
}
