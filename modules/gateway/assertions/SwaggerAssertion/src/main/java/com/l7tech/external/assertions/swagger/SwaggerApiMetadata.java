package com.l7tech.external.assertions.swagger;

import java.io.Serializable;

/**
 * A simple holder for the basic metadata of a Swagger API.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SwaggerApiMetadata implements Serializable {

    private String title;
    private String host;
    private String basePath;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
