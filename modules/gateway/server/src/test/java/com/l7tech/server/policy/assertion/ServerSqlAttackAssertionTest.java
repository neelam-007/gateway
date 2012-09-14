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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test coverage of SQL Attack Protection Assertion
 *
 * @author jwilliams
 * @version 1.1 2012/09/12
 */
public class ServerSqlAttackAssertionTest {

    // protection types
    private static final String META = "SqlMeta";
    private static final String META_TEXT = "SqlMetaText";
    private static final String MS_SQL = "MsSql";
    private static final String ORA_SQL = "OraSql";

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
    public void setUp(){
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
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
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    // Request URL tests

    /**
     * Request URL contains double dash characters - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveDoubleDashInUrl_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=data--", META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains double dash characters - should not be caught by protections other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveDoubleDashInUrl_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl("rest?input=data--", META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains hash mark character - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveHashMarkInUrl_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=dat#a", META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains hash mark character - should not be caught by protections other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveHashMarkInUrl_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=dat#a", META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains single quote character - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSingleQuoteInUrl_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=';DROP%20TABLE%20USERS;'", META);

        assertEquals(AssertionStatus.BAD_REQUEST, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains single quote character - should not be caught by protections other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSingleQuoteInUrl_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl("rest?input=';DROP%20TABLE%20USERS;'", META_TEXT, MS_SQL, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains Oracle 'bfilename' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleBfilenameExploitInUrl_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=bfilename", ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains Oracle 'bfilename' exploit - should not be caught by protections other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleBfilenameExploitInUrl_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=bfilename", META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains Oracle 'offset' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleOffsetExploitInUrl_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=tz_offset", ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains Oracle 'offset' exploit - should not be caught by protections other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleOffsetExploitInUrl_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=tz_offset", META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains Oracle 'timestamp' exploit - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleTimestampExploitInUrl_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=to_timestamp_tz", ORA_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains Oracle 'timestamp' exploit - should not be caught by protections other than ORA_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_OracleTimestampExploitInUrl_AssertionStatusNone() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=to_timestamp_tz", META, META_TEXT, MS_SQL);

        assertEquals(AssertionStatus.NONE, status);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains MS SQL Server 'exec sp_' exploit - should be caught by MS_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecSPExploitInUrl_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=exec%20sp_dropextendedproc", MS_SQL);

        assertEquals(AssertionStatus.BAD_REQUEST, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains MS SQL Server 'exec sp_' exploit - should not be caught by protections other than MS_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecSPExploitInUrlMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl("rest?input=exec%20sp_dropextendedproc", META, META_TEXT, ORA_SQL);

        assertEquals(AssertionStatus.NONE, status);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains MS SQL Server 'exec xp_' exploit - should be caught by MS_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecXPExploitInUrl_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status = runTestOnRequestUrl("rest?input=exec%20xp_smtp_sendmail", MS_SQL);
        assertEquals(AssertionStatus.BAD_REQUEST, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    /**
     * Request URL contains MS SQL Server 'exec xp_' exploit - should not be caught by protections other than MS_SQL.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_MSSQLServerExecXPExploitInUrlMissed_AssertionStatusNone() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl("rest?input=exec%20xp_smtp_sendmail", META, META_TEXT, ORA_SQL);
        assertEquals(AssertionStatus.NONE, status);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.SQLATTACK_REJECTED));
    }

    // Context variable tests

    /**
     * Context variable contains XML with invasive SQL - should be caught by META protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSqlInContextVariableTargetCaught_AssertionStatusFalsified() throws Exception {
        final String varName = "testInput";

        SqlAttackAssertion assertion = createAssertion(TargetMessageType.OTHER, false, true, META);
        assertion.setOtherTargetMessageVariable(varName);

        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = createPolicyEnforcementContext(TargetMessageType.OTHER, null);
        context.setVariable(varName, createMessageFromXmlResource(INVASIVE_SQL_DOUBLE_DASH));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FALSIFIED, status);
    }

    /**
     * Context variable contains XML with invasive SQL - should not be caught by protections other than META.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSqlInContextVariableTargetMissed_AssertionStatusNone() throws Exception {
        final String varName = "testInput";

        SqlAttackAssertion assertion = createAssertion(TargetMessageType.OTHER, false, true, META_TEXT, MS_SQL, ORA_SQL);
        assertion.setOtherTargetMessageVariable(varName);

        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = createPolicyEnforcementContext(TargetMessageType.OTHER, null);
        context.setVariable(varName, createMessageFromXmlResource(INVASIVE_SQL_DOUBLE_DASH));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);
    }

    // Routing check tests

    /**
     * Check is performed on REQUEST, post-routing - the server should return a FAILED status.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_REQUESTMessagePostRouting_AssertionStatusFailed() throws Exception {
        SqlAttackAssertion assertion = createAssertion(TargetMessageType.REQUEST, false, true, META);
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
        SqlAttackAssertion assertion = createAssertion(TargetMessageType.RESPONSE, false, true, META);
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
        SqlAttackAssertion assertion = createAssertion(targetType, false, true, protections);
        ServerSqlAttackAssertion serverAssertion = createServer(assertion);
        final PolicyEnforcementContext context = createPolicyEnforcementContext(targetType, resource);

        return serverAssertion.checkRequest(context);
    }

    private AssertionStatus runTestOnRequestUrl(String url, String... protections)
            throws IOException, PolicyAssertionException, SAXException {
        SqlAttackAssertion assertion = createAssertion(TargetMessageType.REQUEST, true, false, protections);
        ServerSqlAttackAssertion serverAssertion = createServer(assertion);

        Message request = createRequest(HttpMethod.GET, url, "", ContentTypeHeader.TEXT_DEFAULT);

        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, createResponse());

        return serverAssertion.checkRequest(context);
    }

    private SqlAttackAssertion createAssertion(TargetMessageType targetMessageType, boolean includeRequestUrl,
                                               boolean includeRequestBody, String... protections) {
        SqlAttackAssertion assertion = new SqlAttackAssertion();

        assertion.setTarget(targetMessageType);
        assertion.setIncludeRequestUrl(includeRequestUrl);
        assertion.setIncludeRequestBody(includeRequestBody);

        for(String protection : protections) {
            assertion.setProtection(protection);
        }

        return assertion;
    }

    private ServerSqlAttackAssertion createServer(SqlAttackAssertion assertion) {
        ServerSqlAttackAssertion serverAssertion = new ServerSqlAttackAssertion(assertion);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        return serverAssertion;
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(TargetMessageType targetType, @Nullable String resource) throws IOException, SAXException {
        Message request;
        Message response;

        if(TargetMessageType.REQUEST == targetType && null != resource) {
            request = createMessageFromXmlResource(resource);
        } else {
            request = createRequest();
        }

        if(TargetMessageType.RESPONSE == targetType && null != resource) {
            response = createMessageFromXmlResource(resource);
        } else {
            response = createResponse();
        }

        PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        if(TargetMessageType.RESPONSE == targetType) {
            context.setRoutingStatus(RoutingStatus.ROUTED);
        }

        return context;
    }

    private Message createMessageFromXmlResource(String resource) throws IOException, SAXException {
        return new Message(XmlUtil.parse(getClass().getResourceAsStream(resource)));
    }

    private Message createRequest(HttpMethod httpMethod, @Nullable String queryString,
                                  @Nullable String body, ContentTypeHeader contentTypeHeader) throws IOException {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hRequest = new MockHttpServletRequest(servletContext);
        hRequest.setMethod(httpMethod.name());

        if(null != queryString) {
            hRequest.setQueryString(queryString);
        }

        Message request = new Message();

        if(null != body) {
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
}
