/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.api.node.NodeManagementApi;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsClientFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/** @author alex */
public class NodeManagementApiTest {
    private static final Logger log = Logger.getLogger(NodeManagementApiTest.class.getName());
    
    @Test
    @Ignore("destructive and unlikely to work on your computer anyway")
    public void testDeleteNode() throws Exception {
        JaxWsProxyFactoryBean pfb = makeSslStub("https://localhost:8765/services/nodeManagementApi", NodeManagementApi.class);
        pfb.getInInterceptors().add(new LoggingInInterceptor());
        pfb.getOutInterceptors().add(new LoggingOutInterceptor());
        NodeManagementApi api = (NodeManagementApi)pfb.create();
        api.deleteNode("default", 36000);
    }

    private static JaxWsProxyFactoryBean makeSslStub(String url, final Class<?> apiClass) {
        final JaxWsProxyFactoryBean pfb = new JaxWsProxyFactoryBean(new JaxWsClientFactoryBean());
        pfb.setServiceClass(apiClass);
        pfb.setAddress(url);
        final Client c = pfb.getClientFactoryBean().create();
        final HTTPConduit httpConduit = (HTTPConduit)c.getConduit();
        httpConduit.setTlsClientParameters(new TLSClientParameters() {
            public boolean isDisableCNCheck() {
                return true;
            }

            // TODO should we explicitly trust the PC cert?
            public TrustManager[] getTrustManagers() {
                return new TrustManager[] { new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {}
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {}
                    public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                }};
            }
        });
        return pfb;
    }

}