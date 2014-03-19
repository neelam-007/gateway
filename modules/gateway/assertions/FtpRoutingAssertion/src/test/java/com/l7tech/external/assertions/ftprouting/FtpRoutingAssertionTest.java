package com.l7tech.external.assertions.ftprouting;

import com.l7tech.common.ftp.FtpCommand;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource;
import com.l7tech.gateway.common.transport.ftp.FtpSecurity;
import static org.junit.Assert.assertEquals;

import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.test.BugId;
import org.junit.Test;
import org.w3c.dom.Document;


/**
 * @author jbufu
 * @author jwilliams
 */
public class FtpRoutingAssertionTest {

    // connection settings
    private static final FtpSecurity TEST_PROPERTY_SECURITY = FtpSecurity.FTPS_IMPLICIT;
    private static final String TEST_PROPERTY_HOST = "ftp.example.com";
    private static final String TEST_PROPERTY_PORT = "22221";
    private static final int TEST_PROPERTY_TIMEOUT = 10;
    private static final boolean TEST_PROPERTY_VERIFY_SERVER_CERT = false;

    // command settings
    private static final FtpCommand TEST_PROPERTY_FTP_COMMAND = FtpCommand.LIST;
    private static final boolean TEST_PROPERTY_COMMAND_FROM_VARIABLE = false;
    private static final String TEST_PROPERTY_FTP_COMMAND_VARIABLE = null;
    private static final String TEST_PROPERTY_DIRECTORY = "/files";
    private static final String TEST_PROPERTY_ARGUMENTS = "log.txt";
    private static final int TEST_PROPERTY_FAILURE_MODE = FtpRoutingAssertion.FAIL_ON_PERMANENT;
    private static final MessageTargetableSupport TEST_PROPERTY_REQUEST_TARGET =
            new MessageTargetableSupport(TargetMessageType.REQUEST, false);
    private static final MessageTargetableSupport TEST_PROPERTY_RESPONSE_TARGET =
            new MessageTargetableSupport(TargetMessageType.RESPONSE, true);

    // authentication
    private static final FtpCredentialsSource TEST_PROPERTY_CREDENTIALS_SOURCE = FtpCredentialsSource.SPECIFIED;
    private static final String TEST_PROPERTY_USER_NAME = "jdoe";
    private static final String TEST_PROPERTY_PASSWORD_EXPRESSION = "password";
    private static final boolean TEST_PROPERTY_PASSWORD_USES_CONTEXT_VARIABLES = false;
    private static final Goid TEST_PROPERTY_PASSWORD_GOID = null;

    // advanced settings
    private static final String TEST_PROPERTY_RESPONSE_BYTE_LIMIT = "1000000";
    private static final boolean TEST_PROPERTY_USE_CLIENT_CERT = false;
    private static final String TEST_PROPERTY_CLIENT_CERT_KEY_ALIAS = null;
    private static final Goid TEST_PROPERTY_CLIENT_CERT_KEYSTORE_ID = null;

    // pre-icefish settings
    private static final String TEST_PROPERTY_FILE_NAME_PATTERN = "log.txt";

    // policy xml
    private static final String OPENING_POLICY_XML_TAGS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" " +
                    "xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "    <L7p:FtpRoutingAssertion>\n";

    private static final String CLOSING_POLICY_XML_TAGS =
            "    </L7p:FtpRoutingAssertion>\n" +
            "</wsp:Policy>\n";

    private static final String PRE_ICEFISH_POLICY_USING_FILE_NAME_PATTERN =
            OPENING_POLICY_XML_TAGS +
                    "        <L7p:FileNamePattern stringValue=\"" + TEST_PROPERTY_FILE_NAME_PATTERN + "\"/>\n" +
                    "        <L7p:FileNameSource fileNameSource=\"" + FtpFileNameSource.PATTERN.getWspName() + "\"/>\n" +
                    "        <L7p:CredentialsSource credentialsSource=\"" + TEST_PROPERTY_CREDENTIALS_SOURCE.getWspName() + "\"/>\n" +
                    "        <L7p:Directory stringValue=\"" + TEST_PROPERTY_DIRECTORY + "\"/>\n" +
                    "        <L7p:HostName stringValue=\"" + TEST_PROPERTY_HOST + "\"/>\n" +
                    "        <L7p:Password stringValue=\"" + TEST_PROPERTY_PASSWORD_EXPRESSION + "\"/>\n" +
                    "        <L7p:Port stringValue=\"" + TEST_PROPERTY_PORT + "\"/>\n" +
                    "        <L7p:Security security=\"" + TEST_PROPERTY_SECURITY.getWspName() + "\"/>\n" +
                    "        <L7p:Timeout intValue=\"" + TEST_PROPERTY_TIMEOUT + "\"/>\n" +
                    "        <L7p:UserName stringValue=\"" + TEST_PROPERTY_USER_NAME + "\"/>\n" +
            CLOSING_POLICY_XML_TAGS;


    private static final String PRE_ICEFISH_POLICY_USING_AUTO_GENERATED_FILE_NAME =
            OPENING_POLICY_XML_TAGS +
                    "        <L7p:FileNameSource fileNameSource=\"" + FtpFileNameSource.AUTO.getWspName() + "\"/>\n" +
                    "        <L7p:CredentialsSource credentialsSource=\"" + TEST_PROPERTY_CREDENTIALS_SOURCE.getWspName() + "\"/>\n" +
                    "        <L7p:Directory stringValue=\"" + TEST_PROPERTY_DIRECTORY + "\"/>\n" +
                    "        <L7p:HostName stringValue=\"" + TEST_PROPERTY_HOST + "\"/>\n" +
                    "        <L7p:Password stringValue=\"" + TEST_PROPERTY_PASSWORD_EXPRESSION + "\"/>\n" +
                    "        <L7p:Port stringValue=\"" + TEST_PROPERTY_PORT + "\"/>\n" +
                    "        <L7p:Security security=\"" + TEST_PROPERTY_SECURITY.getWspName() + "\"/>\n" +
                    "        <L7p:Timeout intValue=\"" + TEST_PROPERTY_TIMEOUT + "\"/>\n" +
                    "        <L7p:UserName stringValue=\"" + TEST_PROPERTY_USER_NAME + "\"/>\n" +
            CLOSING_POLICY_XML_TAGS;

    // current as of Icefish
    private static final String CURRENT_POLICY =
            OPENING_POLICY_XML_TAGS +
                    "        <L7p:Arguments stringValue=\"" + TEST_PROPERTY_ARGUMENTS + "\"/>\n" +
                    "        <L7p:CredentialsSource credentialsSource=\"" + TEST_PROPERTY_CREDENTIALS_SOURCE.getWspName() + "\"/>\n" +
                    "        <L7p:Directory stringValue=\"" + TEST_PROPERTY_DIRECTORY + "\"/>\n" +
                    "        <L7p:FailureMode intValue=\"" + TEST_PROPERTY_FAILURE_MODE + "\"/>\n" +
                    "        <L7p:FtpCommand ftpCommand=\"" + TEST_PROPERTY_FTP_COMMAND + "\"/>\n" +
                    "        <L7p:HostName stringValue=\"" + TEST_PROPERTY_HOST + "\"/>\n" +
                    "        <L7p:Password stringValue=\"" + TEST_PROPERTY_PASSWORD_EXPRESSION + "\"/>\n" +
                    "        <L7p:Port stringValue=\"" + TEST_PROPERTY_PORT + "\"/>\n" +
                    "        <L7p:ResponseByteLimit stringValue=\"" + TEST_PROPERTY_RESPONSE_BYTE_LIMIT + "\"/>\n" +
                    "        <L7p:Security security=\"" + TEST_PROPERTY_SECURITY.getWspName() + "\"/>\n" +
                    "        <L7p:Timeout intValue=\"" + TEST_PROPERTY_TIMEOUT + "\"/>\n" +
                    "        <L7p:UserName stringValue=\"" + TEST_PROPERTY_USER_NAME + "\"/>\n" +
            CLOSING_POLICY_XML_TAGS;

    @BugId("SSG-6897")
    @Test
    public void testSerialization_CurrentPolicy() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(FtpRoutingAssertion.class);

        WspReader wspReader = new WspReader(registry);

        // test deserialization
        FtpRoutingAssertion assertion =
                (FtpRoutingAssertion) wspReader.parseStrictly(CURRENT_POLICY, WspReader.INCLUDE_DISABLED);

        assertEquals(TEST_PROPERTY_ARGUMENTS, assertion.getArguments());
        assertEquals(TEST_PROPERTY_CLIENT_CERT_KEY_ALIAS, assertion.getClientCertKeyAlias());
        assertEquals(TEST_PROPERTY_CLIENT_CERT_KEYSTORE_ID, assertion.getClientCertKeystoreId());
        assertEquals(TEST_PROPERTY_CREDENTIALS_SOURCE, assertion.getCredentialsSource());
        assertEquals(TEST_PROPERTY_DIRECTORY, assertion.getDirectory());
        assertEquals(TEST_PROPERTY_FAILURE_MODE, assertion.getFailureMode());
        assertEquals(TEST_PROPERTY_FTP_COMMAND, assertion.getFtpCommand());
        assertEquals(TEST_PROPERTY_FTP_COMMAND_VARIABLE, assertion.getFtpCommandVariable());
        assertEquals(TEST_PROPERTY_HOST, assertion.getHostName());
        assertEquals(TEST_PROPERTY_PASSWORD_EXPRESSION, assertion.getPassword());
        assertEquals(TEST_PROPERTY_PASSWORD_GOID, assertion.getPasswordGoid());
        assertEquals(TEST_PROPERTY_PORT, assertion.getPort());
        assertEquals(TEST_PROPERTY_REQUEST_TARGET, assertion.getRequestTarget());
        assertEquals(TEST_PROPERTY_RESPONSE_BYTE_LIMIT, assertion.getResponseByteLimit());
        assertEquals(TEST_PROPERTY_RESPONSE_TARGET, assertion.getResponseTarget());
        assertEquals(TEST_PROPERTY_SECURITY, assertion.getSecurity());
        assertEquals(TEST_PROPERTY_TIMEOUT, assertion.getTimeout());
        assertEquals(TEST_PROPERTY_USER_NAME, assertion.getUserName());
        assertEquals(TEST_PROPERTY_PASSWORD_USES_CONTEXT_VARIABLES, assertion.isPasswordUsesContextVariables());
        assertEquals(TEST_PROPERTY_USE_CLIENT_CERT, assertion.isUseClientCert());
        assertEquals(TEST_PROPERTY_VERIFY_SERVER_CERT, assertion.isVerifyServerCert());
        assertEquals(TEST_PROPERTY_COMMAND_FROM_VARIABLE, assertion.isCommandFromVariable());

        // test serialization
        assertEquals(CURRENT_POLICY, WspWriter.getPolicyXml(assertion));
    }

    @BugId("SSG-6897")
    @Test
    public void testSerialization_ParsePreIcefishPolicyUsingFileNamePattern() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(FtpRoutingAssertion.class);

        WspReader wspReader = new WspReader(registry);

        FtpRoutingAssertion assertion =
                (FtpRoutingAssertion) wspReader.parseStrictly(PRE_ICEFISH_POLICY_USING_FILE_NAME_PATTERN,
                        WspReader.INCLUDE_DISABLED);

        // test new properties have not been changed from expected defaults
        assertEquals(false, assertion.isCommandFromVariable());
        assertEquals(null, assertion.getFtpCommandVariable());
        assertEquals(FtpCommand.STOR, assertion.getFtpCommand());
        assertEquals(null, assertion.getPasswordGoid());
        assertEquals(null, assertion.getResponseByteLimit());
        assertEquals(TEST_PROPERTY_RESPONSE_TARGET, assertion.getResponseTarget());
        assertEquals(FtpRoutingAssertion.FAIL_ON_TRANSIENT, assertion.getFailureMode());

        // test arguments property interpreted correctly
        assertEquals(TEST_PROPERTY_FILE_NAME_PATTERN, assertion.getArguments());
    }

    @BugId("SSG-6897")
    @Test
    public void testSerialization_ParsePreIcefishPolicyUsingAutoGeneratedFileName() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(FtpRoutingAssertion.class);

        WspReader wspReader = new WspReader(registry);

        FtpRoutingAssertion assertion =
                (FtpRoutingAssertion) wspReader.parseStrictly(PRE_ICEFISH_POLICY_USING_AUTO_GENERATED_FILE_NAME,
                        WspReader.INCLUDE_DISABLED);

        // test new properties have not been changed from expected defaults
        assertEquals(false, assertion.isCommandFromVariable());
        assertEquals(null, assertion.getFtpCommandVariable());
        assertEquals(FtpCommand.STOR, assertion.getFtpCommand());
        assertEquals(null, assertion.getPasswordGoid());
        assertEquals(null, assertion.getResponseByteLimit());
        assertEquals(TEST_PROPERTY_RESPONSE_TARGET, assertion.getResponseTarget());
        assertEquals(FtpRoutingAssertion.FAIL_ON_TRANSIENT, assertion.getFailureMode());

        // test arguments property interpreted correctly
        assertEquals(FtpRoutingAssertion.AUTO_FILE_NAME_REQUEST_ID_VARIABLE, assertion.getArguments());
    }
}
