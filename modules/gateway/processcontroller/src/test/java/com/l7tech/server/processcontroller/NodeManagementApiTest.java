/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.config.node.DatabaseConfig;
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
import java.util.Collections;

/** @author alex */
public class NodeManagementApiTest {
    
    @Test
    @Ignore("Destructive and unlikely to work on your computer anyway")
    public void testDeleteNode() throws Exception {
        NodeManagementApi api = makeSslStub();
        api.deleteNode("default", 36000);
    }

    @Test
    @Ignore("Change the mysql root password if you want to try it")
    public void testCreateDatabase() throws Exception {
        NodeManagementApi api = makeSslStub();
        final DatabaseConfig dbc = new DatabaseConfig("localhost", 3306, "ssgtemp", "gatewaytemp", "7layertemp");
        dbc.setDatabaseAdminUsername("root");
        dbc.setDatabaseAdminPassword("thisIsNotMyMysqlPassword");
        api.createDatabase("default", dbc, Collections.<String>emptySet(), "myadmin", "mypass");
    }

    private static NodeManagementApi makeSslStub() {
        final JaxWsProxyFactoryBean pfb = new JaxWsProxyFactoryBean(new JaxWsClientFactoryBean());
        pfb.setServiceClass(NodeManagementApi.class);
        pfb.setAddress("https://localhost:8765/services/nodeManagementApi");
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
        pfb.getInInterceptors().add(new LoggingInInterceptor());
        pfb.getOutInterceptors().add(new LoggingOutInterceptor());
        return (NodeManagementApi)pfb.create();
    }

}