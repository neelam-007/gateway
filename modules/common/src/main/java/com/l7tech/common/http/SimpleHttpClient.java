/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.ByteOrderMarkInputStream;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;


/**
 * Adaptor providing a very simple interface to any generic HTTP client.
 */
public class SimpleHttpClient implements GenericHttpClient {

    //- PUBLIC

    /**
     * Very simple interface for HTTP responses returned by the {@link SimpleHttpClient}.
     */
    public interface SimpleHttpResponse extends GenericHttpResponseParams {
        /**
         * Get the response body bytes.  Always succeeds.
         *
         * @return the response body bytes.  May be empty but never null.
         */
        byte[] getBytes();

        /**
         * Get the response body as a string.
         *
         * @return The response as a string.
         */
        String getString() throws IOException;
    }

    /**
     * Very simple interface for XML responses returned by the {@link SimpleHttpClient}.
     */
    public interface SimpleXmlResponse extends GenericHttpResponseParams {
        /**
         * Get the response as a DOM Document.  Always succeeds.
         *
         * @return the response Document.  Never null.
         */
        Document getDocument();
    }

    /**
     *
     */
    public SimpleHttpClient(GenericHttpClient delegate) {
        this( delegate, 0 );
    }

    /**
     *
     */
    public SimpleHttpClient(GenericHttpClient delegate, int maxContentLength ) {
        if (delegate == null) throw new NullPointerException();
        this.client = delegate;
        this.maxContentLength = maxContentLength;
    }

    /**
     *
     */
    @Override
    public GenericHttpRequest createRequest(HttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
        return client.createRequest(method, params);
    }

    /**
     * Simple one-step GET request.
     *
     * @param params the request parameters.  Must not be null.
     * @return the response.  Never null.
     * @throws GenericHttpException if there is a configuration, network or HTTP problem
     */
    public SimpleHttpResponse get(GenericHttpRequestParams params) throws GenericHttpException {
        GenericHttpRequest request = null;
        GenericHttpResponse response = null;
        try {
            request = createRequest(HttpMethod.GET, params);
            response = request.getResponse();
            byte[] bodyBytes = maxContentLength <= 0 ?
                    IOUtils.slurpStream(response.getInputStream()) :
                    IOUtils.slurpStream(response.getInputStream(), maxContentLength);
            return new SimpleHttpResponseImpl(response, bodyBytes);
        } catch (IOException e) {
            throw new GenericHttpException(e);
        } finally {
            if (response != null) response.close();
            if (request != null) request.close();
        }
    }

    /**
     * Even simpler one-step GET request.
     *
     * @param url The url to get.  Must not be null.
     * @return the response.  Never null.
     * @throws GenericHttpException if there is a configuration, network or HTTP problem
     */
    public SimpleHttpResponse get( final String url ) throws GenericHttpException {
        if ( url == null ) throw new GenericHttpException("Missing URL");
        try {
            return get( new GenericHttpRequestParams( new URL(url) ) );
        } catch (MalformedURLException e) {
            throw new GenericHttpException("Invalid URL '"+url+"'.");
        }
    }

    /**
     * Simple two-step POST request.  Caller must fill up the request input stream with post data, then
     * commit the request and get the response.  Both the request and the response should be closed as they
     * are no longer needed.
     *
     * @param params the request parameters.  Must not be null.
     * @return a generic HTTP request implementation ready to proceed with the POST.  Never null.
     * @throws GenericHttpException if there is a configuration, network or HTTP problem.
     */
    public GenericHttpRequest post(GenericHttpRequestParams params) throws GenericHttpException {
        return createRequest(HttpMethod.POST, params);
    }

    /**
     *
     */
    private static class SimpleHttpResponseImpl extends GenericHttpResponseParamsImpl implements SimpleHttpResponse {
        private final byte[] bodyBytes;

        private SimpleHttpResponseImpl(GenericHttpResponseParams responseParams, byte[] bodyBytes) {
            super(responseParams);
            this.bodyBytes = bodyBytes != null ? bodyBytes : new byte[0];
        }
        @Override
        public byte[] getBytes() {
            return bodyBytes;
        }

        @Override
        public String getString() throws IOException {
            final StringBuilder encodingBuilder = new StringBuilder();
            final ContentTypeHeader contentType = getContentType();
            if ( contentType != null ) {
                encodingBuilder.append(contentType.getEncoding());
            } else {
                encodingBuilder.append(ContentTypeHeader.DEFAULT_HTTP_ENCODING);
            }

            return new String(stripBom(getBytes(), encodingBuilder), encodingBuilder.toString());
        }

        private byte[] stripBom( final byte[] data, final StringBuilder encodingBuilder ) throws IOException {
            byte[] cleanData;

            ByteOrderMarkInputStream bomStream = null;
            try {
                bomStream = new ByteOrderMarkInputStream( new ByteArrayInputStream(data) );
                cleanData = IOUtils.slurpStream( bomStream );
                if ( bomStream.getEncoding() != null ) {
                    encodingBuilder.setLength( 0 );
                    encodingBuilder.append( bomStream.getEncoding() );
                }
            } finally {
                ResourceUtils.closeQuietly( bomStream );
            }

            return cleanData;
        }
    }

    /**
     * Post a request and obtain the response in a single step.
     *
     * @param params the request parameters.  Must not be null.
     * @param requestBody the byte array containing the request body.  May be empty but must not be null.
     * @return the response.  Never null.
     * @throws GenericHttpException if there is a network or HTTP problem
     */
    public SimpleHttpResponse post(GenericHttpRequestParams params, byte[] requestBody) throws GenericHttpException {
        if (params == null || requestBody == null) throw new NullPointerException();
        GenericHttpRequest request = null;
        GenericHttpResponse response = null;
        try {
            request = createRequest(HttpMethod.POST, params);
            request.setInputStream(new ByteArrayInputStream(requestBody));
            response = request.getResponse();
            byte[] bodyBytes = IOUtils.slurpStream(response.getInputStream());
            return new SimpleHttpResponseImpl(response, bodyBytes);
        } catch (IOException e) {
            throw new GenericHttpException(e);
        } finally {
            if (response != null) response.close();
            if (request != null) request.close();
        }
    }

    /**
     * Post an XML request and obtain the XML response in a single step.
     *
     * @param params the request parameters.  Must not be null.
     * @param doc the DOM tree of the XML request.  Must not be null.
     * @return the response.  Never null.
     * @throws GenericHttpException if there is a network or HTTP problem
     * @throws SAXException if the request can't be serialized
     * @throws SAXException if the response is not well-formed XML
     */
    public SimpleXmlResponse postXml(GenericHttpRequestParams params, Document doc) throws GenericHttpException, SAXException {
        if (params == null || doc == null) throw new NullPointerException();
        GenericHttpRequest request = null;
        GenericHttpResponse response = null;
        if (params.getContentType() == null)
            params.setContentType(ContentTypeHeader.XML_DEFAULT);
        try {
            byte[] requestBody = XmlUtil.nodeToString(doc).getBytes(params.getContentType().getEncoding());
            request = createRequest(HttpMethod.POST, params);
            request.setInputStream(new ByteArrayInputStream(requestBody));
            response = request.getResponse();
            final ContentTypeHeader contentType = response.getContentType();
            if (contentType == null || !contentType.isXml())
                throw new SAXException("Response from server was non-XML content type: " + contentType);
            Document responseDoc = XmlUtil.parse(response.getInputStream());
            return new SimpleXmlResponseImpl(response, responseDoc);
        } catch (IOException e) {
            throw new GenericHttpException(e);
        } finally {
            if (response != null) response.close();
            if (request != null) request.close();
        }
    }

    //- PRIVATE

    private final GenericHttpClient client;
    private final int maxContentLength;

    /**
     *
     */    
    private static class SimpleXmlResponseImpl extends GenericHttpResponseParamsImpl implements SimpleXmlResponse {
        private Document bodyDoc;

        private SimpleXmlResponseImpl(GenericHttpResponseParams responseParams, Document bodyDoc) {
            super(responseParams);
            this.bodyDoc = bodyDoc;
        }

        @Override
        public Document getDocument() {
            return bodyDoc;
        }
    }
}
