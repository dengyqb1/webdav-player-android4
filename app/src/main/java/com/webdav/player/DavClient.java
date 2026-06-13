package com.webdav.player;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Simple WebDAV client using only Android built-in HTTP APIs.
 * Works on API 15+ with no external dependencies.
 */
public class DavClient {

    private final String baseUrl;
    private final String username;
    private final String password;

    public DavClient(String url, String username, String password) {
        this.baseUrl = url.endsWith("/") ? url : url + "/";
        this.username = username != null ? username : "";
        this.password = password != null ? password : "";
    }

    /**
     * List directory contents via WebDAV PROPFIND.
     */
    public List<DavEntry> list(String path) throws Exception {
        String url = buildUrl(path);
        HttpURLConnection conn = openConnection(url);
        try {
            conn.setRequestMethod("PROPFIND");
            conn.setRequestProperty("Depth", "1");
            conn.setRequestProperty("Content-Type", "application/xml");
            String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                    + "<propfind xmlns=\"DAV:\">"
                    + "<prop><displayname/><resourcetype/><getcontentlength/></prop>"
                    + "</propfind>";
            conn.setDoOutput(true);
            conn.getOutputStream().write(body.getBytes("UTF-8"));
            conn.getOutputStream().flush();

            int code = conn.getResponseCode();
            if (code == 207) {
                return parsePropfind(conn.getInputStream(), baseUrl);
            } else {
                throw new Exception("HTTP " + code);
            }
        } finally {
            conn.disconnect();
        }
    }

    public String getDownloadUrl(String filePath) {
        return buildUrl(filePath);
    }

    private String buildUrl(String path) {
        String p = path;
        if (p.startsWith("/")) p = p.substring(1);
        return baseUrl + p;
    }

    private HttpURLConnection openConnection(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (username.length() > 0) {
            String cred = username + ":" + password;
            String basicAuth = "Basic " + Base64.encode(cred.getBytes("UTF-8"));
            conn.setRequestProperty("Authorization", basicAuth);
        }
        return conn;
    }

    private List<DavEntry> parsePropfind(InputStream in, String base) throws Exception {
        List<DavEntry> entries = new ArrayList<DavEntry>();

        // Simple XML parsing without pulling in a library
        byte[] data = readAll(in);
        String xml = new String(data, "UTF-8");

        // Split on <response> tags
        int idx = 0;
        while (true) {
            int start = xml.indexOf("<response>", idx);
            if (start < 0) {
                start = xml.indexOf("<d:response>", idx);
            }
            if (start < 0) break;

            int end = xml.indexOf("</response>", start);
            if (end < 0) {
                end = xml.indexOf("</d:response>", start);
            }
            if (end < 0) break;
            end += "</d:response>".length();

            String resp = xml.substring(start, end);
            idx = end;

            // Extract href
            String href = extractXmlValue(resp, "href", "d:href");
            if (href == null || href.isEmpty()) continue;

            // Normalize path
            href = href.replaceAll("https?://[^/]+", "");
            if (href.endsWith("/")) href = href.substring(0, href.length() - 1);
            if (!href.startsWith("/")) href = "/" + href;

            String name;
            int lastSlash = href.lastIndexOf('/');
            name = lastSlash >= 0 ? href.substring(lastSlash + 1) : href;

            // Skip root
            if (name.isEmpty() || href.equals("/")) continue;

            boolean isDir = resp.contains("<collection") || resp.contains("<d:collection");
            long size = 0;
            String sizeStr = extractXmlValue(resp, "getcontentlength", "d:getcontentlength");
            if (sizeStr != null && !sizeStr.isEmpty()) {
                try {
                    size = Long.parseLong(sizeStr);
                } catch (NumberFormatException ignore) {}
            }

            entries.add(new DavEntry(name, href, isDir, size));
        }

        return entries;
    }

    private String extractXmlValue(String xml, String... tags) {
        for (String tag : tags) {
            String open = "<" + tag + ">";
            String openNs = "<d:" + tag + ">";
            int s = xml.indexOf(open);
            if (s < 0) s = xml.indexOf(openNs);
            if (s >= 0) {
                int e = xml.indexOf("</", s + 1);
                if (e > s) {
                    int valueStart = xml.indexOf('>', s) + 1;
                    if (valueStart < e) {
                        return xml.substring(valueStart, e).trim();
                    }
                }
            }
        }
        return null;
    }

    private byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = in.read(tmp)) >= 0) {
            buf.write(tmp, 0, n);
        }
        return buf.toByteArray();
    }

    public static class DavEntry {
        public final String name;
        public final String path;
        public final boolean isDir;
        public final long size;

        DavEntry(String name, String path, boolean isDir, long size) {
            this.name = name;
            this.path = path;
            this.isDir = isDir;
            this.size = size;
        }
    }

    /**
     * Minimal Base64 for API 15 (android.util.Base64 is API 8+, but this avoids any confusion).
     */
    private static class Base64 {
        private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

        static String encode(byte[] data) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < data.length) {
                int b0 = data[i++] & 0xff;
                int b1 = (i < data.length) ? data[i++] & 0xff : 0;
                int b2 = (i < data.length) ? data[i++] & 0xff : 0;
                sb.append(CHARS.charAt(b0 >> 2));
                sb.append(CHARS.charAt(((b0 & 3) << 4) | (b1 >> 4)));
                sb.append(i > data.length + 1 ? '=' : CHARS.charAt(((b1 & 15) << 2) | (b2 >> 6)));
                sb.append(i > data.length ? '=' : CHARS.charAt(b2 & 63));
            }
            return sb.toString();
        }
    }
}
