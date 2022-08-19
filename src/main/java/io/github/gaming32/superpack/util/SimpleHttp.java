package io.github.gaming32.superpack.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class SimpleHttp {
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
}
