/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.skunkworks;

import com.l7tech.message.*;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.test.BenchmarkRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.InputStream;

/**
 * Test performance of Message.getKnob().
 */
public class GetKnobTest {
    public static void main(String[] args) throws Exception {

        final long[] resultthing = new long[] { 0 };

        BenchmarkRunner test = new BenchmarkRunner(new Runnable() {
            public void run() {
                try {
                    final InputStream reqStream = null;//TestDocuments.getTestDocumentURL(TestDocuments.PLACEORDER_CLEARTEXT).openStream();
                    final InputStream respStream = null;//estDocuments.getTestDocumentURL(TestDocuments.DOTNET_SIGNED_REQUEST2).openStream();

                    final Message request = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, reqStream);
                    final Message response = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, respStream);

                    request.attachHttpRequestKnob(new HttpServletRequestKnob(new MockHttpServletRequest()));
                    response.attachHttpResponseKnob(new HttpServletResponseKnob(new MockHttpServletResponse()));
                    request.getXmlKnob();
                    request.getSoapKnob();

                    response.getXmlKnob();
                    response.getSoapKnob();

                    long cc = 0;
                    for (int i = 0; i < 5000; ++i) {
                        cc += 7;
                        cc += request.getKnob(XmlKnob.class).hashCode();
                        cc += request.getKnob(HttpRequestKnob.class).hashCode();
                        cc += response.getKnob(HttpResponseKnob.class).hashCode();
                        cc += response.getKnob(HttpResponseKnob.class).hashCode();
                        response.getKnob(TarariKnob.class);
                        cc += response.getKnob(HttpResponseKnob.class).hashCode();
                        cc += request.getKnob(TcpKnob.class).hashCode();
                        cc += response.getKnob(MimeKnob.class).hashCode();
                        cc += response.getKnob(HttpResponseKnob.class).hashCode();
                        request.getKnob(TarariKnob.class);
                        cc += request.getKnob(TcpKnob.class).hashCode();
                        cc += request.getKnob(HttpServletRequestKnob.class).hashCode();
                        cc += response.getKnob(HttpServletResponseKnob.class).hashCode();
                        cc += request.getKnob(XmlKnob.class).hashCode();
                        cc += response.getKnob(XmlKnob.class).hashCode();
                        cc += request.getKnob(HttpRequestKnob.class).hashCode();
                        cc += request.getKnob(TcpKnob.class).hashCode();
                        cc += response.getKnob(HttpResponseKnob.class).hashCode();
                        cc += request.getKnob(HttpRequestKnob.class).hashCode();
                        request.getKnob(JmsKnob.class);
                        request.getKnob(JmsKnob.class);
                        cc += request.getKnob(TcpKnob.class).hashCode();
                        cc += request.getKnob(XmlKnob.class).hashCode();
                        cc += request.getKnob(XmlKnob.class).hashCode();
                        cc += request.getKnob(HttpServletRequestKnob.class).hashCode();
                        cc += request.getKnob(HttpRequestKnob.class).hashCode();
                        cc += request.getKnob(XmlKnob.class).hashCode();
                        cc += request.getKnob(XmlKnob.class).hashCode();
                        cc += request.getKnob(HttpRequestKnob.class).hashCode();
                        cc += request.getKnob(HttpRequestKnob.class).hashCode();
                        cc += response.getKnob(HttpResponseKnob.class).hashCode();
                        request.getKnob(TarariKnob.class);
                        cc += request.getKnob(HttpRequestKnob.class).hashCode();
                        cc += request.getKnob(HttpRequestKnob.class).hashCode();
                        cc += request.getKnob(HttpRequestKnob.class).hashCode();
                        cc += request.getKnob(HttpRequestKnob.class).hashCode();
                        cc += request.getKnob(HttpRequestKnob.class).hashCode();
                        cc += response.getKnob(MimeKnob.class).hashCode();
                        request.getKnob(JmsKnob.class);
                    }

                    resultthing[0] += cc;

                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }, 1000, "getKnobTest");

        test.setThreadCount(5);
        test.run();

        System.out.println("Useless result to prevent optimizing away the test code: " + resultthing[0]);
    }
}
