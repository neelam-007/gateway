package com.l7tech.external.assertions.js.features.bindings.views;

import com.l7tech.external.assertions.js.features.ScriptMalformedUrlException;
import com.l7tech.external.assertions.js.features.bindings.JavaScriptHttpRequestMessage;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Provides URL view of the HTTP Request message. Provides access to URL components of the HTTP Request message. Can be
 * accessed using content.getVariable('request:url');
 */
@SuppressWarnings("unused")
public class UrlView extends JavaScriptMessageView {

    private final String protocol;
    private final String host;
    private final int port;
    private final String path;
    private final String queryString;
    private final Object query;

    public UrlView(final JavaScriptHttpRequestMessage javaScriptMessage) {
        super(javaScriptMessage);
        final URL url = getUrl(javaScriptMessage);

        this.host = url.getHost();
        this.protocol = url.getProtocol();
        this.port = url.getPort();
        this.path = url.getPath();

        this.queryString = url.getQuery();
        this.query = javaScriptMessage.getParameters();
    }

    @NotNull
    private URL getUrl(final JavaScriptHttpRequestMessage javaScriptMessage) {
        try {
            return new URL(javaScriptMessage.getUrl());
        } catch (MalformedURLException e) {
            throw new ScriptMalformedUrlException("Malformed URL in the JavaScriptHttpRequestMessage.", e);
        }
    }

    /**
     * Gets the Protocol (http or https)
     * @return protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Gets the host in the URL.
     * @return host
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the port in the URL
     * @return port
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the path in the URL
     * @return path
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the query string in the URL
     * @return queryString
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * Gets the query params in the URL as a JavaScript JSON object
     * @return path
     */
    public Object getQuery() {
        return query;
    }

    @Override
    protected void flush() {
        // Nothing to write to context in this View.
    }
}
