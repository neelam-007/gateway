/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;


/**
 * Adaptor providing a very simple interface to any generic HTTP client.
 */
public class SimpleHttpClient implements GenericHttpClient {
    private final GenericHttpClient client;

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

    public SimpleHttpClient(GenericHttpClient delegate) {
        if (delegate == null) throw new NullPointerException();
        this.client = delegate;
    }

    public GenericHttpRequest createRequest(GenericHttpMethod method, GenericHttpRequestParams params) throws GenericHttpException {
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
            request = client.createRequest(GenericHttpClient.GET, params);
            response = request.getResponse();
            byte[] bodyBytes = HexUtils.slurpStream(response.getInputStream());
            return new SimpleHttpResponseImpl(response, bodyBytes);
        } catch (IOException e) {
            throw new GenericHttpException(e);
        } finally {
            if (response != null) response.close();
            if (request != null) request.close();
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
        return client.createRequest(GenericHttpClient.POST, params);
    }

    private static class SimpleHttpResponseImpl extends GenericHttpResponseParamsImpl implements SimpleHttpResponse {
        private final byte[] bodyBytes;

        private SimpleHttpResponseImpl(GenericHttpResponseParams responseParams, byte[] bodyBytes) {
            super(responseParams);
            this.bodyBytes = bodyBytes != null ? bodyBytes : new byte[0];
        }
        public byte[] getBytes() {
            return bodyBytes;
        }
    }

    private static class SimpleXmlResponseImpl extends GenericHttpResponseParamsImpl implements SimpleXmlResponse {
        private Document bodyDoc;

        private SimpleXmlResponseImpl(GenericHttpResponseParams responseParams, Document bodyDoc) {
            super(responseParams);
            this.bodyDoc = bodyDoc;
        }

        public Document getDocument() {
            return bodyDoc;
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
            request = client.createRequest(GenericHttpClient.POST, params);
            HexUtils.copyStream(new ByteArrayInputStream(requestBody), request.getOutputStream());
            response = request.getResponse();
            byte[] bodyBytes = HexUtils.slurpStream(response.getInputStream());
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
        try {
            byte[] requestBody = XmlUtil.nodeToString(doc).getBytes(params.getContentType().getEncoding());
            request = client.createRequest(GenericHttpClient.POST, params);
            HexUtils.copyStream(new ByteArrayInputStream(requestBody), request.getOutputStream());
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
}
