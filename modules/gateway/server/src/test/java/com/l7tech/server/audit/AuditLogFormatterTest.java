package com.l7tech.server.audit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;
import com.l7tech.util.MockConfig;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static org.junit.Assert.*;

/**
 * User: vchan
 */
public class AuditLogFormatterTest {

    private ApplicationContext appCtx;
    private PolicyEnforcementContext pec;

    private Config config;
    private Map<String,String> cfgMap;
    private Map<String, Object> ctxMap;

    // audit generators
    private MessageSummaryAuditGen summAuditGenerator;
    private SystemAuditGen sysAuditGenerator;
    private AdminAuditGen adminAuditGenerator;

    // bug #6671
    private final String BAD_FMT_TEMPLATE =
            "bad tags template. >{}< {0} {1} {2} >{a}< {3} {4} {1011} >${}< >${a}< >${aa}< >${aaa}< $ " +
            "section 2. >{     }< >{x}< >{foo}< >{ww ww}< >{2 2}< >{2123xx}< >{x3x2s3}< >{x3x_2s3}< >{n98}< >${  }< " +
            "section 3. >{1.2 }< >{ 2.2}< >{ 3.3 }< >{1x2x2}< >{....123}< >{.2.a}< >{ .2.a}< >{.2.a }<";

    @Before
    public void setUp() throws Exception {
        if (pec == null) {
            PublishedService pubsvc = new PublishedService();
            pubsvc.setGoid(new Goid(0, 393216L));
            pubsvc.setName("Warehoust");
            pubsvc.setRoutingUri("/wh2");
            Message req = new Message(XmlUtil.createEmptyDocument());
            Message resp = new Message(XmlUtil.createEmptyDocument());
            pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, resp);
            pec.setService(pubsvc);
        }
        if (appCtx != null && pec != null) {
            try {
                Class.forName(com.l7tech.gateway.common.audit.CommonMessages.class.getName());
            } catch (ClassNotFoundException nfe) {
                nfe.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        if (ctxMap == null) {
            // all key values must be lowercase
            ctxMap = new HashMap<String, Object>();
            ctxMap.put("requestid", "reqId-00002");
            ctxMap.put("strvar1", "abc123");
            ctxMap.put("strvar2", "some test string");
            ctxMap.put("arrayvar1", new String[] {"a1", "a2", "a3"});
            ctxMap.put("bigassmsg", "my big ass message");
        }

        if (summAuditGenerator == null || sysAuditGenerator == null || adminAuditGenerator == null) {
            summAuditGenerator = new MessageSummaryAuditGen();
            sysAuditGenerator = new SystemAuditGen();
            adminAuditGenerator = new AdminAuditGen();
        }

        cfgMap = new HashMap<String, String>();
        cfgMap.put( "auditLogFormatServiceHeader", "Processing request for service: {3}" );
        cfgMap.put( "auditLogFormatServiceFooter", "{1}" );
        cfgMap.put( "auditLogFormatServiceDetail", "{0}: {1}" );
        cfgMap.put( "auditLogFormatOther", "{1}" );
        cfgMap.put( "auditLogFormatOtherDetail", "{0}: {1}" );

        config = new MockConfig(cfgMap);
    }

    @Test
    public void testMessageSummaryAuditHeader() {

        final String TEST_PROP = ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_HEADER;

        TestingAuditLogFormatter.setTestingConfig( config );
        try {
            MessageSummaryAuditRecord testRecord = summAuditGenerator.createAuditRecord();
            String logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, true);
            checkFormattedLog("AuditHeader: default", "Processing request for service: Warehoust [/wh2]", logStr);

            // test with custom properties
            setCustomTestFormat(TEST_PROP, "Processing service_oid={2} name={3}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, true);
            checkFormattedLog("AuditHeader-1: valid parms", "Processing service_oid="+new Goid(0,393216L).toHexString()+" name=Warehoust [/wh2]", logStr);

            setCustomTestFormat(TEST_PROP, "header: {2};{3};ignore={0}{1};unchanged={4}{8}{16}{32}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, true);
            checkFormattedLog("AuditHeader-2: all parms", "header: "+new Goid(0,393216L).toHexString()+";Warehoust [/wh2];ignore=;unchanged={4}{8}{16}{32}", logStr);

            setCustomTestFormat(TEST_PROP, "Just plain text");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, true);
            checkFormattedLog("AuditHeader-3: all parms", "Just plain text", logStr);

            cfgMap.remove(TEST_PROP);
            TestingAuditLogFormatter.notifyPropertyChange(TEST_PROP);
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, true);
            checkFormattedLog("AuditHeader-4: null test", "Processing request for service: Warehoust [/wh2]", logStr);

            setCustomTestFormat(TEST_PROP, "");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, true);
            assertNull("AuditHeader-5: empty property, expecting null", logStr);

            // tests with cluster properties
            setCustomTestFormat(TEST_PROP, "test ctx ${requestId}, ${strvar1}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).format(testRecord, true);
            checkFormattedLog("AuditHeader-6: ctx vars", "test ctx reqId-00002, abc123", logStr);

            setCustomTestFormat(TEST_PROP, "test ctx ${arrayVar1}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).format(testRecord, true);
            checkFormattedLog("AuditHeader-7: ctx vars", "test ctx a1, a2, a3", logStr);

            setCustomTestFormat(TEST_PROP, "test ctx ${requestId}, ignore=${ignoreVar}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).format(testRecord, true);
            checkFormattedLog("AuditHeader-7: ctx vars", "test ctx reqId-00002, ignore=", logStr);

            setCustomTestFormat(TEST_PROP, BAD_FMT_TEMPLATE);
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).format(testRecord, true);
            checkFormattedLog("AuditHeader-8: ctx vars, bad formatting",
                    "bad tags template. ><   "+new Goid(0,393216L).toHexString()+" >< Warehoust [/wh2] {4} {1011} >< >< >< >< $ section 2. >< >< >< >< >< >< >< >< >< >< section 3. >< >< >< >< >< >< >< ><",
                    logStr);

            // ++test var length limit, test total msg length limit++

        } finally {
            TestingAuditLogFormatter.setTestingConfig(null);
            TestingAuditLogFormatter.notifyPropertyChange(TEST_PROP);
        }
    }

    @Test
    public void testMessageSummaryAuditFooter() {

        final String TEST_PROP = ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_FOOTER;

        TestingAuditLogFormatter.setTestingConfig( config );
        try {
            MessageSummaryAuditRecord testRecord = summAuditGenerator.createAuditRecord();
            String logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, false);
            checkFormattedLog("AuditFooter: default", testRecord, logStr);

            // test with custom properties
            setCustomTestFormat(TEST_PROP, "{1} oid={2} svc={3}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, false);
            checkFormattedLog("AuditFooter-1: valid parms", "Message processed successfully oid="+new Goid(0,393216L).toHexString()+" svc=Warehoust [/wh2]", logStr);

            setCustomTestFormat(TEST_PROP, "footer: {1};{2};{3};ignore={0};unchanged={4}{8}{16}{32}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, false);
            checkFormattedLog("AuditFooter-2: all parms", "footer: Message processed successfully;"+new Goid(0,393216L).toHexString()+";Warehoust [/wh2];ignore=;unchanged={4}{8}{16}{32}", logStr);

            setCustomTestFormat(TEST_PROP, "Just plain text");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, false);
            checkFormattedLog("AuditFooter-3: all parms", "Just plain text", logStr);

            setCustomTestFormat(TEST_PROP, null);
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, false);
            checkFormattedLog("AuditFooter-4: null test", testRecord, logStr);

            setCustomTestFormat(TEST_PROP, "");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).format(testRecord, false);
            assertNull("AuditFooter-5: empty property, expecting null", logStr);

            // tests with cluster properties
            setCustomTestFormat(TEST_PROP, "footer-ctx: ${requestId}, ${strvar1}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).format(testRecord, false);
            checkFormattedLog("AuditFooter-6: ctx vars", "footer-ctx: reqId-00002, abc123", logStr);

            setCustomTestFormat(TEST_PROP, "footer-ctx: ${arrayVar1}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).format(testRecord, false);
            checkFormattedLog("AuditFooter-7: ctx vars", "footer-ctx: a1, a2, a3", logStr);

            setCustomTestFormat(TEST_PROP, "footer-ctx: ${requestId}, ignore=${ignoreVar}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).format(testRecord, false);
            checkFormattedLog("AuditFooter-8: ctx vars", "footer-ctx: reqId-00002, ignore=", logStr);

            setCustomTestFormat(TEST_PROP, BAD_FMT_TEMPLATE);
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).format(testRecord, false);
            checkFormattedLog("AuditFooter-9: ctx vars, bad template",
                    "bad tags template. ><  Message processed successfully "+new Goid(0,393216L).toHexString()+" >< Warehoust [/wh2] {4} {1011} >< >< >< >< $ section 2. >< >< >< >< >< >< >< >< >< >< section 3. >< >< >< >< >< >< >< ><",
                    logStr);

        } finally {
            TestingAuditLogFormatter.setTestingConfig(null);
            TestingAuditLogFormatter.notifyPropertyChange(TEST_PROP);
        }
    }

    @Test
    public void testAuditDetail() {

        final String TEST_PROP = ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_SERVICE_DETAIL;

        TestingAuditLogFormatter.setTestingConfig( config );
        try {
            MessageSummaryAuditRecord testRecord = summAuditGenerator.createAuditRecord();
            AuditDetailMessage dtl = summAuditGenerator.createAuditDetail(4001);
            String logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).formatDetail(testRecord, dtl);
            checkFormattedDetail("AuditDetail: default", dtl, logStr);

            int mid = dtl.getId();
            String mmsg = dtl.getMessage();

            // test with custom properties
            setCustomTestFormat(TEST_PROP, "{0}: {1} oid={2} svc={3}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).formatDetail(testRecord, dtl);
            checkFormattedLog("AuditDetail-1: valid parms", mid + ": " + mmsg + " oid="+new Goid(0,393216L).toHexString()+" svc=Warehoust [/wh2]", logStr);

            setCustomTestFormat(TEST_PROP, "detail: {0};{1};{2};{3};ignore=none;unchanged={4}{8}{16}{32}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).formatDetail(testRecord, dtl);
            checkFormattedLog("AuditDetail-2: all parms", "detail: "+mid+";"+mmsg+";"+new Goid(0,393216L).toHexString()+";Warehoust [/wh2];ignore=none;unchanged={4}{8}{16}{32}", logStr);

            setCustomTestFormat(TEST_PROP, "Just plain text");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).formatDetail(testRecord, dtl);
            checkFormattedLog("AuditDetail-3: all parms", "Just plain text", logStr);

            setCustomTestFormat(TEST_PROP, null);
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).formatDetail(testRecord, dtl);
            checkFormattedDetail("AuditDetail-4: null test", dtl, logStr);

            setCustomTestFormat(TEST_PROP, "");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(null).formatDetail(testRecord, dtl);
            assertNull("AuditDetail-5: empty property, expecting null", logStr);

            // tests with cluster properties
            setCustomTestFormat(TEST_PROP, "detail-ctx: ${requestId}, ${strvar1}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).formatDetail(testRecord, dtl);
            checkFormattedLog("AuditDetail-6: ctx vars", "detail-ctx: reqId-00002, abc123", logStr);

            setCustomTestFormat(TEST_PROP, "detail-ctx: ${arrayVar1}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).formatDetail(testRecord, dtl);
            checkFormattedLog("AuditDetail-7: ctx vars", "detail-ctx: a1, a2, a3", logStr);

            setCustomTestFormat(TEST_PROP, "detail-ctx: ${requestId}, ignore=${ignoreVar}");
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).formatDetail(testRecord, dtl);
            checkFormattedLog("AuditDetail-8: ctx vars", "detail-ctx: reqId-00002, ignore=", logStr);

            setCustomTestFormat(TEST_PROP, BAD_FMT_TEMPLATE);
            logStr = new TestingAuditLogFormatter<MessageSummaryAuditRecord>(ctxMap).formatDetail(testRecord, dtl);
            checkFormattedLog("AuditDetail-9: ctx vars, bad template",
                    "bad tags template. >< "+mid+" "+mmsg+" "+new Goid(0,393216L).toHexString()+" >< Warehoust [/wh2] {4} {1011} >< >< >< >< $ section 2. >< >< >< >< >< >< >< >< >< >< section 3. >< >< >< >< >< >< >< ><",
                    logStr);

        } finally {
            TestingAuditLogFormatter.setTestingConfig(null);
            TestingAuditLogFormatter.notifyPropertyChange(TEST_PROP);
        }
    }

    @Test
    public void testSystemAudit() {

        final String TEST_PROP = ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_OTHER;

        TestingAuditLogFormatter.setTestingConfig( config );
        try {
            pec.close();

            SystemAuditRecord testRecord = sysAuditGenerator.createAuditRecord();
            String logStr = new TestingAuditLogFormatter<SystemAuditRecord>(null).format(testRecord);
            checkFormattedLog("SystemAudit: default", testRecord, logStr);

            // test with custom properties
            setCustomTestFormat(TEST_PROP, "{1}");
            logStr = new TestingAuditLogFormatter<SystemAuditRecord>(null).format(testRecord);
            checkFormattedLog("SystemAudit-1: valid parms", testRecord, logStr);

            setCustomTestFormat(TEST_PROP, "sysaudit: {1};ignore={0}{2}{3};unchanged={4}{8}{16}{32}");
            logStr = new TestingAuditLogFormatter<SystemAuditRecord>(null).format(testRecord);
            checkFormattedLog("SystemAudit-2: all parms", "sysaudit: "+testRecord.getMessage()+";ignore=;unchanged={4}{8}{16}{32}", logStr);

            setCustomTestFormat(TEST_PROP, "Just plain text");
            logStr = new TestingAuditLogFormatter<SystemAuditRecord>(null).format(testRecord);
            checkFormattedLog("SystemAudit-3: all parms", "Just plain text", logStr);

            setCustomTestFormat(TEST_PROP, null);
            logStr = new TestingAuditLogFormatter<SystemAuditRecord>(null).format(testRecord);
            checkFormattedLog("SystemAudit-4: null test", testRecord, logStr);

            setCustomTestFormat(TEST_PROP, "");
            logStr = new TestingAuditLogFormatter<SystemAuditRecord>(null).format(testRecord);
            assertNull("SystemAudit-5: empty property, expecting null", logStr);

            // tests with cluster properties
            setCustomTestFormat(TEST_PROP, "test ctx ${requestId}, ${strvar1}, ${arrayVar1}");
            logStr = new TestingAuditLogFormatter<SystemAuditRecord>(ctxMap).format(testRecord);
            checkFormattedLog("SystemAudit-6: ctx vars", "test ctx , , ", logStr);

            setCustomTestFormat(TEST_PROP, BAD_FMT_TEMPLATE);
            logStr = new TestingAuditLogFormatter<SystemAuditRecord>(ctxMap).format(testRecord);
            checkFormattedLog("SystemAudit-7: ctx vars, bad template",
                    "bad tags template. ><  "+testRecord.getMessage()+"  ><  {4} {1011} >< >< >< >< $ section 2. >< >< >< >< >< >< >< >< >< >< section 3. >< >< >< >< >< >< >< ><",
                    logStr);

        } finally {
            TestingAuditLogFormatter.setTestingConfig(null);
            TestingAuditLogFormatter.notifyPropertyChange(TEST_PROP);
        }
    }

    @Test
    public void testAdminAudit() {

        final String TEST_PROP = ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_OTHER;

        TestingAuditLogFormatter.setTestingConfig( config );
        try {
            pec.close();

            AdminAuditRecord testRecord = adminAuditGenerator.createAuditRecord();
            String logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).format(testRecord);
            checkFormattedLog("AdminAudit: default", testRecord, logStr);

            // test with custom properties
            setCustomTestFormat(TEST_PROP, "{1}");
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).format(testRecord);
            checkFormattedLog("AdminAudit-1: valid parms", testRecord, logStr);

            setCustomTestFormat(TEST_PROP, "adminAudit: {1};ignore={0}{2}{3};unchanged={4}{8}{16}{32}");
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).format(testRecord);
            checkFormattedLog("AdminAudit-2: all parms", "adminAudit: "+testRecord.getMessage()+";ignore=;unchanged={4}{8}{16}{32}", logStr);

            setCustomTestFormat(TEST_PROP, "Just plain text");
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).format(testRecord);
            checkFormattedLog("AdminAudit-3: all parms", "Just plain text", logStr);

            setCustomTestFormat(TEST_PROP, null);
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).format(testRecord);
            checkFormattedLog("AdminAudit-4: null test", testRecord, logStr);

            setCustomTestFormat(TEST_PROP, "");
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).format(testRecord);
            assertNull("AdminAudit-5: empty property, expecting null", logStr);

            // tests with cluster properties
            setCustomTestFormat(TEST_PROP, "test ctx ${requestId}, ${strvar1}, ${arrayVar1}");
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(ctxMap).format(testRecord);
            checkFormattedLog("AdminAudit-6: ctx vars", "test ctx , , ", logStr);

            setCustomTestFormat(TEST_PROP, BAD_FMT_TEMPLATE);
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(ctxMap).format(testRecord);
            checkFormattedLog("AdminAudit-7: ctx vars, bad template",
                    "bad tags template. ><  "+testRecord.getMessage()+"  ><  {4} {1011} >< >< >< >< $ section 2. >< >< >< >< >< >< >< >< >< >< section 3. >< >< >< >< >< >< >< ><",
                    logStr);

        } finally {
            TestingAuditLogFormatter.setTestingConfig(null);
            TestingAuditLogFormatter.notifyPropertyChange(TEST_PROP);
        }
    }

    @Test
    public void testOtherAuditDetail() {

        final String TEST_PROP = ServerConfigParams.PARAM_AUDIT_LOG_FORMAT_OTHER_DETAIL;

        TestingAuditLogFormatter.setTestingConfig( config );
        try {
            pec.close();

            AdminAuditRecord testRecord = adminAuditGenerator.createAuditRecord();
            AuditDetailMessage dtl = adminAuditGenerator.createAuditDetail(2013);
            String logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).formatDetail(testRecord, dtl);
            checkFormattedDetail("testAdminAuditDetail()", dtl, logStr);

            int mid = dtl.getId();
            String mmsg = dtl.getMessage();

            // test with custom properties
            setCustomTestFormat(TEST_PROP, "{0}: {1}");
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).formatDetail(testRecord, dtl);
            checkFormattedDetail("AdminDetail-1: valid parms", dtl, logStr);

//            setCustomTestFormat(TEST_PROP, "{0}: {1} {2} - don't have a cow man two {2} 3 {3} ignore {4}");
            setCustomTestFormat(TEST_PROP, "detail: {0};{1};ignore={2}{3};unchanged={4}{8}{16}{32}");
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).formatDetail(dtl);
            checkFormattedLog("AdminDetail-2: all parms", "detail: "+mid+";"+mmsg+";ignore=;unchanged={4}{8}{16}{32}", logStr);

            setCustomTestFormat(TEST_PROP, "Just plain text");
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).formatDetail(testRecord, dtl);
            checkFormattedLog("AdminDetail-3: all parms", "Just plain text", logStr);

            setCustomTestFormat(TEST_PROP, null);
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).formatDetail(testRecord, dtl);
            checkFormattedDetail("AdminDetail-4: null test", dtl, logStr);

            setCustomTestFormat(TEST_PROP, "");
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(null).formatDetail(testRecord, dtl);
            assertNull("AdminDetail-5: empty property, expecting null", logStr);

            // tests with cluster properties
            setCustomTestFormat(TEST_PROP, "detail-ctx: ${requestId}, ${strvar1}, ${dummyVar}");
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(ctxMap).formatDetail(testRecord, dtl);
            checkFormattedLog("AdminDetail-6: ctx vars", "detail-ctx: , , ", logStr);

            setCustomTestFormat(TEST_PROP, BAD_FMT_TEMPLATE);
            logStr = new TestingAuditLogFormatter<AdminAuditRecord>(ctxMap).formatDetail(testRecord, dtl);
            checkFormattedLog("AdminDetail-7: ctx vars, bad template",
                    "bad tags template. >< "+mid+" "+mmsg+"  ><  {4} {1011} >< >< >< >< $ section 2. >< >< >< >< >< >< >< >< >< >< section 3. >< >< >< >< >< >< >< ><",
                    logStr);

        } finally {
            TestingAuditLogFormatter.setTestingConfig(null);
            TestingAuditLogFormatter.notifyPropertyChange(TEST_PROP);
        }
    }

    private void setCustomTestFormat(String propKey, String newFormat) {
        if (cfgMap.containsKey(propKey)) {
            cfgMap.remove(propKey);
        }
        cfgMap.put(propKey, newFormat);
        TestingAuditLogFormatter.notifyPropertyChange(propKey);
    }

    private void checkFormattedLog(String testCase, String expected, String actual) {
        assertNotNull("test: "+testCase, actual);
        assertEquals("test: "+testCase, expected, actual);
    }

    private void checkFormattedLog(String testCase, AuditRecord expected, String actual) {
        assertNotNull("test: "+testCase, actual);
        assertEquals("test: "+testCase, expected.getMessage(), actual);
    }

    private void checkFormattedDetail(String testCase, AuditDetailMessage expected, String actual) {
        assertNotNull("test: "+testCase, actual);
        assertEquals("test: "+testCase, expected.getId() + ": " + expected.getMessage(), actual);
    }

    protected class MessageSummaryAuditGen extends AuditGen<MessageSummaryAuditRecord> {

        MessageSummaryAuditRecord createAuditRecord() {
            return new MessageSummaryAuditRecord(
                    LEVEL,
                    NODE_ID,
                    getRequestId(),
                    ASN_STATUS,
                    CLIENT_ADDR,
                    null, 4096,
                    null, 4096,
                    HTTP_RESP, LATENCY,
                    SERVICE_GOID, SERVICE_NAME,
                    null,
                    true,
                    SECURITY_TOKEN,
                    IDENTITY_PROV_OID,
                    USER_NAME, USER_ID,
                    new Functions.Nullary<Goid>() {
                        @Override
                        public Goid call() {
                            return new Goid(0, 3);
                        }
                    });
        }
    }

    protected class SystemAuditGen extends AuditGen<SystemAuditRecord> {

        SystemAuditRecord createAuditRecord() {
            return new SystemAuditRecord(
                    LEVEL,
                    NODE_ID,
                    Component.GATEWAY,
                    "System Audit Generated Message: " + System.currentTimeMillis(),
                    true,
                    IDENTITY_PROV_OID,
                    USER_NAME,
                    USER_ID,
                    "SomeAction",
                    CLIENT_ADDR);
        }
    }

    protected class AdminAuditGen extends AuditGen<AdminAuditRecord> {

        AdminAuditRecord createAuditRecord() {
            return new AdminAuditRecord(
                    LEVEL,
                    NODE_ID,
                    33333L,
                    AdminAuditGen.class.getName(),
                    "AuditName",
                    'L',
                    "Admin Audit Generated Message: " + System.currentTimeMillis(),
                    IDENTITY_PROV_OID,
                    ADMIN_NAME,
                    ADMIN_ID,
                    CLIENT_ADDR);
        }
    }

    protected class AuditGen<REC extends AuditRecord> {

        final Level LEVEL = Level.INFO;
        final String NODE_ID = "test-node-1";
        final AssertionStatus ASN_STATUS = AssertionStatus.NONE;
        final String CLIENT_ADDR = "192.168.1.144";
        final int HTTP_RESP = 200; // OK
        final int LATENCY = 1234;
        final Goid SERVICE_GOID = new Goid(0,393216L);
        final String SERVICE_NAME = "Warehoust [/wh2]";

        final SecurityTokenType SECURITY_TOKEN = SecurityTokenType.HTTP_BASIC;
        final Goid IDENTITY_PROV_OID = new Goid(0,-2L);

        final String USER_NAME = "Lance Uppercut";
        final String USER_ID = "luppercut";
        final String ADMIN_NAME = "Administrator";
        final String ADMIN_ID = "admin";

        protected String getRequestId() {
            return "req" + System.currentTimeMillis();
        }

        protected AuditDetailMessage createAuditDetail(int did) {
            return MessagesUtil.getAuditDetailMessageById(did);
        }
    }


    /**
     * Testing version of formatter that permits the tests to override the formatting cluster properties.
     */
    protected static class TestingAuditLogFormatter<AREC extends AuditRecord> extends AuditLogFormatter<AREC> {

        public TestingAuditLogFormatter(Map<String, Object> ctxVariablesMap) {
            super(ctxVariablesMap);
        }

        // lets us override the properties in ServerConfig
        static void setTestingConfig( final Config config ) {
            AuditLogFormatter.config = config;
        }
    }
}
