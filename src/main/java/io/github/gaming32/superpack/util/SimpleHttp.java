package io.github.gaming32.superpack.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.gaming32.superpack.Superpack;

public final class SimpleHttp {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHttp.class);

    public static final String USER_AGENT;

    static {
        final Package specPackage = Superpack.class.getPackage();
        String userAgent = specPackage.getSpecificationTitle() + '/' + specPackage.getSpecificationVersion();
        if (userAgent.equals("null/null")) {
            userAgent = Superpack.class.getSimpleName() + "/DEBUG";
        }
        USER_AGENT = userAgent;
    }

    private SimpleHttp() {
    }

    public static URL createUrl(String base, String route, Map<String, Object> queryParams) throws MalformedURLException {
        final StringBuilder url = new StringBuilder(base);
        if (!base.endsWith("/")) {
            url.append('/');
        }
        if (!route.startsWith("/")) {
            url.append(route);
        } else {
            url.append(route, 1, route.length());
        }
        return buildUrl(url, queryParams);
    }

    public static URL createUrl(String url, Map<String, Object> queryParams) throws MalformedURLException {
        return buildUrl(new StringBuilder(url), queryParams);
    }

    private static URL buildUrl(StringBuilder url, Map<String, ? extends Object> queryParams) throws MalformedURLException {
        if (!queryParams.isEmpty()) {
            url.append('?');
            for (final var entry : queryParams.entrySet()) {
                url.append(entry.getKey());
                url.append('=');
                url.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
                url.append('&');
            }
            url.setLength(url.length() - 1);
        }
        return new URL(url.toString());
    }

    public static URLConnection request(URL url) throws IOException {
        LOGGER.info("Requesting {}", url);
        final URLConnection cnxn = url.openConnection();
        cnxn.setRequestProperty("User-Agent", USER_AGENT);
        return cnxn;
    }

    public static InputStream stream(URL url) throws IOException {
        return request(url).getInputStream();
    }
}
