/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.server.management.api.monitoring.MonitorableEvent;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import com.l7tech.server.management.api.monitoring.MonitoringApi;
import com.l7tech.server.management.config.monitoring.*;
import com.l7tech.util.ComparisonOperator;
import static junit.framework.Assert.assertEquals;
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
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class MonitoringKernelTest {
    @Test
    public void testMarshall() throws Exception {
        final MonitoringConfiguration mc1 = makeConfig();

        JAXBContext ctx = makeJaxbContext();

        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );

        ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
        marshaller.marshal(mc1, baos);
        String doc1 = new String(baos.toByteArray(), "UTF-8");
        System.out.println("mc1: " + doc1);

        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        MonitoringConfiguration mc2 = (MonitoringConfiguration) unmarshaller.unmarshal(new StringReader(doc1));

        baos.reset();
        marshaller.marshal(mc2, baos);
        String doc2 = new String(baos.toByteArray(), "UTF-8");
        System.out.println("mc2: " + doc2);

        assertEquals(doc1, doc2);
    }

    private JAXBContext makeJaxbContext() throws JAXBException {
        return JAXBContext.newInstance(MonitoringConfiguration.class, PropertyTrigger.class);
    }

    @Test @Ignore("doesn't work unless a PC is running at localhost") 
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
        mc.setName("My config");
        mc.setOid(1234);

        final HttpNotificationRule rulez = new HttpNotificationRule();
        rulez.setName("bring me a bucket");
        rulez.setUrl("http://localhost/");
        rulez.setMethod(HttpMethod.GET);
        rulez.setOid(342345);
        mc.getNotificationRules().add(rulez);

        final EmailNotificationRule rulez2 = new EmailNotificationRule();
        rulez2.setName("spammity spam");
        rulez2.setFrom("root@localhost");
        rulez2.setSubject("Uh-oh");
        rulez2.setOid(43243);
        mc.getNotificationRules().add(rulez2);

        final SnmpTrapNotificationRule rulez3 = new SnmpTrapNotificationRule();
        rulez3.setName("ITS A TRAP");
        rulez3.setCommunity("public");
        rulez3.setOidSuffix(1234);
        rulez3.setSnmpHost("localhost");
        rulez3.setOid(43244);
        mc.getNotificationRules().add(rulez3);

        final PropertyTrigger tempTrigger = new PropertyTrigger(new MonitorableProperty(ComponentType.HOST, "cpuIdle", Integer.class), null, ComparisonOperator.GT, "10", 5000);
        tempTrigger.setName("idoru");
        tempTrigger.setOid(2345);
        tempTrigger.getNotificationRules().add(rulez);
        tempTrigger.getNotificationRules().add(rulez2);
        tempTrigger.getNotificationRules().add(rulez3);

        final EventTrigger anotherTrigger = new EventTrigger(new MonitorableEvent(ComponentType.HOST, "shuttingDown"), null, 1, null);
        anotherTrigger.setOid(2342);
        anotherTrigger.getNotificationRules().add(rulez3);
        anotherTrigger.getNotificationRules().add(rulez2);

        mc.getTriggers().add(tempTrigger);
        mc.getTriggers().add(anotherTrigger);
        
        return mc;
    }
}
