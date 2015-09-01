package com.l7tech.external.assertions.odata.server;

import com.sun.jersey.api.uri.UriBuilderImpl;
import org.odata4j.producer.resources.HeaderMap;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Our custom JAX-RS UriInfo implementation
 *
 * @author rraquepo, 8/22/13
 */
public class JaxRsUriInfo implements UriInfo {

    private URI uri;
    private UriBuilder uriBuilder;
    private String queryOptions;
    private String path;

    public JaxRsUriInfo(String uri, String queryOptions) throws URISyntaxException {
        this.uri = new URI(uri);
        this.queryOptions = queryOptions;
        uriBuilder = new UriBuilderImpl();
        uriBuilder = UriBuilderImpl.fromUri(uri);
        uriBuilder.replaceQuery(queryOptions);
    }

    public String getQueryOptions() {
        return queryOptions;
    }

    public void setQueryOptions(String queryOptions) {
        this.queryOptions = queryOptions;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getPath(boolean b) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<PathSegment> getPathSegments(boolean b) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public URI getRequestUri() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return uriBuilder;
    }

    @Override
    public URI getAbsolutePath() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public URI getBaseUri() {
        return uri;
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean b) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        HeaderMap map = new HeaderMap();
        if (queryOptions != null && !"".equals(queryOptions)) {
            String[] parameters = queryOptions.split("&");
            for (String param : parameters) {
                String[] data = param.split("=");
                String key = data[0], value = null;
                if (data.length > 1) {
                    value = data[1];
                }
                map.add(key, value);
            }
        }
        return map;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean b) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getMatchedURIs() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getMatchedURIs(boolean b) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<Object> getMatchedResources() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
