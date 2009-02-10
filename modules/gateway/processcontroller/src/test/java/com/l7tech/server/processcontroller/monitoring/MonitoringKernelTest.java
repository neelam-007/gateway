/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import com.l7tech.server.management.api.monitoring.MonitoringApi;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.server.management.config.monitoring.HttpNotificationRule;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.management.config.monitoring.PropertyTrigger;
import com.l7tech.util.ComparisonOperator;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class MonitoringKernelTest {
    @Test @Ignore
    public void testMarshall() throws Exception {
        final MonitoringConfiguration mc = makeConfig();

        JAXBContext ctx = makeJaxbContext();
        Marshaller marshall = ctx.createMarshaller();
        marshall.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
        marshall.marshal(mc, System.out);
    }

    private JAXBContext makeJaxbContext() throws JAXBException {
        return JAXBContext.newInstance(MonitoringConfiguration.class, PropertyTrigger.class);
    }

    @Test @Ignore
    public void testStuff() throws Exception {
        final MonitoringConfiguration mc = makeConfig();

        JaxWsProxyFactoryBean pfb = new JaxWsProxyFactoryBean();
        pfb.setDataBinding(new JAXBDataBinding(makeJaxbContext()));
        pfb.setAddress("https://localhost:8765/services/monitoringApi");
        pfb.setServiceClass(MonitoringApi.class);
        Client c = pfb.getClientFactoryBean().create();
        HTTPConduit hc = (HTTPConduit) c.getConduit();
        hc.setTlsClientParameters(new TLSClientParameters() {
            @Override
            public TrustManager[] getTrustManagers() {
                return new TrustManager[] { new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                }};
            }

            @Override
            public KeyManager[] getKeyManagers() {
                return new KeyManager[0];
            }

            @Override
            public boolean isDisableCNCheck() {
                return true;
            }
        });

        MonitoringApi api = (MonitoringApi) pfb.create();
        api.pushMonitoringConfiguration(mc, true);
    }

    private MonitoringConfiguration makeConfig() {
        final MonitoringConfiguration mc = new MonitoringConfiguration();
        mc.setOid(1234);

        final HttpNotificationRule rulez = new HttpNotificationRule(mc);
        rulez.setUrl("http://localhost/");
        rulez.setMethod(HttpMethod.GET);
        rulez.setOid(342345);

        final PropertyTrigger tempTrigger = new PropertyTrigger(new MonitorableProperty(ComponentType.HOST, "cpuIdle", Integer.class), null, ComparisonOperator.GT, "10", 5000);
        tempTrigger.setOid(2345);
        tempTrigger.getNotificationRules().add(rulez);

        mc.getTriggers().add(tempTrigger);
        return mc;
    }
}
