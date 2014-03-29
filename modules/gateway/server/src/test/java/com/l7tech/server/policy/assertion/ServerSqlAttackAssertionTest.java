package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test coverage of SQL Attack Protection Assertion
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 * @version 1.2 2014/03/28
 */
public class ServerSqlAttackAssertionTest {

    // protection types
    private static final String META = "SqlMeta";
    private static final String META_TEXT = "SqlMetaText";
    private static final String MS_SQL = "MsSql";
    private static final String ORA_SQL = "OraSql";
    private static final String UNRECOGNIZED_PATTERN = "ThisPatternShouldNotExist";

    // URL components
    private static final String BENIGN_URL_PATH = "/path/to/resource";
    private static final String BENIGN_URL_QUERY_STRING = "var1=val1&var2=val2";

    // message resources
    private static final String VALID_SOAP_REQUEST = "ValidListProductsRequestSOAPMessage.xml";
    private static final String VALID_SOAP_RESPONSE = "ValidEchoResponseSOAPMessage.xml";
    private static final String INVASIVE_SQL_DOUBLE_DASH = "SqlAttack_InvasiveSql_DoubleDash.xml";
    private static final String INVASIVE_SQL_HASH_MARK = "SqlAttack_InvasiveSql_HashMark.xml";
    private static final String INVASIVE_SQL_SINGLE_QUOTE = "SqlAttack_InvasiveSql_SingleQuote.xml";
    private static final String MS_SQL_EXPLOIT_EXEC_SP = "SqlAttack_MsSqlServerExploit_sp.xml";
    private static final String MS_SQL_EXPLOIT_EXEC_XP = "SqlAttack_MsSqlServerExploit_xp.xml";
    private static final String ORACLE_EXPLOIT_BFILENAME = "SqlAttack_OracleExploit_bfilename.xml";
    private static final String ORACLE_EXPLOIT_OFFSET = "SqlAttack_OracleExploit_offset.xml";
    private static final String ORACLE_EXPLOIT_TIMESTAMP = "SqlAttack_OracleExploit_timestamp.xml";
    private static final String STANDARD_SQL_CDATA_DOUBLE_DASH = "SqlAttack_StandardSql_CdataDoubleDash.xml";
    private static final String STANDARD_SQL_CDATA_HASH_MARK = "SqlAttack_StandardSql_CdataHashMark.xml";
    private static final String STANDARD_SQL_CDATA_SINGLE_QUOTE = "SqlAttack_StandardSql_CdataSingleQuote.xml";
    private static final String STANDARD_SQL_ELEMENT_DOUBLE_DASH = "SqlAttack_StandardSql_ElementDoubleDash.xml";
    private static final String STANDARD_SQL_ELEMENT_HASH_MARK = "SqlAttack_StandardSql_ElementHashMark.xml";
    private static final String STANDARD_SQL_ELEMENT_SINGLE_QUOTE = "SqlAttack_StandardSql_ElementSingleQuote.xml";
    private static final String STANDARD_SQL_ATTACK_RESPONSE_MESSAGE =
            "SqlAttack_StandardSql_CdataHashMark_EchoResponseSOAPMessage.xml";

    private StashManager stashManager;
    private TestAudit testAudit;
    private SecurityManager originalSecurityManager;

    @Before
    public void setUp() {
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
        StashManagerFactory factory = (StashManagerFactory) applicationContext.getBean("stashManagerFactory");
        stashManager = factory.createStashManager();

        testAudit = new TestAudit();
        originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new GatewayPermissiveLoggingSecurityManager());
    }

    @After
    public void tearDown() throws Exception {
        System.setSecurityManager(originalSecurityManager);
    }

    // Message body tests

    /**
     * Valid benign request message, test against all protection types, should pass without issue.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_ValidSOAPRequest_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, VALID_SOAP_REQUEST, META, META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Valid benign response message, test against all protection types, should pass without issue.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_ValidSOAPResponse_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.RESPONSE, VALID_SOAP_RESPONSE, META, META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Empty request message target, test against all protection types, should pass without issue.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_EmptyBodyInRequestTarget_AssertionStatusNone() throws Exception {
        SqlAttackAssertion assertion =
                createAssertion(TargetMessageType.REQUEST, false, false, true, META, META_TEXT, ORA_SQL, MS_SQL);
        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        Message request = createRequest(HttpMethod.POST, null, null, "", ContentTypeHeader.TEXT_DEFAULT);

        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, createResponse());

        final AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Double dash characters in namespace declaration - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveDoubleDashInNamespaceCaughtByMETA_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, INVASIVE_SQL_DOUBLE_DASH, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Double dash characters in namespace declaration - should not be caught by protections other than META.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveDoubleDashInNamespaceMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, INVASIVE_SQL_DOUBLE_DASH, META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Hash mark character in namespace declaration - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveHashMarkInNamespaceCaughtByMETA_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, INVASIVE_SQL_HASH_MARK, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Hash mark character in namespace declaration - should not be caught by protections other than META.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveHashMarkInNamespaceMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, INVASIVE_SQL_HASH_MARK, META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Single quote character in namespace declaration - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSingleQuoteInNamespaceCaughtByMETA_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, INVASIVE_SQL_SINGLE_QUOTE, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Single quote character in namespace declaration - should not be caught by protections other than META.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSingleQuoteInNamespaceMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, INVASIVE_SQL_SINGLE_QUOTE, META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Double dash characters in CDATA section in context variable - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardDoubleDashInCDATAInContextVariableCaughtByMETATEXT_AssertionStatusBadRequest()
            throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.OTHER,
                        STANDARD_SQL_CDATA_DOUBLE_DASH, META);

        assertEquals(AssertionStatus.FALSIFIED, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Double dash characters in CDATA section - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardDoubleDashInCDATACaughtByMETA_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_CDATA_DOUBLE_DASH, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Double dash characters in CDATA section - should be caught by META_TEXT protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardDoubleDashInCDATACaughtByMETATEXT_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_CDATA_DOUBLE_DASH, META_TEXT);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Double dash characters in CDATA section - should not be caught by protections other than META or META_TEXT.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardDoubleDashInCDATAMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST,
                        STANDARD_SQL_CDATA_DOUBLE_DASH, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Hash mark character in CDATA section - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardHashMarkInCDATACaughtByMETA_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_CDATA_HASH_MARK, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Hash mark character in CDATA section - should be caught by META_TEXT protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardHashMarkInCDATACaughtByMETATEXT_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_CDATA_HASH_MARK, META_TEXT);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Hash mark character in CDATA section - should not be caught by protections other than META or META_TEXT.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardHashMarkInCDATAMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST,
                        STANDARD_SQL_CDATA_HASH_MARK, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Single quote character in CDATA section - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardSingleQuoteInCDATACaughtByMETA_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_CDATA_SINGLE_QUOTE, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Single quote character in CDATA section - should be caught by META_TEXT protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardSingleQuoteInCDATACaughtByMETATEXT_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_CDATA_SINGLE_QUOTE, META_TEXT);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Single quote character in CDATA section - should not be caught by protections other than META or META_TEXT.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardSingleQuoteInCDATAMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST,
                        STANDARD_SQL_CDATA_SINGLE_QUOTE, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Double dash characters in an Element - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardDoubleDashInElementCaughtByMETA_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_ELEMENT_DOUBLE_DASH, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Double dash characters in an Element - should be caught by META_TEXT protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardDoubleDashInElementCaughtByMETATEXT_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_ELEMENT_DOUBLE_DASH, META_TEXT);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Double dash characters in an Element - should not be caught by protections other than META or META_TEXT.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardDoubleDashInElementMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST,
                        STANDARD_SQL_ELEMENT_DOUBLE_DASH, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Hash mark character in an Element - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardHashMarkInElementCaughtByMETA_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_ELEMENT_HASH_MARK, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Hash mark character in an Element - should be caught by META_TEXT protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardHashMarkInElementCaughtByMETATEXT_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_ELEMENT_HASH_MARK, META_TEXT);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Hash mark character in an Element - should not be caught by protections other than META or META_TEXT.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardHashMarkInElementMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST,
                        STANDARD_SQL_ELEMENT_HASH_MARK, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Single quote character in an Element - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardSingleQuoteInElementCaughtByMETA_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_ELEMENT_SINGLE_QUOTE, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Single quote character in an Element - should be caught by META_TEXT protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardSingleQuoteInElementCaughtByMETATEXT_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, STANDARD_SQL_ELEMENT_SINGLE_QUOTE, META_TEXT);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Single quote character in an Element - should not be caught by protections other than META or META_TEXT.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardSingleQuoteInElementMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST,
                        STANDARD_SQL_ELEMENT_SINGLE_QUOTE, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Message contains MS SQL Server 'exec sp_' exploit - should be caught by MS_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecSPExploitCaughtByMSSQL_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, MS_SQL_EXPLOIT_EXEC_SP, MS_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Message contains MS SQL Server 'exec sp_' exploit - should not be caught by protections other than MS_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecSPExploitMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, MS_SQL_EXPLOIT_EXEC_SP, META, META_TEXT, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Message contains MS SQL Server 'exec xp_' exploit - should be caught by MS_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecXPExploitCaughtByMSSQL_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, MS_SQL_EXPLOIT_EXEC_XP, MS_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Message contains MS SQL Server 'exec xp_' exploit - should not be caught by protections other than MS_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecXPExploitMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, MS_SQL_EXPLOIT_EXEC_XP, META, META_TEXT, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Message contains Oracle 'bfilename' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleBfilenameExploit_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, ORACLE_EXPLOIT_BFILENAME, ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Message contains Oracle 'bfilename' exploit - should not be caught by protections other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleBfilenameExploitMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, ORACLE_EXPLOIT_BFILENAME, META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Message contains Oracle 'offset' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleOffsetExploit_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, ORACLE_EXPLOIT_OFFSET, ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Message contains Oracle 'offset' exploit - should not be caught by protections other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleOffsetExploitMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, ORACLE_EXPLOIT_OFFSET, META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Message contains Oracle 'timestamp' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleTimestampExploit_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, ORACLE_EXPLOIT_TIMESTAMP, ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Message contains Oracle 'timestamp' exploit - should not be caught by protections other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleTimestampExploitMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.REQUEST, ORACLE_EXPLOIT_TIMESTAMP, META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Hash mark character in CDATA section of SOAP response message - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardAttackInResponseCaughtByMETA_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.RESPONSE, STANDARD_SQL_ATTACK_RESPONSE_MESSAGE, META);

        assertEquals(AssertionStatus.BAD_RESPONSE, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Hash mark character in CDATA section of SOAP response message - should be caught by META_TEXT protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardAttackInResponseCaughtByMETATEXT_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.RESPONSE, STANDARD_SQL_ATTACK_RESPONSE_MESSAGE, META_TEXT);

        assertEquals(AssertionStatus.BAD_RESPONSE, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Hash mark character in CDATA section of SOAP response message - should not be caught by protections other
     * than META or META_TEXT.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_StandardAttackInResponseMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestWithResource(TargetMessageType.RESPONSE, STANDARD_SQL_ATTACK_RESPONSE_MESSAGE, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    // Request URL Query String tests

    /**
     * Target is a Context Variable but only includeQueryString is specified - SQLATTACK_NOT_HTTP should be logged and
     * the URL Query String should not be scanned, but the assertion should succeed.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_IncludeUrlQueryStringForVariableTarget_NotHttpLogged_AssertionStatusNone() throws Exception {
        final String varName = "testInput";

        SqlAttackAssertion assertion = createAssertion(TargetMessageType.OTHER, false, true, false, META);
        assertion.setOtherTargetMessageVariable(varName);

        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = createPolicyEnforcementContext(TargetMessageType.OTHER, null);
        context.setVariable(varName, createMessageFromXmlResource(INVASIVE_SQL_DOUBLE_DASH));

        final AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_NOT_HTTP));

        checkAuditPresence(false, false, false, false, false, false, false);
    }

    /**
     * Request URL Query String contains double dash characters - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveDoubleDashInUrlQueryStringCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl(BENIGN_URL_PATH, "input=data--", false, true, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, false, true, false, false, true, true);
    }

    /**
     * Request URL Query String contains double dash characters - should not be caught by protections
     * other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveDoubleDashInUrlQueryStringMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl(BENIGN_URL_PATH, "input=data--", false, true, META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, false, true, false, false, false, false);
    }

    /**
     * Request URL Query String contains hash mark character - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveHashMarkInUrlQueryStringCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl(BENIGN_URL_PATH, "input=dat#a", false, true, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, false, true, false, false, true, true);
    }

    /**
     * Request URL Query String contains hash mark character - should not be caught by protections other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveHashMarkInUrlQueryStringMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl(BENIGN_URL_PATH, "input=dat#a", false, true, META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, false, true, false, false, false, false);
    }

    /**
     * Request URL Query String contains single quote character - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSingleQuoteInUrlQueryStringCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl(BENIGN_URL_PATH, "input=';DROP%20TABLE%20USERS;'", false, true, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, false, true, false, false, true, true);
    }

    /**
     * Request URL Query String contains single quote character - should not be caught by protections
     * other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSingleQuoteInUrlQueryStringMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl(BENIGN_URL_PATH, "input=';DROP%20TABLE%20USERS;'",
                false, true, META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, false, true, false, false, false, false);
    }

    /**
     * Request URL Query String contains Oracle 'bfilename' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleBfilenameExploitInUrlQueryStringCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl(BENIGN_URL_PATH, "input=bfilename", false, true, ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, false, true, false, false, true, true);
    }

    /**
     * Request URL Query String contains Oracle 'bfilename' exploit - should not be caught by protections
     * other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleBfilenameExploitInUrlQueryStringMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl(BENIGN_URL_PATH, "input=bfilename", false, true, META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, false, true, false, false, false, false);
    }

    /**
     * Request URL Query String contains Oracle 'offset' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleOffsetExploitInUrlQueryStringCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl(BENIGN_URL_PATH, "input=tz_offset", false, true, ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, false, true, false, false, true, true);
    }

    /**
     * Request URL Query String contains Oracle 'offset' exploit - should not be caught by protections
     * other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleOffsetExploitInUrlQueryStringMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl(BENIGN_URL_PATH, "input=tz_offset", false, true, META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, false, true, false, false, false, false);
    }

    /**
     * Request URL Query String contains Oracle 'timestamp' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleTimestampExploitInUrlQueryStringCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl(BENIGN_URL_PATH, "input=to_timestamp_tz", false, true, ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, false, true, false, false, true, true);
    }

    /**
     * Request URL Query String contains Oracle 'timestamp' exploit - should not be caught by protections
     * other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleTimestampExploitInUrlQueryStringMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl(BENIGN_URL_PATH, "input=to_timestamp_tz",
                false, true, META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, false, true, false, false, false, false);
    }

    /**
     * Request URL Query String contains MS SQL Server 'exec sp_' exploit - should be caught by MS_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecSPExploitInUrlQueryStringCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl(BENIGN_URL_PATH, "input=exec%20sp_dropextendedproc", false, true, MS_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, false, true, false, false, true, true);
    }

    /**
     * Request URL Query String contains MS SQL Server 'exec sp_' exploit - should not be caught by protections
     * other than MS_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecSPExploitInUrlQueryStringMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl(BENIGN_URL_PATH, "input=exec%20sp_dropextendedproc",
                false, true, META, META_TEXT, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, false, true, false, false, false, false);
    }

    /**
     * Request URL Query String contains MS SQL Server 'exec xp_' exploit - should be caught by MS_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecXPExploitInUrlQueryStringCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl(BENIGN_URL_PATH, "input=exec%20xp_smtp_sendmail", false, true, MS_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, false, true, false, false, true, true);
    }

    /**
     * Request URL Query String contains MS SQL Server 'exec xp_' exploit - should not be caught by protections
     * other than MS_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecXPExploitInUrlQueryStringMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl(BENIGN_URL_PATH, "input=exec%20xp_smtp_sendmail",
                false, true, META, META_TEXT, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, false, true, false, false, false, false);
    }

    // Request URL Path tests

    /**
     * Target is a Context Variable but only includePath is specified - SQLATTACK_NOT_HTTP should be logged and the path
     * should not be scanned, but the assertion should succeed.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_IncludeUrlPathForVariableTarget_NotHttpLogged_AssertionStatusNone() throws Exception {
        final String varName = "testInput";

        SqlAttackAssertion assertion = createAssertion(TargetMessageType.OTHER, true, false, false, META);
        assertion.setOtherTargetMessageVariable(varName);

        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = createPolicyEnforcementContext(TargetMessageType.OTHER, null);
        context.setVariable(varName, createMessageFromXmlResource(INVASIVE_SQL_DOUBLE_DASH));

        final AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_NOT_HTTP));

        checkAuditPresence(false, false, false, false, false, false, false);
    }

    /**
     * Request URL Path contains double dash characters - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveDoubleDashInUrlPathCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl("/path/to/re--source", BENIGN_URL_QUERY_STRING, true, false, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, true, false, false, true, false, true);
    }

    /**
     * Request URL Path contains double dash characters - should not be caught by protections
     * other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveDoubleDashInUrlPathMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/path/to/re--source", BENIGN_URL_QUERY_STRING,
                true, false, META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, true, false, false, false, false, false);
    }

    /**
     * Request URL Path contains hash mark character - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveHashMarkInUrlPathCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl("/p#ath/to/resource", BENIGN_URL_QUERY_STRING, true, false, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, true, false, false, true, false, true);
    }

    /**
     * Request URL Path contains hash mark character - should not be caught by protections other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveHashMarkInUrlPathMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/p#ath/to/resource", BENIGN_URL_QUERY_STRING,
                true, false, META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, true, false, false, false, false, false);
    }

    /**
     * Request URL Path contains single quote character - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSingleQuoteInUrlPathCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/path/to/re';DROP%20TABLE%20USERS;'source",
                BENIGN_URL_QUERY_STRING, true, false, META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, true, false, false, true, false, true);
    }

    /**
     * Request URL Path contains single quote character - should not be caught by protections
     * other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSingleQuoteInUrlPathMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/path/to/re';DROP%20TABLE%20USERS;'source",
                BENIGN_URL_QUERY_STRING, true, false, META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, true, false, false, false, false, false);
    }

    /**
     * Request URL Path contains Oracle 'bfilename' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleBfilenameExploitInUrlPathCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl("/path/to/re+bfilename+source", BENIGN_URL_QUERY_STRING, true, false, ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, true, false, false, true, false, true);
    }

    /**
     * Request URL Path contains Oracle 'bfilename' exploit - should not be caught by protections
     * other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleBfilenameExploitInUrlPathMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/path/to/re+bfilename+source", BENIGN_URL_QUERY_STRING,
                true, false, META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, true, false, false, false, false, false);
    }

    /**
     * Request URL Path contains Oracle 'offset' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleOffsetExploitInUrlPathCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl("/path/to/resource+tz_offset",
                        BENIGN_URL_QUERY_STRING, true, false, ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, true, false, false, true, false, true);
    }

    /**
     * Request URL Path contains Oracle 'offset' exploit - should not be caught by protections
     * other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleOffsetExploitInUrlPathMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/path/to/resource+tz_offset",
                BENIGN_URL_QUERY_STRING, true, false, META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, true, false, false, false, false, false);
    }

    /**
     * Request URL Path contains Oracle 'timestamp' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleTimestampExploitInUrlPathCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/path/to/resource+to_timestamp_tz",
                BENIGN_URL_QUERY_STRING, true, false, ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, true, false, false, true, false, true);
    }

    /**
     * Request URL Path contains Oracle 'timestamp' exploit - should not be caught by protections
     * other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleTimestampExploitInUrlPathMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/path/to/resource+to_timestamp_tz",
                BENIGN_URL_QUERY_STRING, true, false, META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, true, false, false, false, false, false);
    }

    /**
     * Request URL Path contains MS SQL Server 'exec sp_' exploit - should be caught by MS_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecSPExploitInUrlPathCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/path/to/resource+exec+sp_dropextendedproc",
                BENIGN_URL_QUERY_STRING, true, false, MS_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, true, false, false, true, false, true);
    }

    /**
     * Request URL Path contains MS SQL Server 'exec sp_' exploit - should not be caught by protections
     * other than MS_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecSPExploitInUrlPathMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/path/to/resource+exec+sp_dropextendedproc",
                BENIGN_URL_QUERY_STRING, true, false, META, META_TEXT, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, true, false, false, false, false, false);
    }

    /**
     * Request URL Path contains MS SQL Server 'exec xp_' exploit - should be caught by MS_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecXPExploitInUrlPathCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/path/to/resource+exec+xp_smtp_sendmail",
                BENIGN_URL_QUERY_STRING, true, false, MS_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, true, false, false, true, false, true);
    }

    /**
     * Request URL Path contains MS SQL Server 'exec xp_' exploit - should not be caught by protections
     * other than MS_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecXPExploitInUrlPathMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("/path/to/resource+exec+xp_smtp_sendmailsource",
                BENIGN_URL_QUERY_STRING, true, false, META, META_TEXT, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(false, true, false, false, false, false, false);
    }

    // Scan all target message components test

    /**
     * Valid benign request message, test all components against all protection types, should pass without issue.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_IncludeAllComponentsForRequestTarget_AssertionStatusNone() throws Exception {
        SqlAttackAssertion assertion =
                createAssertion(TargetMessageType.REQUEST, true, true, true, META, META_TEXT, ORA_SQL, MS_SQL);
        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        Message request = createRequest(HttpMethod.POST, BENIGN_URL_PATH, BENIGN_URL_QUERY_STRING,
                new String(IOUtils.slurpStream(getClass().getResourceAsStream(VALID_SOAP_REQUEST)), Charsets.UTF8),
                ContentTypeHeader.XML_DEFAULT);

        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, createResponse());

        final AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, true, true, false, false, false, false);
    }

    // Context variable tests

    /**
     * Context variable contains XML with invasive SQL - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSqlInContextVariableTargetCaught_AssertionStatusFalsified() throws Exception {
        final String varName = "testInput";

        SqlAttackAssertion assertion = createAssertion(TargetMessageType.OTHER, false, false, true, META);
        assertion.setOtherTargetMessageVariable(varName);

        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = createPolicyEnforcementContext(TargetMessageType.OTHER, null);
        context.setVariable(varName, createMessageFromXmlResource(INVASIVE_SQL_DOUBLE_DASH));

        final AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FALSIFIED, status);

        checkAuditPresence(true, false, false, true, false, false, true);
    }

    /**
     * Context variable contains XML with invasive SQL - should not be caught by protections other than META.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSqlInContextVariableTargetMissed_AssertionStatusNone() throws Exception {
        final String varName = "testInput";

        SqlAttackAssertion assertion = createAssertion(TargetMessageType.OTHER, false, false, true,
                META_TEXT, MS_SQL, ORA_SQL);
        assertion.setOtherTargetMessageVariable(varName);

        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = createPolicyEnforcementContext(TargetMessageType.OTHER, null);
        context.setVariable(varName, createMessageFromXmlResource(INVASIVE_SQL_DOUBLE_DASH));

        final AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);

        checkAuditPresence(true, false, false, false, false, false, false);
    }

    /**
     * Context variable contains XML with invasive SQL - should not be caught by protections other than META.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_UnrecognizedProtectionType_AssertionStatusFailed() throws Exception {
        try {
            runTestWithResource(TargetMessageType.RESPONSE,
                    STANDARD_SQL_ATTACK_RESPONSE_MESSAGE, UNRECOGNIZED_PATTERN);
            fail();
        } catch (AssertionStatusException e) {
            assertEquals(AssertionStatus.FAILED, e.getAssertionStatus());
            assertEquals("Unrecognized protection pattern.", e.getMessage());
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_UNRECOGNIZED_PROTECTION));
    }

    // Routing check tests

    /**
     * Check is performed on REQUEST, post-routing - the server should return a FAILED status.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_REQUESTMessagePostRouting_AssertionStatusFailed() throws Exception {
        SqlAttackAssertion assertion = createAssertion(TargetMessageType.REQUEST, false, false, true, META);
        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        Message request = createRequest();
        Message response = createResponse();

        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        context.setRoutingStatus(RoutingStatus.ROUTED);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_ALREADY_ROUTED));
    }

    /**
     * Check is performed on RESPONSE, not routed - the server should return a status of NONE.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_RESPONSEMessageNotRouted_AssertionStatusNone() throws Exception {
        SqlAttackAssertion assertion = createAssertion(TargetMessageType.RESPONSE, false, false, true, META);
        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        Message request = createRequest();
        Message response = createResponse();

        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        context.setRoutingStatus(RoutingStatus.NONE);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_SKIP_RESPONSE_NOT_ROUTED));
    }

    // Helper methods

    private AssertionStatus runTestWithResource(TargetMessageType targetType, String resource, String... protections)
            throws IOException, PolicyAssertionException, SAXException {
        String contextVariableName = "testMessage";
        Message otherTargetMessage = null;

        SqlAttackAssertion assertion = createAssertion(targetType, false, false, true, protections);

        if (TargetMessageType.OTHER == targetType) {
            otherTargetMessage = createMessageFromXmlResource(resource);
            assertion.setOtherTargetMessageVariable(contextVariableName);
        }

        ServerSqlAttackAssertion serverAssertion = createServer(assertion);
        final PolicyEnforcementContext context = createPolicyEnforcementContext(targetType, resource);

        if (TargetMessageType.OTHER == targetType) {
            context.setVariable(contextVariableName, otherTargetMessage);
        }

        return serverAssertion.checkRequest(context);
    }

    private AssertionStatus runTestOnRequestUrl(String urlPath, String urlQueryString,
                                                boolean includePath, boolean includeQueryString,
                                                String... protections)
            throws IOException, PolicyAssertionException, SAXException {
        SqlAttackAssertion assertion =
                createAssertion(TargetMessageType.REQUEST, includePath, includeQueryString, false, protections);
        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        Message request = createRequest(HttpMethod.GET, urlPath, urlQueryString, "", ContentTypeHeader.TEXT_DEFAULT);

        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, createResponse());

        return serverAssertion.checkRequest(context);
    }

    private SqlAttackAssertion createAssertion(TargetMessageType targetMessageType, boolean includeUrlPath,
                                               boolean includeUrlQueryString, boolean includeBody,
                                               String... protections) {
        SqlAttackAssertion assertion = new SqlAttackAssertion();

        assertion.setTarget(targetMessageType);
        assertion.setIncludeUrlPath(includeUrlPath);
        assertion.setIncludeUrlQueryString(includeUrlQueryString);
        assertion.setIncludeBody(includeBody);

        for(String protection : protections) {
            assertion.setProtection(protection);
        }

        return assertion;
    }

    private ServerSqlAttackAssertion createServer(SqlAttackAssertion assertion) throws PolicyAssertionException {
        ServerSqlAttackAssertion serverAssertion = new ServerSqlAttackAssertion(assertion);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        return serverAssertion;
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(TargetMessageType targetType,
            @Nullable String resource) throws IOException, SAXException {
        Message request;
        Message response;

        if (TargetMessageType.REQUEST == targetType && null != resource) {
            request = createMessageFromXmlResource(resource);
        } else {
            request = createRequest();
        }

        if (TargetMessageType.RESPONSE == targetType && null != resource) {
            response = createMessageFromXmlResource(resource);
        } else {
            response = createResponse();
        }

        PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        if (TargetMessageType.RESPONSE == targetType) {
            context.setRoutingStatus(RoutingStatus.ROUTED);
        }

        return context;
    }

    private Message createMessageFromXmlResource(String resource) throws IOException, SAXException {
        return new Message(XmlUtil.parse(getClass().getResourceAsStream(resource)));
    }

    private Message createRequest(HttpMethod httpMethod, @Nullable String requestPath, @Nullable String queryString,
                                  @Nullable String body, ContentTypeHeader contentTypeHeader) throws IOException {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hRequest = new MockHttpServletRequest(servletContext);

        hRequest.setMethod(httpMethod.name());
        hRequest.addHeader("Content-Type", contentTypeHeader.getFullValue());

        if (null != requestPath) {
            hRequest.setRequestURI(requestPath);
        }

        if (null != queryString) {
            hRequest.setQueryString(queryString);
        }

        Message request = new Message();

        if (null != body) {
            request.initialize(stashManager, contentTypeHeader, new ByteArrayInputStream(body.getBytes(Charsets.UTF8)));
        }

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));

        return request;
    }

    private Message createRequest() {
        MockHttpServletRequest hRequest = new MockHttpServletRequest();

        Message request = new Message();
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));

        return request;
    }

    private Message createResponse() {
        MockHttpServletResponse hResponse = new MockHttpServletResponse();

        Message response = new Message();
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hResponse));

        return response;
    }

    /**
     * Checks presence or absence of audits to confirm the correct operations were/not carried out and any expected
     * results were recorded properly.
     */
    private void checkAuditPresence(boolean scanningBody, boolean scanningUrlPath, boolean scanningUrlQueryString,
                                    boolean detectedBody, boolean detectedUrlPath, boolean detectedUrlQueryString,
                                    boolean rejected) {
        assertEquals(AssertionMessages.SQLATTACK_SCANNING_BODY_TEXT.getMessage(),
                scanningBody, testAudit.isAuditPresent(AssertionMessages.SQLATTACK_SCANNING_BODY_TEXT));
        assertEquals(AssertionMessages.SQLATTACK_SCANNING_URL_PATH.getMessage(),
                scanningUrlPath, testAudit.isAuditPresent(AssertionMessages.SQLATTACK_SCANNING_URL_PATH));
        assertEquals(AssertionMessages.SQLATTACK_SCANNING_URL_QUERY_STRING.getMessage(),
                scanningUrlQueryString,
                testAudit.isAuditPresent(AssertionMessages.SQLATTACK_SCANNING_URL_QUERY_STRING));
        assertEquals(AssertionMessages.SQLATTACK_DETECTED.getMessage(),
                detectedBody, testAudit.isAuditPresent(AssertionMessages.SQLATTACK_DETECTED));
        assertEquals(AssertionMessages.SQLATTACK_DETECTED_PATH.getMessage(),
                detectedUrlPath, testAudit.isAuditPresent(AssertionMessages.SQLATTACK_DETECTED_PATH));
        assertEquals(AssertionMessages.SQLATTACK_DETECTED_PARAM.getMessage(),
                detectedUrlQueryString, testAudit.isAuditPresent(AssertionMessages.SQLATTACK_DETECTED_PARAM));
        assertEquals(AssertionMessages.SQLATTACK_REJECTED.getMessage(),
                rejected, testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }
}
