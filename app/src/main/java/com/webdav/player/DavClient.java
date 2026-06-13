package com.webdav.player;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

import java.util.List;

public class DavClient {

    private Sardine sardine;
    private String baseUrl;

    public DavClient(String url, String username, String password) {
        this.baseUrl = url;
        if (username != null && username.length() > 0) {
            this.sardine = SardineFactory.begin(username, password);
        } else {
            this.sardine = SardineFactory.begin();
        }
    }

    public DavClient(String url) {
        this(url, null, null);
    }

    public List<com.github.sardine.DavResource> list(String path) throws Exception {
        String fullUrl = buildUrl(path);
        return sardine.list(fullUrl);
    }

    public String getDownloadUrl(String filePath) {
        return buildUrl(filePath);
    }

    private String buildUrl(String path) {
        String base = baseUrl;
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return base + path;
    }
}
