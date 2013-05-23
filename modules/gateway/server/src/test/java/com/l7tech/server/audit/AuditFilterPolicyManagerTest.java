/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.audit;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.folder.FolderCacheStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.*;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

public class AuditFilterPolicyManagerTest {
    private PolicyCacheImpl policyCache;
    private AuditLogListener auditLogListener;
    private AuditFilterPolicyManager filterPolicyManager;
    private MessageSummaryAuditRecord record;
    private Policy policy;
    private PolicyManager policyManager;
    private String successAMFOutput;
    private List<Integer> auditedMessages;

    @Before
    public void setUp() throws Exception{
        ApplicationContext testApplicationContext = ApplicationContexts.getTestApplicationContext();
        ServerPolicyFactory pfac = (ServerPolicyFactory)testApplicationContext.getBean("policyFactory");

        policyCache = new PolicyCacheImpl(null, pfac, new FolderCacheStub()){
            @Override
            protected void logAndAudit(AuditDetailMessage message, String... params) {
                System.err.println( MessageFormat.format(message.getMessage(), (Object[])params) );
            }

            @Override
            protected void logAndAudit(AuditDetailMessage message, String[] params, Exception ex) {
                System.err.println( MessageFormat.format(message.getMessage(), (Object[])params) );
                if ( ex != null ) ex.printStackTrace();
            }
        };
        ApplicationEventPublisher aep = new ApplicationEventPublisher() {
            @Override
            public void publishEvent( ApplicationEvent event ) {
            }
        };

        policy = new Policy(PolicyType.INTERNAL, "test", successPolicyXml, false);
        policy.setGuid(UUID.nameUUIDFromBytes("test".getBytes()).toString());
        policy.setOid(23423432);
        policy.setInternalTag(PolicyType.TAG_AUDIT_MESSAGE_FILTER);

        policyManager = new PolicyManagerStub(new Policy[]{policy});
        
        policyCache.setApplicationEventPublisher(aep);
        policyCache.setPolicyManager(policyManager);
        policyCache.onApplicationEvent(new Started(this, Component.GATEWAY, "Test"));
        auditLogListener = new AuditLogListener() {
            @Override
            public void notifyDetailCreated(String source, String loggerName, AuditDetailMessage message, String[] params, AuditLogFormatter formatter, Throwable thrown) {

            }

            @Override
            public void notifyDetailFlushed(String source, String loggerName, AuditDetailMessage message, String[] params, AuditLogFormatter formatter, Throwable thrown) {
                System.out.println("notifyDetailFlushed: " + source+" " + loggerName+ " " + message.getId()+" " + Arrays.asList(params)+" " + thrown);
                auditedMessages.add(message.getId());
            }

            @Override
            public void notifyRecordFlushed(AuditRecord record, AuditLogFormatter formatter, boolean header) {

            }
        };
        filterPolicyManager = new AuditFilterPolicyManager(policyCache, TestStashManagerFactory.getInstance());
        record = new MessageSummaryAuditRecord(Level.WARNING,
                "node id",
                "request id",
                AssertionStatus.NONE,
                "127.0.0.1",
                "request xml",
                1000,
                "response xml",
                1000,
                200,
                10,
                123,
                "serviceName",
                null,
                false, null, 02, "user name", "user id", null);
        successAMFOutput = "<output>AMF output</output>";
        auditedMessages = new ArrayList<Integer>();
    }

    @Test
    public void testGetAMFPolicy(){
        final Set<String> guids = policyCache.getPoliciesByTypeAndTag(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_MESSAGE_FILTER);
        Assert.assertNotNull("Guid should have been found", guids);
        Assert.assertFalse("Guid should have been found", guids.isEmpty());
    }

    @Test
    public void testAMFPolicy() throws Exception {
        updatePolicyXml(successPolicyXml);
        final String requestXml = "<xml>test</xml>";
        final Message requestMsg = new Message();
        requestMsg.initialize(ContentTypeHeader.XML_DEFAULT, requestXml.getBytes());
        final Message responseMsg = new Message();
        responseMsg.initialize(ContentTypeHeader.XML_DEFAULT, requestXml.getBytes());
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);

        filterPolicyManager.filterAuditRecord(record, context, auditLogListener, new  AuditLogFormatter() );
        Assert.assertEquals("Invalid AMF output for request", successAMFOutput, record.getRequestXml());
        Assert.assertEquals("Invalid AMF output for response", successAMFOutput, record.getResponseXml());
    }

    @Test
    public void testAmfFailsForRequestAndResponse() throws Exception {
        updatePolicyXml(policyToReturnErrorStatus);
        final String requestXml = "<xml>test</xml>";
        final Message requestMsg = new Message();
        requestMsg.initialize(ContentTypeHeader.XML_DEFAULT, requestXml.getBytes());
        final Message responseMsg = new Message();
        responseMsg.initialize(ContentTypeHeader.XML_DEFAULT, requestXml.getBytes());
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);

        filterPolicyManager.filterAuditRecord(record, context, auditLogListener, new  AuditLogFormatter() );
        Assert.assertNull("Request xml should be null when policy fails", record.getRequestXml());
        Assert.assertNull("Response xml should be null when policy fails", record.getResponseXml());
    }

    @Test
    @BugNumber(10082)
    public void testBug_ProcessSoapFaultResponse() throws Exception{
        updatePolicyXml(successPolicyXml);
        final String requestXml = "<xml>test</xml>";
        final Message requestMsg = new Message();
        final ContentTypeHeader xmlTypeHeader = ContentTypeHeader.XML_DEFAULT;
        requestMsg.initialize(xmlTypeHeader, requestXml.getBytes());
        final Message responseMsg = new Message();//not initialized, so no input stream
        responseMsg.close();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);

        record.setResponseXml(soapFaultXml);//this should be used as response is not available
        filterPolicyManager.filterAuditRecord(record, context, auditLogListener, new  AuditLogFormatter() );
        System.out.println(record.getResponseXml());
        Assert.assertEquals("Response should have been processed based on the audit records response XML", successAMFOutput, record.getResponseXml());
    }

    /**
     * Create a request message and consume it's first part destructively such that the next attempt to read it will
     * cause an exception. This will cause the AuditFilterPolicymanager to attempt to audit, which reproduces this bug.
     *
     * Also validates that the audit record's request and response XML are null, owning to AMF policy execution failure.
     * Also validates that correct number of audits are created and that each audit detail has the correct ordinal value.
     * @throws Exception
     */
    @Test
    @BugNumber(10104)
    public void testBug_AuditingCausedByAssertion() throws Exception{
        updatePolicyXml(successPolicyXml);
        final String requestXml = "<xml>test</xml>";
        final Message requestMsg = new Message();
        requestMsg.initialize(ContentTypeHeader.XML_DEFAULT, requestXml.getBytes());
        final InputStream inputStream = requestMsg.getMimeKnob().getPart(0).getInputStream(true);
        IOUtils.slurpStream(inputStream);
        
        final Message responseMsg = new Message();//not initialized, so no input stream
        responseMsg.initialize(ContentTypeHeader.XML_DEFAULT, requestXml.getBytes());
        IOUtils.slurpStream(responseMsg.getMimeKnob().getPart(0).getInputStream(true));
        
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);

        filterPolicyManager.filterAuditRecord(record, context, auditLogListener, new  AuditLogFormatter() );
        //no exception, bug is fixed.

        Assert.assertEquals("Incorrect number of audit details flushed", 4, auditedMessages.size());
        final AuditDetail[] details = record.getDetailsInOrder();
        Assert.assertEquals("Incorrect number of ordered audits found", 4, details.length);

        Assert.assertEquals("Detail should have 0 ordinal", 0, details[0].getOrdinal());
        Assert.assertEquals("Detail should have 1 ordinal", 1, details[1].getOrdinal());
        Assert.assertEquals("Detail should have 2 ordinal", 2, details[2].getOrdinal());
        Assert.assertEquals("Detail should have 3 ordinal", 3, details[3].getOrdinal());

        Assert.assertNull("requestXml should be null",  record.getRequestXml());
        Assert.assertNull("responseXml should be null",  record.getResponseXml());
    }

    // - PRIVATE
    
    private static final String successPolicyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"PG91dHB1dD5BTUYgb3V0cHV0PC9vdXRwdXQ+\"/>\n" +
            "            <L7p:ContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
            "            <L7p:DataType variableDataType=\"message\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"request\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    private static final String policyToReturnErrorStatus = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:RequestXpathAssertion>\n" +
            "            <L7p:XpathExpression xpathExpressionValue=\"included\">\n" +
            "                <L7p:Expression stringValue=\"/s:Envelope/s:Body\"/>\n" +
            "                <L7p:Namespaces mapValue=\"included\">\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"s\"/>\n" +
            "                        <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
            "                    </L7p:entry>\n" +
            "                </L7p:Namespaces>\n" +
            "            </L7p:XpathExpression>\n" +
            "        </L7p:RequestXpathAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    private static final String soapFaultXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "    <soapenv:Body>\n" +
            "        <soapenv:Fault>\n" +
            "            <faultcode>soapenv:Server</faultcode>\n" +
            "            <faultstring>Policy Falsified</faultstring>\n" +
            "            <faultactor>http://localhost:8080/av</faultactor>\n" +
            "            <detail>\n" +
            "                <l7:policyResult status=\"Error in Assertion Processing\" xmlns:l7=\"http://www.layer7tech.com/ws/policy/fault\"/>\n" +
            "            </detail>\n" +
            "        </soapenv:Fault>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private void updatePolicyXml(String newXml) throws UpdateException {
        policy.setXml(newXml);
        policyManager.update(policy);
        policyCache.update(policy);
    }
}
