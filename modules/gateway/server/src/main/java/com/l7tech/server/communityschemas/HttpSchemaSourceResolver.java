package com.l7tech.server.communityschemas;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpHeaders;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.url.AbstractUrlObjectCache;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationEvent;
import org.xml.sax.EntityResolver;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * SchemaSourceResolver for remote HTTP(S) schemas.
 *
 * <p>This source does not support resolution by target namespace.</p>
 */
public class HttpSchemaSourceResolver implements PostStartupApplicationListener, SchemaSourceResolver {

    //- PUBLIC

    public HttpSchemaSourceResolver( final SchemaConfiguration schemaConfiguration,
                                     final GenericHttpClientFactory httpClientFactory,
                                     final EntityResolver entityResolver ) {
        this.schemaConfiguration = schemaConfiguration;
        this.httpClientFactory = httpClientFactory;
        this.entityResolver = entityResolver;
        updateConfiguration();
    }

    @Override
    public String getId() {
        return "HTTP Resolver";
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public SchemaSource getSchemaByTargetNamespace( final Audit audit,
                                                    final String targetNamespace ) {
        return null;
    }

    @Override
    public SchemaSource getSchemaByUri( final Audit audit,
                                        final String uri ) throws IOException {
        String schemaDoc = null;

        if ( uri.startsWith( "http:" ) || uri.startsWith( "https:" ) ) {
            try {
                schemaDoc = httpStringCache.get().resolveUrl( audit, uri );
            } catch ( ParseException e ) {
                throw new CausedIOException("Unable to download remote schema " + uri +  " : " + ExceptionUtils.getMessage(e), e);
            }
        }

        return schemaDoc == null ? null : new DefaultSchemaSource( uri, schemaDoc, this );
    }

    @Override
    public void refreshSchemaByUri( final Audit audit,
                                    final String uri ) throws IOException {
        getSchemaByUri( audit, uri );
    }

    @Override
    public void registerInvalidationListener( final SchemaInvalidationListener listener ) {
        listenerRef.set( listener );
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof SchemaConfiguration.SchemaConfigurationReloadedEvent ) {
            updateConfiguration();
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( HttpSchemaSourceResolver.class.getName() );

    private final AtomicReference<SchemaInvalidationListener> listenerRef = new AtomicReference<SchemaInvalidationListener>();
    private final SchemaConfiguration schemaConfiguration;
    private final GenericHttpClientFactory httpClientFactory;
    private final EntityResolver entityResolver;

    /**
     * Shared cache for all remotely-loaded schema strings, system-wide.
     * This is a low-level cache that stores Strings, to save network calls.
     */
    private final AtomicReference<HttpObjectCache<String>> httpStringCache = new AtomicReference<HttpObjectCache<String>>();

    private void updateConfiguration() {
        final GenericHttpClientFactory hcf = wrapHttpClientFactory(httpClientFactory, schemaConfiguration.getMaxSchemaSize());

        final HttpObjectCache.UserObjectFactory<String> userObjectFactory = new HttpObjectCache.UserObjectFactory<String>() {
            @Override
            public String createUserObject(String url, AbstractUrlObjectCache.UserObjectSource responseSource) throws IOException {
                String response = responseSource.getString(true);
                onUrlDownloaded(url, response);
                return response;
            }
        };

        httpStringCache.set( new HttpObjectCache<String>(
                                                      "XML Schema",
                                                      schemaConfiguration.getMaxCacheEntries(),
                                                      schemaConfiguration.getMaxCacheAge(),
                                                      schemaConfiguration.getMaxStaleAge(),
                                                      hcf,
                                                      userObjectFactory,
                                                      HttpObjectCache.WAIT_LATEST,
                                                      ServerConfigParams.PARAM_SCHEMA_CACHE_MAX_SCHEMA_SIZE ));

    }

    /**
     * Report that a URL has just been downloaded.  If any previous schema with this URL was known, it and any
     * schemas that make use of it may need to be recompiled before their next use.
     *
     * @param url  the URL for the schema to invalidate.  Must not be null.
     * @param content the contents of the schema if available. May be null.
     */
    private void onUrlDownloaded( final String url,
                                  final String content ) {
        Exception schemaException = null;
        if ( content != null ) {
            try {
                XmlUtil.getSchemaTNS( url, content, entityResolver );
            } catch ( XmlUtil.BadSchemaException e ) {
                schemaException = e;
            }
        }

        final SchemaInvalidationListener listener = listenerRef.get();
        if ( listener!=null && !listener.invalidateSchemaByUri( url, schemaException==null ) ) {
            logger.warning("Invalid schema downloaded -- not invalidating cache for URL '" + url + "', error is: " + ExceptionUtils.getMessage(schemaException));
        }
    }

    private GenericHttpClientFactory wrapHttpClientFactory(final GenericHttpClientFactory httpClientFactory, final long maxResponseSize) {
        return new GenericHttpClientFactory() {
            @Override
            public GenericHttpClient createHttpClient() {
                return wrapHttpClient(httpClientFactory.createHttpClient());
            }

            @Override
            public GenericHttpClient createHttpClient(int hostConnections, int totalConnections, int connectTimeout, int timeout, Object identity) {
                return wrapHttpClient(httpClientFactory.createHttpClient(hostConnections, totalConnections, connectTimeout, timeout, identity));
            }

            private GenericHttpClient wrapHttpClient(final GenericHttpClient httpClient) {
                return new GenericHttpClient() {
                    @Override
                    public GenericHttpRequest createRequest( HttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
                        return wrapHttpRequest(httpClient.createRequest(method, params));
                    }
                };
            }

            private GenericHttpRequest wrapHttpRequest(final GenericHttpRequest request) {
                return new GenericHttpRequest() {
                    @Override
                    public void setInputStream( InputStream bodyInputStream) {
                        request.setInputStream(bodyInputStream);
                    }

                    @Override
                    public GenericHttpResponse getResponse() throws GenericHttpException {
                        return wrapHttpResponse(request.getResponse());
                    }

                    @Override
                    public void addParameters(List<String[]> parameters) throws IllegalArgumentException, IllegalStateException {
                        request.addParameters(parameters);
                    }

                    @Override
                    public void close() {
                        request.close();
                    }
                };
            }

            private GenericHttpResponse wrapHttpResponse(final GenericHttpResponse response) {
                return new GenericHttpResponse() {
                    @Override
                    public InputStream getInputStream() throws GenericHttpException {
                        return new ByteLimitInputStream(response.getInputStream(), 1024, maxResponseSize);
                    }

                    @Override
                    public void close() {
                        response.close();
                    }

                    @Override
                    public int getStatus() {
                        return response.getStatus();
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        return response.getHeaders();
                    }

                    @Override
                    public ContentTypeHeader getContentType() {
                        return response.getContentType();
                    }

                    @Override
                    public Long getContentLength() {
                        return response.getContentLength();
                    }
                };
            }
        };
    }
}
