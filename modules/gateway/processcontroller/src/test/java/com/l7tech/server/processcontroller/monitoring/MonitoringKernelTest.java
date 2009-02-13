/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.server.management.api.monitoring.*;
import com.l7tech.server.management.config.monitoring.*;
import com.l7tech.util.ComparisonOperator;
import static junit.framework.Assert.assertEquals;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
        return JAXBContext.newInstance(
                MonitoringConfiguration.class,
                PropertyTrigger.class,
                MonitoredPropertyStatus.class
        );
    }

    @Test @Ignore("doesn't work unless a PC is running at localhost")
    public void testPushConfiguration() throws Exception {
        final MonitoringConfiguration mc = makeConfig();

        MonitoringApi api = getApi();
        api.pushMonitoringConfiguration(mc, true);
    }

    @Test @Ignore("doesn't work unless a PC is running at localhost")
    public void testGetCurrentProperties() throws Exception {
        MonitoringApi api = getApi();
        List<MonitoredPropertyStatus> stats = api.getCurrentPropertyStatuses();
        System.out.println(stats);
    }


    private MonitoringApi getApi() throws JAXBException {
        JaxWsProxyFactoryBean pfb = new JaxWsProxyFactoryBean();
        pfb.setDataBinding(new JAXBDataBinding(makeJaxbContext()));
        pfb.setAddress("https://localhost:8765/services/monitoringApi");
        pfb.setServiceClass(MonitoringApi.class);
        Client c = pfb.getClientFactoryBean().create();
        c.getInInterceptors().add( new LoggingInInterceptor() );
        c.getOutInterceptors().add( new LoggingOutInterceptor() );
        c.getInFaultInterceptors().add( new LoggingInInterceptor() );
        c.getOutFaultInterceptors().add( new LoggingOutInterceptor() );
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

        return (MonitoringApi) pfb.create();
    }

    private MonitoringConfiguration makeConfig() {
        Random random = new Random();
        final MonitoringConfiguration mc = new MonitoringConfiguration();
        mc.setName("My config");
        mc.setOid(Math.abs(random.nextLong()));

        final HttpNotificationRule rulez = new HttpNotificationRule();
        rulez.setName("bring me a bucket");
        rulez.setUrl("http://localhost/");
        rulez.setMethod(HttpMethod.GET);
        rulez.setOid(Math.abs(random.nextLong()));
        mc.getNotificationRules().add(rulez);

        final EmailNotificationRule rulez2 = new EmailNotificationRule();
        rulez2.setName("spammity spam");
        rulez2.setFrom("root@localhost");
        rulez2.setSubject("Uh-oh");
        rulez2.setSmtpHost("localhost");
        rulez2.setOid(Math.abs(random.nextLong()));
        mc.getNotificationRules().add(rulez2);

        final SnmpTrapNotificationRule rulez3 = new SnmpTrapNotificationRule();
        rulez3.setName("ITS A TRAP");
        rulez3.setCommunity("public");
        rulez3.setOidSuffix(1234);
        rulez3.setSnmpHost("localhost");
        rulez3.setText("yo dawg, I herd you like traps");
        rulez3.setOid(Math.abs(random.nextLong()));
        mc.getNotificationRules().add(rulez3);

        addTrigger(mc, BuiltinMonitorables.CPU_IDLE, ComparisonOperator.GT, "10", Math.abs(random.nextLong()), 5000, rulez3);
        addTrigger(mc, BuiltinMonitorables.AUDIT_SIZE, ComparisonOperator.LT, "100000000", Math.abs(random.nextLong()), 30000, rulez3);
        addTrigger(mc, BuiltinMonitorables.DISK_FREE_KIB, ComparisonOperator.LT, "1000000000", Math.abs(random.nextLong()), 30000, rulez3);
        addTrigger(mc, BuiltinMonitorables.DISK_USAGE_PERCENT, ComparisonOperator.GT, "50", Math.abs(random.nextLong()), 30000, rulez3);
        addTrigger(mc, BuiltinMonitorables.NTP_STATUS, ComparisonOperator.NE, "OK", Math.abs(random.nextLong()), 60000, rulez2, rulez3);

        final EventTrigger anotherTrigger = new EventTrigger(new MonitorableEvent(ComponentType.HOST, "shuttingDown"), null, 1, null);
        anotherTrigger.setOid(Math.abs(random.nextLong()));

        return mc;
    }

    private void addTrigger(MonitoringConfiguration mc, final MonitorableProperty prop, final ComparisonOperator op, final String val, final long oid, final int interval, NotificationRule... rulez) {
        final PropertyTrigger tempTrigger = new PropertyTrigger(prop, "foo.bar.example.com", op, val, interval);
        tempTrigger.setName("idoru");
        tempTrigger.setOid(oid);
        tempTrigger.getNotificationRules().addAll(Arrays.asList(rulez));
        mc.getTriggers().add(tempTrigger);
    }
}
