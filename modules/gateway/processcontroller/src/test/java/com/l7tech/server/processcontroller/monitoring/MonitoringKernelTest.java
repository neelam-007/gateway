/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.management.api.monitoring.*;
import com.l7tech.server.management.config.monitoring.*;
import com.l7tech.server.processcontroller.CxfUtils;
import com.l7tech.test.BugId;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ComparisonOperator;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertEquals;

public class MonitoringKernelTest {
    @Test
    public void testMarshallMonitoringConfiguration() throws Exception {
        testMarshallRoundTrip(makeConfig());
    }

    @Test
    public void testMarshallNotificationAttemptSuccessful() throws Exception {
        Object successfulAttempt = new NotificationAttempt(NotificationAttempt.StatusType.ACKNOWLEDGED,
                "Message that was successfully sent to some HTTP listener that cared deeply", System.currentTimeMillis());
        testMarshallRoundTrip(successfulAttempt);
    }

    @Test
    public void testMarshallNotificationAttemptFailed() throws Exception {
        //noinspection ThrowableInstanceNeverThrown
        Object successfulAttempt = new NotificationAttempt(new GenericHttpException("Oh noes"), System.currentTimeMillis());
        testMarshallRoundTrip(successfulAttempt);
    }

    @Test
    public void testMarshallingMonitoringStatus() throws Exception {
        Object successfulAttempt = new MonitoredPropertyStatus(
                ComponentType.NODE,
                "monitorableId",
                "componentId",
                System.currentTimeMillis(),
                MonitoredStatus.StatusType.OK,
                CollectionUtils.set(new Goid(123L, 456L), new Goid(444L, 555L)),
                "value",
                MonitoredPropertyStatus.ValueType.OK);
        testMarshallRoundTrip(successfulAttempt);
    }

    @BugId("EM-999")
    @Test
    public void testUnMarshallingMonitoringStatusOidsVersions() throws Exception {
        String oidsMarchalled = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<monitoredPropertyStatus xmlns:ns2=\"http://ns.l7tech.com/secureSpan/1.0/monitoring\" xmlns:ns3=\"http://ns.l7tech.com/secureSpan/1.0/core\" xmlns:ns4=\"http://ns.l7tech.com/secureSpan/1.0/monitoring/notification\" valueType=\"OK\" componentId=\"componentId\" monitorableId=\"monitorableId\" status=\"OK\" timestamp=\"1397760968529\" type=\"NODE\">\n" +
                "    <triggerOids>\n" +
                "        <triggerOids>123</triggerOids>\n" +
                "        <triggerOids>456</triggerOids>\n" +
                "    </triggerOids>\n" +
                "    <value>value</value>\n" +
                "</monitoredPropertyStatus>\n";

        JAXBContext ctx = makeJaxbContext();

        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        Object mc2 = unmarshaller.unmarshal(new StringReader(oidsMarchalled));

        ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
        baos.reset();
        marshaller.marshal(mc2, baos);
        String doc2 = new String(baos.toByteArray(), "UTF-8");
        System.out.println("re-marshalled: " + doc2);

        assertEquals(oidsMarchalled, doc2);
    }

    @BugId("EM-999")
    @Test
    public void testUnMarshallingMonitoringStatusGoidsVersions() throws Exception {
        String oidsMarchalled = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<monitoredPropertyStatus xmlns:ns2=\"http://ns.l7tech.com/secureSpan/1.0/monitoring\" xmlns:ns3=\"http://ns.l7tech.com/secureSpan/1.0/core\" xmlns:ns4=\"http://ns.l7tech.com/secureSpan/1.0/monitoring/notification\" valueType=\"OK\" componentId=\"componentId\" monitorableId=\"monitorableId\" status=\"OK\" timestamp=\"1397762286281\" type=\"NODE\">\n" +
                "    <triggerOids>\n" +
                "        <triggerOids>000000000000007b00000000000001c8</triggerOids>\n" +
                "        <triggerOids>00000000000001bc000000000000022b</triggerOids>\n" +
                "    </triggerOids>\n" +
                "    <value>value</value>\n" +
                "</monitoredPropertyStatus>\n";

        JAXBContext ctx = makeJaxbContext();

        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        Object mc2 = unmarshaller.unmarshal(new StringReader(oidsMarchalled));

        ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
        baos.reset();
        marshaller.marshal(mc2, baos);
        String doc2 = new String(baos.toByteArray(), "UTF-8");
        System.out.println("re-marshalled: " + doc2);

        assertEquals(oidsMarchalled, doc2);
    }

    // Test the specified object to ensure it survives a round trip through the result of makeJaxbContext()
    private void testMarshallRoundTrip(Object toMarshall) throws JAXBException, UnsupportedEncodingException {
        JAXBContext ctx = makeJaxbContext();

        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
        marshaller.marshal(toMarshall, baos);
        String doc1 = new String(baos.toByteArray(), "UTF-8");
        System.out.println("doc1: " + doc1);

        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        Object mc2 = unmarshaller.unmarshal(new StringReader(doc1));

        baos.reset();
        marshaller.marshal(mc2, baos);
        String doc2 = new String(baos.toByteArray(), "UTF-8");
        System.out.println("doc2: " + doc2);

        assertEquals(doc1, doc2);
    }

    private JAXBContext makeJaxbContext() throws JAXBException {
        return JAXBContext.newInstance(
                MonitoringConfiguration.class,
                PropertyTrigger.class,
                MonitoredPropertyStatus.class,
                NotificationAttempt.class
        );
    }

    @Test
    @Ignore("doesn't work unless a PC is running at localhost")
    public void testPushConfiguration() throws Exception {
        final MonitoringConfiguration mc = makeConfig();
        mc.setResponsibleForClusterMonitoring(true);

        MonitoringApi api = getApi();
        api.pushMonitoringConfiguration(mc);
    }

    @Test
    @Ignore("doesn't work unless a PC is running at localhost")
    public void testUnsetConfiguration() throws Exception {
        getApi().pushMonitoringConfiguration(null);
    }

    @Test
    @Ignore("doesn't work unless a PC is running at localhost")
    public void testGetCurrentProperties() throws Exception {
        MonitoringApi api = getApi();
        List<MonitoredPropertyStatus> stats = api.getCurrentPropertyStatuses();
        System.out.println(stats);
    }

    @Test
    @Ignore("doesn't work unless a PC is running at localhost")
    public void testGetRecentNotifications() throws Exception {
        MonitoringApi api = getApi();
        List<NotificationAttempt> attempts = api.getRecentNotificationAttempts(0);
        System.out.println(attempts);
    }

    private MonitoringApi getApi() throws JAXBException {
        // todo: really need to override getKeyManagers() for the TLS params?
        CxfUtils.ApiBuilder apiBuilder = new CxfUtils.ApiBuilder("https://localhost:8765/services/monitoringApi")
            .dataBinding(new JAXBDataBinding(makeJaxbContext()))
            .inFaultInterceptor(new LoggingInInterceptor()).outFaultInterceptor(new LoggingOutInterceptor());
        return apiBuilder.build(MonitoringApi.class);
    }

    public static MonitoringConfiguration makeConfig() {
        Random random = new Random();
        final MonitoringConfiguration mc = new MonitoringConfiguration();
        mc.setGoid(new Goid(0,Math.abs(random.nextLong())));

        final HttpNotificationRule rulez = new HttpNotificationRule();
        rulez.setUrl("http://localhost:8080/dump");
        rulez.setMethod(HttpMethod.POST);
        rulez.setRequestBody("<a>Hi there!</a>");
        rulez.setContentType("text/xml; charset=\"utf-8\"");
        rulez.setGoid(new Goid(0,Math.abs(random.nextLong())));
        mc.getNotificationRules().add(rulez);

        final EmailNotificationRule rulez2 = new EmailNotificationRule();
        rulez2.setFrom("acruise@layer7tech.com");
        rulez2.setTo(Arrays.asList("acruise@layer7tech.com"));
        rulez2.setSubject("Uh-oh");
        rulez2.setSmtpHost("mail.l7tech.com");
        rulez2.setText("Here is a very nice message for you");
        rulez2.setGoid(new Goid(0,Math.abs(random.nextLong())));
        mc.getNotificationRules().add(rulez2);

        final SnmpTrapNotificationRule rulez3 = new SnmpTrapNotificationRule();
        rulez3.setCommunity("public");
        rulez3.setOidSuffix(1234);
        rulez3.setSnmpHost("localhost");
        rulez3.setText("yo dawg, I herd you like traps");
        rulez3.setGoid(new Goid(0,Math.abs(random.nextLong())));
        mc.getNotificationRules().add(rulez3);

        addTrigger(mc, BuiltinMonitorables.AUDIT_SIZE, ComparisonOperator.LT, "100000000", Math.abs(random.nextLong()), 60000, rulez, rulez2, rulez3);
        addTrigger(mc, BuiltinMonitorables.CPU_TEMPERATURE, ComparisonOperator.GT, "50", Math.abs(random.nextLong()), 60000, rulez, rulez2, rulez3);
        addTrigger(mc, BuiltinMonitorables.CPU_USAGE, ComparisonOperator.GT, "90", Math.abs(random.nextLong()), 60000, rulez, rulez2, rulez3);
        addTrigger(mc, BuiltinMonitorables.DISK_FREE_KIB, ComparisonOperator.LT, "1000000000", Math.abs(random.nextLong()), 60000, rulez, rulez2, rulez3);
        addTrigger(mc, BuiltinMonitorables.DISK_USAGE_PERCENT, ComparisonOperator.GT, "50", Math.abs(random.nextLong()), 60000, rulez, rulez2, rulez3);
        addTrigger(mc, BuiltinMonitorables.LOG_SIZE, ComparisonOperator.GT, "10000", Math.abs(random.nextLong()), 60000, rulez, rulez2, rulez3);
        addTrigger(mc, BuiltinMonitorables.NODE_STATE, ComparisonOperator.NE, "RUNNING", Math.abs(random.nextLong()), 60000, rulez, rulez2, rulez3);
        addTrigger(mc, BuiltinMonitorables.NTP_STATUS, ComparisonOperator.NE, "OK", Math.abs(random.nextLong()), 60000, rulez, rulez2, rulez3);
        addTrigger(mc, BuiltinMonitorables.SWAP_USAGE_KIB, ComparisonOperator.GT, "100000", Math.abs(random.nextLong()), 60000, rulez, rulez2, rulez3);
        addTrigger(mc, BuiltinMonitorables.TIME, ComparisonOperator.GT, "1234567890", Math.abs(random.nextLong()), 60000, rulez, rulez2, rulez3);
        addTrigger(mc, BuiltinMonitorables.RAID_STATUS, ComparisonOperator.NE, "OK", Math.abs(random.nextLong()), 60000, rulez, rulez2, rulez3);

        final EventTrigger anotherTrigger = new EventTrigger(new MonitorableEvent(ComponentType.HOST, "shuttingDown"), null, 1, null);
        anotherTrigger.setGoid(new Goid(0,Math.abs(random.nextLong())));

        return mc;
    }

    public static void addTrigger(MonitoringConfiguration mc, final MonitorableProperty prop, final ComparisonOperator op, final String val, final long oid, final int interval, NotificationRule... rulez) {
        final PropertyTrigger tempTrigger = new PropertyTrigger(prop, "foo.bar.example.com", op, val, interval);
        tempTrigger.setName("idoru");
        tempTrigger.setGoid(new Goid(0,oid));
        tempTrigger.getNotificationRules().addAll(Arrays.asList(rulez));
        mc.getTriggers().add(tempTrigger);
    }
}
