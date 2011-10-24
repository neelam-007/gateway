package com.l7tech.external.assertions.icapantivirusscanner.server;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import java.io.InputStream;


/**
 * <p>A wrapped {@link org.jboss.netty.handler.codec.http.DefaultHttpResponse} to allow for streaming of http response.
 * </p>
 *
 * @author Ken Diep
 */
public class StreamedHttpResponse extends DefaultHttpResponse {

    private InputStream inputStream;

    public StreamedHttpResponse(HttpVersion version, HttpResponseStatus status) {
        super(version, status);
    }

    public void setContent(final InputStream inputStream){
        this.inputStream = inputStream;
    }

    public InputStream getContentAsStream(){
        return inputStream;
    }
}
