package com.l7tech.server.policy.variable;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.log.TestHandler;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.mqtt.MQTTQOS2Proxy;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyMetadataStub;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.server.policy.assertion.ServerTrueAssertion;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.password.SecurePasswordManagerStub;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.SoapFaultDetail;
import com.l7tech.xml.soap.SoapVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPConstants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;
import static com.l7tech.message.JmsKnob.HEADER_TYPE_JMS_PROPERTY;
import static org.junit.Assert.*;
import static org.junit.Assert.assertArrayEquals;

/**
 *
 */
@SuppressWarnings({"JavaDoc"})
public class ServerVariablesTest {
    private static final Logger logger = Logger.getLogger(ServerVariablesTest.class.getName());
    private static final Logger serverVariablesLogger = Logger.getLogger(ServerVariables.class.getName());

    public static final String PROP_TEMPLATE_MAX_SIZE = "com.l7tech.server.policy.variable.partInfoBodyMaxSize";
    public static final String SOAP_NAMESPACE = "${request.soap.namespace}";

    private TestAudit auditor;
    private static final String REQUEST_BODY = "<myrequest/>";
    private static final String RESPONSE_BODY = "<myresponse/>";
    private static final String JSON_MESSAGE = "{\"result\":\"success\"}";
    private static final String AUDITED_REQUEST_XML = "<auditRecordRequestXml/>";
    private static final String AUDITED_RESPONSE_XML = "<auditRecordResponseXml/>";
    private TimeZone utcTimeZone;
    private TimeZone localTimeZone;

    @Before
    public void setUp() throws Exception {
        auditor = new TestAudit();
        utcTimeZone = TimeZone.getTimeZone("UTC");
        localTimeZone = TimeZone.getDefault();
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            Message.PROPERTY_ENABLE_ORIGINAL_DOCUMENT,
            PROP_TEMPLATE_MAX_SIZE
        );
    }

    @Test
    public void testBasicServiceContextVariables() throws Exception {
        final PolicyEnforcementContext pec = context();
        final PublishedService ps = new PublishedService();
        ps.setGoid(new Goid(0,123456L));
        ps.setName( "testServiceNameContextVariable" );
        ps.getPolicy().setGuid( "8ca3ff80-eaf5-11e0-9572-0800200c9a66" );
        ps.putProperty( "prop1", "prop1val" );
        ps.putProperty( "prop2", "prop2val" );
        ps.setDefaultRoutingUrl( "http://example.com/foo/bar/defaultRoutingUrl/*" );
        ps.setRoutingUri( "/foo/bar/routingUri/*" );
        pec.setService(ps);
        pec.setServicePolicyMetadata(new PolicyMetadataStub() {
            @Override
            public PolicyHeader getPolicyHeader() {
                return new PolicyHeader(ps.getPolicy(), 123L);
            }
        });

        final String nameValue = ServerVariables.get("service.name", pec).toString();
        assertEquals("service name variable ", "testServiceNameContextVariable", nameValue);

        final String oidValue = ServerVariables.get("service.oid", pec).toString();
        assertEquals("service oid variable", new Goid(0,123456L).toHexString(), oidValue);

        final String policyGuidValue = ServerVariables.get("service.policy.guid", pec).toString();
        assertEquals("service policy guid variable", "8ca3ff80-eaf5-11e0-9572-0800200c9a66", policyGuidValue);

        final String policyVersionValue = ServerVariables.get("service.policy.version", pec).toString();
        assertEquals("service policy guid variable", "123", policyVersionValue);

        final String prop1val = ServerVariables.get("service.property.prop1", pec).toString();
        assertEquals("service property value", "prop1val", prop1val);

        final String[] propsList = (String[]) ServerVariables.get( "service.property", pec );
        assertEquals( 2, propsList.length );
        Set<String> names = new HashSet<>( Arrays.asList( propsList ) );
        assertTrue( names.contains( "prop1" ) );
        assertTrue( names.contains( "prop2" ) );

        final String resolutionUri = (String) ServerVariables.get( "service.resolutionUri", pec );
        assertEquals( "/foo/bar/routingUri/*", resolutionUri );

        final String routingUrl = (String) ServerVariables.get( "service.defaultRoutingURL", pec );
        assertEquals( "http://example.com/foo/bar/defaultRoutingUrl/*", routingUrl );
    }

    @Test
    public void testPolicyContextVariables() throws Exception {
        final PolicyEnforcementContext pec = context();
        final Policy policy = new Policy( PolicyType.INCLUDE_FRAGMENT, "testPolicyNameContextVariable", null, false );
        policy.setGoid(new Goid(0, 1234567L));
        policy.setGuid("8ca3ff80-eaf5-11e0-9572-0800200c9a67");
        pec.setCurrentPolicyMetadata( new PolicyMetadataStub() {
            @Override
            public PolicyHeader getPolicyHeader() {
                return new PolicyHeader( policy, 1234321L );
            }
        } );

        final String nameValue = ServerVariables.get("policy.name", pec).toString();
        assertEquals("service name variable ", "testPolicyNameContextVariable", nameValue);

        final String oidValue = ServerVariables.get("policy.oid", pec).toString();
        assertEquals("service oid variable", new Goid(0, 1234567L).toHexString(), oidValue);

        final String policyGuidValue = ServerVariables.get("policy.guid", pec).toString();
        assertEquals("service policy guid variable", "8ca3ff80-eaf5-11e0-9572-0800200c9a67", policyGuidValue);

        final String policyVersionValue = ServerVariables.get("policy.version", pec).toString();
        assertEquals("service policy guid variable", "1234321", policyVersionValue);
    }

    @Test
    public void testAssertionContextVariables() throws Exception {
        final PolicyEnforcementContext pec = context();
        pec.pushAssertionOrdinal( 1 );
        pec.pushAssertionOrdinal( 2 );
        pec.pushAssertionOrdinal( 3 );

        {
            final Integer[] numberValue = (Integer[])ServerVariables.get("assertion.number", pec);
            assertArrayEquals("assertion number", new Integer[]{ 1,2,3 }, numberValue);

            final String numberStrValue = ServerVariables.get("assertion.numberstr", pec).toString();
            assertEquals("assertion number str variable", "1.2.3", numberStrValue);
        }

        pec.assertionStarting( new ServerTrueAssertion<>( new TrueAssertion() ) );

        final Integer[] numberValue = (Integer[])ServerVariables.get("assertion.number", pec);
        assertArrayEquals("assertion number (with assertion)", new Integer[]{ 1,2,3,1 }, numberValue);

        final String numberStrValue = ServerVariables.get("assertion.numberstr", pec).toString();
        assertEquals("assertion number str variable (with assertion)", "1.2.3.1", numberStrValue);
    }

    @Test
    public void testRequest() throws Exception {
        doTestMessage("request", REQUEST_BODY);
    }

    @Test
    public void testResponse() throws Exception {
        doTestMessage("response", RESPONSE_BODY);
    }

    /**
     * Test 'request.jms.property' prefix for property name that is not present.
     * Result should be empty String.
     */
    @Test
    public void testGetJmsPropertyValueFromRequest_PropertyDoesNotExist_EmptyStringReturned() throws Exception {
        expandAndCheck(context(), "${request.jms.property.foo}", "");
    }

    /**
     * Test 'request.jms.property' prefix for a property with multiple entries in the Request message.
     * Result should be the most recently set value for the property.
     */
    @Test
    public void testGetJmsPropertyValueFromRequest_MultipleEntriesForProperty_MostRecentValueReturned() throws Exception {
        PolicyEnforcementContext context = context();
        HeadersKnob headersKnob = context.getRequest().getHeadersKnob();

        headersKnob.addHeader("foo", "bar", HEADER_TYPE_JMS_PROPERTY);
        headersKnob.addHeader("foo", "baz", HEADER_TYPE_JMS_PROPERTY);

        // ensure the value returned is the most recently set one
        expandAndCheck(context, "${request.jms.property.foo}", "baz");
    }

    /**
     * Test 'response.jms.property' prefix for property in Response message.
     * Result should be the most recently set value for the property.
     */
    @Test
    public void testGetJmsPropertyValueFromResponse_PropertyExists_ValueReturned() throws Exception {
        PolicyEnforcementContext context = context();
        HeadersKnob headersKnob = context.getResponse().getHeadersKnob();

        headersKnob.addHeader("foo", "bar", HEADER_TYPE_JMS_PROPERTY);
        headersKnob.addHeader("foo", "baz", HEADER_TYPE_JMS_PROPERTY);

        // ensure the value returned is the most recently set one
        expandAndCheck(context, "${response.jms.property.foo}", "baz");
    }

    /**
     * Test 'request.jms.propertynames' variable for case where no properties are present in Request message.
     * Result should be an empty String.
     */
    @Test
    public void testGetJmsPropertyNamesFromRequest_NoPropertiesExist_EmptyStringReturned() throws Exception {
        expandAndCheck(context(), "${request.jms.propertynames}", "");
    }

    /**
     * Test 'response.jms.propertynames' variable for case where no properties are present in Response message.
     * Result should be an empty String.
     */
    @Test
    public void testGetJmsPropertyNamesFromResponse_NoPropertiesExist_EmptyStringReturned() throws Exception {
        expandAndCheck(context(), "${response.jms.propertynames}", "");
    }

    /**
     * Test 'request.jms.propertynames' variable for case where multiple properties are present in Request message.
     * Result should be all unique property names, separated by ', '.
     */
    @Test
    public void testGetJmsPropertyNamesFromRequest_MultiplePropertiesExist_NamesReturned() throws Exception {
        PolicyEnforcementContext context = context();
        HeadersKnob headersKnob = context.getRequest().getHeadersKnob();

        headersKnob.addHeader("foo", "bar", HEADER_TYPE_JMS_PROPERTY);
        headersKnob.addHeader("foo", "baz", HEADER_TYPE_JMS_PROPERTY);
        headersKnob.addHeader("fuz", "buz", HEADER_TYPE_JMS_PROPERTY);

        expandAndCheck(context, "${request.jms.propertynames}", "foo, fuz");
    }

    /**
     * Test 'response.jms.propertynames' variable for case where multiple properties are present in Response message.
     * Result should be all unique property names, separated by ', '.
     */
    @Test
    public void testGetJmsPropertyNamesFromResponse_MultiplePropertiesExist_NamesReturned() throws Exception {
        PolicyEnforcementContext context = context();
        HeadersKnob headersKnob = context.getResponse().getHeadersKnob();

        headersKnob.addHeader("foo", "bar", HEADER_TYPE_JMS_PROPERTY);
        headersKnob.addHeader("foo", "baz", HEADER_TYPE_JMS_PROPERTY);
        headersKnob.addHeader("fuz", "buz", HEADER_TYPE_JMS_PROPERTY);

        expandAndCheck(context, "${response.jms.propertynames}", "foo, fuz");
    }

    /**
     * Test 'request.jms.allpropertyvalues' variable for case where no properties are present in Request message.
     * Result should be an empty String.
     */
    @Test
    public void testGetJmsValuesFromRequest_NoValuesExist_EmptyStringReturned() throws Exception {
        expandAndCheck(context(), "${request.jms.allpropertyvalues}", "");
    }

    /**
     * Test 'response.jms.allpropertyvalues' variable for case where no properties are present in Response message.
     * Result should be an empty String.
     */
    @Test
    public void testGetJmsValuesFromResponse_NoValuesExist_EmptyStringReturned() throws Exception {
        expandAndCheck(context(), "${response.jms.allpropertyvalues}", "");
    }

    /**
     * Test 'request.jms.allpropertyvalues' prefix for a property with multiple entries in the Request message.
     * Result should be all property names and corresponding values (the most recently set), separated by ', '.
     */
    @Test
    public void testGetAllJmsPropertyValuesFromRequest_MultipleValuesExist_LatestValueForEachHeaderReturned() throws Exception {
        PolicyEnforcementContext context = context();
        HeadersKnob headersKnob = context.getRequest().getHeadersKnob();

        headersKnob.addHeader("foo", "bar", HEADER_TYPE_JMS_PROPERTY);
        headersKnob.addHeader("foo", "baz", HEADER_TYPE_JMS_PROPERTY);
        headersKnob.addHeader("fuz", "buz", HEADER_TYPE_JMS_PROPERTY);

        // ensure the values returned for each name are the most recently set ones
        final String varName = "${request.jms.allpropertyvalues}";
        String[] usedVars = Syntax.getReferencedNames(varName);
        Map<String, Object> vars = context.getVariableMap(usedVars, auditor);
        List values =ExpandVariables.processNoFormat(varName, vars, auditor);
        assertEquals(2, values.size());
        assertTrue(values.contains("fuz:buz"));
        assertTrue(values.contains("foo:baz"));
        assertFalse(values.contains("foo:bar"));
    }

    /**
     * Test 'response.jms.allpropertyvalues' prefix for property in Response message.
     * Result should be all property names and corresponding values (the most recently set), separated by ', '.
     */
    @Test
    public void testGetAllJmsPropertyValuesFromResponse_MultipleValuesExist_LatestValueForEachHeaderReturned() throws Exception {
        PolicyEnforcementContext context = context();
        HeadersKnob headersKnob = context.getResponse().getHeadersKnob();

        headersKnob.addHeader("foo", "bar", HEADER_TYPE_JMS_PROPERTY);
        headersKnob.addHeader("foo", "baz", HEADER_TYPE_JMS_PROPERTY);
        headersKnob.addHeader("fuz", "buz", HEADER_TYPE_JMS_PROPERTY);

        final String varName = "${response.jms.allpropertyvalues}";
        String[] usedVars = Syntax.getReferencedNames(varName);
        Map<String, Object> vars = context.getVariableMap(usedVars, auditor);
        List values =ExpandVariables.processNoFormat(varName, vars, auditor);
        assertEquals(2, values.size());
        assertTrue(values.contains("fuz:buz"));
        assertTrue(values.contains("foo:baz"));
        assertFalse(values.contains("foo:bar"));
    }

    /**
     * Test support for .mainpart on Message variables of type 'application/json'.
     *
     * @throws Exception
     */
    @Test
    public void testJsonMainPart() throws Exception {
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        StashManagerFactory factory = (StashManagerFactory) applicationContext.getBean("stashManagerFactory");
        StashManager stashManager = factory.createStashManager();

        Message m = new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(JSON_MESSAGE.getBytes()));
        context.setVariable("jsonmessage", m);
        expandAndCheck(context, "${jsonmessage.mainpart}", JSON_MESSAGE);
    }

    private void doTestMessage(String prefix, String messageBody) throws Exception {
        expandAndCheck(context(), "${" + prefix + ".size}", String.valueOf(messageBody.getBytes().length));
        expandAndCheck(context(), "${" + prefix + ".mainpart}", messageBody);
        expandAndCheck(context(), "${" + prefix + ".wss.certificates.count}", "0");
    }

    @Test
    public void testNoUser() throws Exception {
        expandAndCheck(context(), "${request.authenticatedUser}", "");
        expandAndCheck(context(), "${request.authenticatedUsers}", "");
        expandAndCheck(context(), "${request.authenticatedDn}", "");
        expandAndCheck(context(), "${request.authenticatedDns}", "");
    }

    @Test
    public void testNoDn() throws Exception {
        PolicyEnforcementContext context = context(new InternalUser("blah"));
        expandAndCheck(context, "${request.authenticatedUser}", "blah");
        expandAndCheck(context, "${request.authenticatedUsers}", "blah");
        expandAndCheck(context, "${request.authenticatedDn}", "");
        expandAndCheck(context, "${request.authenticatedDns}", "");
    }

    @Test
    public void testNullDn() throws Exception {
        PolicyEnforcementContext context = context(new LdapUser(new Goid(0,-3823), null, null));
        expandAndCheck(context, "${request.authenticatedUser}", "");
        expandAndCheck(context, "${request.authenticatedUsers}", "");
        expandAndCheck(context, "${request.authenticatedDn}", "");
        expandAndCheck(context, "${request.authenticatedDns}", "");
    }

    @Test
    public void testNonNumericSuffix() throws Exception {
        PolicyEnforcementContext context = context(new LdapUser(new Goid(0,-3823), "cn=blah", "blah"));
        expandAndCheck(context, "${request.authenticatedUser.bogus}", "");
        expandAndCheck(context, "${request.authenticatedUsers.bogus}", "");
        expandAndCheck(context, "${request.authenticatedDn.bogus}", "");
        expandAndCheck(context, "${request.authenticatedDns.bogus}", "");
    }

    @Test
    public void testOutOfBoundsSuffix() throws Exception {
        PolicyEnforcementContext context = context();
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3823), "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3824), "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3825), "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "${request.authenticatedUser}", "test+3");
        expandAndCheck(context, "${request.authenticatedUser.0}", "test+1");
        expandAndCheck(context, "${request.authenticatedUser.1}", "test+2");
        expandAndCheck(context, "${request.authenticatedUser.2}", "test+3");
        expandAndCheck(context, "${request.authenticatedUsers}", "test+1, test+2, test+3");
        expandAndCheck(context, "${request.authenticatedUsers|, }", "test+1, test+2, test+3");
        expandAndCheck(context, "||${request.authenticatedUsers|||}||", "||test+1||test+2||test+3||");
    }

    @Test
    public void testEmptyDn() throws Exception {
        PolicyEnforcementContext context = context(new LdapUser(new Goid(0,-3823), "", null));
        expandAndCheck(context, "${request.authenticatedUser}", "");
        expandAndCheck(context, "${request.authenticatedUsers}", "");
        expandAndCheck(context, "${request.authenticatedDn}", "");
        expandAndCheck(context, "${request.authenticatedDns}", "");
    }

    @Test
    public void testEscapedDnUserName() throws Exception {
        PolicyEnforcementContext context = context(new LdapUser(new Goid(0,-3823), "cn=test\\+1, ou=foobar, o=bling", "test+1"));
        expandAndCheck(context, "${request.authenticatedUser}", "test+1");
        expandAndCheck(context, "${request.authenticatedUsers}", "test+1");
    }

    @Test
    public void testMultipleUserNames() throws Exception {
        PolicyEnforcementContext context = context();
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3823), "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3824), "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3825), "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "${request.authenticatedUser}", "test+3");
        expandAndCheck(context, "${request.authenticatedUser.0}", "test+1");
        expandAndCheck(context, "${request.authenticatedUser.1}", "test+2");
        expandAndCheck(context, "${request.authenticatedUser.2}", "test+3");
        expandAndCheck(context, "${request.authenticatedUsers}", "test+1, test+2, test+3");
        expandAndCheck(context, "${request.authenticatedUsers|, }", "test+1, test+2, test+3");
        expandAndCheck(context, "||${request.authenticatedUsers|||}||", "||test+1||test+2||test+3||");
    }

    @Test
    public void testMultipleUserDns() throws Exception {
        PolicyEnforcementContext context = context();
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3823), "cn=test1, ou=foobar, o=bling", "test1"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3824), "cn=test2", "test2"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3825), "cn=test3", "test3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "${request.authenticatedDn}", "cn=test3");
        expandAndCheck(context, "${request.authenticatedDn.0}", "cn=test1, ou=foobar, o=bling");
        expandAndCheck(context, "${request.authenticatedDn.1}", "cn=test2");
        expandAndCheck(context, "${request.authenticatedDn.2}", "cn=test3");
        expandAndCheck(context, "||${request.authenticatedDns|||}||", "||cn=test1, ou=foobar, o=bling||cn=test2||cn=test3||");
        expandAndCheck(context, "${request.authenticatedDns}", "cn=test1, ou=foobar, o=bling, cn=test2, cn=test3");
        expandAndCheck(context, "${request.authenticatedDns|, }", "cn=test1, ou=foobar, o=bling, cn=test2, cn=test3");
    }

    @Test
    @BugNumber(6813)
    public void testEscapedDnUserDn() throws Exception {
        PolicyEnforcementContext context = context(new LdapUser(new Goid(0,-3823), "cn=test\\+1, ou=foobar, o=bling", "test+1"));
        expandAndCheck(context, "${request.authenticatedDn}", "cn=test\\+1, ou=foobar, o=bling");
        expandAndCheck(context, "${request.authenticatedDns}", "cn=test\\+1, ou=foobar, o=bling");
    }

    @Test
    @BugNumber(6813)
    public void testMultipleUserDnsEscaped() throws Exception {
        PolicyEnforcementContext context = context();
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3823), "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3824), "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3825), "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "||${request.authenticatedDns|||}||", "||cn=test\\+1, ou=foobar, o=bling||cn=test\\+2||cn=test\\+3||");
        expandAndCheck(context, "${request.authenticatedDns}", "cn=test\\+1, ou=foobar, o=bling, cn=test\\+2, cn=test\\+3");
        expandAndCheck(context, "${request.authenticatedDns|, }", "cn=test\\+1, ou=foobar, o=bling, cn=test\\+2, cn=test\\+3");
        expandAndCheck(context, "${request.authenticatedDn}", "cn=test\\+3");
        expandAndCheck(context, "${request.authenticatedDn.0}", "cn=test\\+1, ou=foobar, o=bling");
    }

    @Test
    public void testResponseNoUser() throws Exception {
        expandAndCheck(context(), "${response.authenticatedUser}", "");
        expandAndCheck(context(), "${response.authenticatedUsers}", "");
        expandAndCheck(context(), "${response.authenticatedDn}", "");
        expandAndCheck(context(), "${response.authenticatedDns}", "");
    }

    @Test
    public void testResponseNoDn() throws Exception {
        PolicyEnforcementContext context = responseContext(new InternalUser("blah"));
        expandAndCheck(context, "${RESPONSE.authenticatedUser}", "blah");
        expandAndCheck(context, "${RESPONSE.authenticatedUsers}", "blah");
        expandAndCheck(context, "${RESPONSE.authenticatedDn}", "");
        expandAndCheck(context, "${RESPONSE.authenticatedDns}", "");
    }

    @Test
    public void testResponseNullDn() throws Exception {
        PolicyEnforcementContext context = responseContext(new LdapUser(new Goid(0,-3823), null, null));
        expandAndCheck(context, "${response.AuthenticatedUser}", "");
        expandAndCheck(context, "${response.AuthenticatedUser}", "");
        expandAndCheck(context, "${response.AuthenticatedUser}", "");
        expandAndCheck(context, "${response.AuthenticatedUser}", "");
    }

    @Test
    public void testResponseNonNumericSuffix() throws Exception {
        PolicyEnforcementContext context = responseContext(new LdapUser(new Goid(0,-3823), "cn=blah", "blah"));
        expandAndCheck(context, "${response.authenticatedUser.bogus}", "");
        expandAndCheck(context, "${response.authenticatedUsers.bogus}", "");
        expandAndCheck(context, "${response.authenticatedDn.bogus}", "");
        expandAndCheck(context, "${response.authenticatedDns.bogus}", "");
    }

    @Test
    public void testResponseOutOfBoundsSuffix() throws Exception {
        PolicyEnforcementContext context = context();
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3823), "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3824), "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3825), "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "${response.authenticatedUser}", "test+3");
        expandAndCheck(context, "${response.authenticatedUser.0}", "test+1");
        expandAndCheck(context, "${response.authenticatedUser.1}", "test+2");
        expandAndCheck(context, "${response.authenticatedUser.2}", "test+3");
        expandAndCheck(context, "${response.authenticatedUsers}", "test+1, test+2, test+3");
        expandAndCheck(context, "${response.authenticatedUsers|, }", "test+1, test+2, test+3");
        expandAndCheck(context, "||${response.authenticatedUsers|||}||", "||test+1||test+2||test+3||");
    }

    @Test
    public void testResponseEmptyDn() throws Exception {
        PolicyEnforcementContext context = responseContext(new LdapUser(new Goid(0,-3823), "", null));
        expandAndCheck(context, "${response.authenticatedUser}", "");
        expandAndCheck(context, "${response.authenticatedUsers}", "");
        expandAndCheck(context, "${response.authenticatedDn}", "");
        expandAndCheck(context, "${response.authenticatedDns}", "");
    }

    @Test
    public void testResponseEscapedDnUserName() throws Exception {
        PolicyEnforcementContext context = responseContext(new LdapUser(new Goid(0,-3823), "cn=test\\+1, ou=foobar, o=bling", "test+1"));
        expandAndCheck(context, "${response.authenticatedUser}", "test+1");
        expandAndCheck(context, "${response.authenticatedUsers}", "test+1");
    }

    @Test
    public void testResponseMultipleUserNames() throws Exception {
        PolicyEnforcementContext context = context();
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3823), "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3824), "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3825), "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "${response.authenticatedUser}", "test+3");
        expandAndCheck(context, "${response.authenticatedUser.0}", "test+1");
        expandAndCheck(context, "${response.authenticatedUser.1}", "test+2");
        expandAndCheck(context, "${response.authenticatedUser.2}", "test+3");
        expandAndCheck(context, "${response.authenticatedUsers}", "test+1, test+2, test+3");
        expandAndCheck(context, "${response.authenticatedUsers|, }", "test+1, test+2, test+3");
        expandAndCheck(context, "||${response.authenticatedUsers|||}||", "||test+1||test+2||test+3||");
    }

    @Test
    public void testResponseMultipleUserDns() throws Exception {
        PolicyEnforcementContext context = context();
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3823), "cn=test1, ou=foobar, o=bling", "test1"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3824), "cn=test2", "test2"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3825), "cn=test3", "test3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "${response.authenticatedDn}", "cn=test3");
        expandAndCheck(context, "${response.authenticatedDn.0}", "cn=test1, ou=foobar, o=bling");
        expandAndCheck(context, "${response.authenticatedDn.1}", "cn=test2");
        expandAndCheck(context, "${response.authenticatedDn.2}", "cn=test3");
        expandAndCheck(context, "||${response.authenticatedDns|||}||", "||cn=test1, ou=foobar, o=bling||cn=test2||cn=test3||");
        expandAndCheck(context, "${response.authenticatedDns}", "cn=test1, ou=foobar, o=bling, cn=test2, cn=test3");
        expandAndCheck(context, "${response.authenticatedDns|, }", "cn=test1, ou=foobar, o=bling, cn=test2, cn=test3");
    }

    @Test
    public void testResponseEscapedDnUserDn() throws Exception {
        PolicyEnforcementContext context = responseContext(new LdapUser(new Goid(0,-3823), "cn=test\\+1, ou=foobar, o=bling", "test+1"));
        expandAndCheck(context, "${response.authenticatedDn}", "cn=test\\+1, ou=foobar, o=bling");
        expandAndCheck(context, "${response.authenticatedDns}", "cn=test\\+1, ou=foobar, o=bling");
    }

    @Test
    public void testResponseMultipleUserDnsEscaped() throws Exception {
        PolicyEnforcementContext context = context();
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3823), "cn=test\\+1, ou=foobar, o=bling", "test+1"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3824), "cn=test\\+2", "test+2"), new OpaqueSecurityToken()));
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(new LdapUser(new Goid(0,-3825), "cn=test\\+3", "test+3"), new OpaqueSecurityToken()));
        expandAndCheck(context, "||${response.authenticatedDns|||}||", "||cn=test\\+1, ou=foobar, o=bling||cn=test\\+2||cn=test\\+3||");
        expandAndCheck(context, "${response.authenticatedDns}", "cn=test\\+1, ou=foobar, o=bling, cn=test\\+2, cn=test\\+3");
        expandAndCheck(context, "${response.authenticatedDns|, }", "cn=test\\+1, ou=foobar, o=bling, cn=test\\+2, cn=test\\+3");
        expandAndCheck(context, "${response.authenticatedDn}", "cn=test\\+3");
        expandAndCheck(context, "${response.authenticatedDn.0}", "cn=test\\+1, ou=foobar, o=bling");
    }

    @Test
    public void testUsernamePassword() throws Exception {
        PolicyEnforcementContext context = context();
        context.getAuthenticationContext(context.getRequest()).addCredentials(LoginCredentials.makeLoginCredentials(new HttpBasicToken("Alice", "password".toCharArray()), HttpBasic.class));
        context.getAuthenticationContext(context.getResponse()).addCredentials(LoginCredentials.makeLoginCredentials(new UsernameTokenImpl("Bob", "secret".toCharArray()), WssBasic.class));
        expandAndCheck(context, "${request.username}", "Alice");
        expandAndCheck(context, "${request.password}", "password");
        expandAndCheck(context, "${response.username}", "Bob");
        expandAndCheck(context, "${response.password}", "secret");
    }

    @BugNumber(5336)
    @Test
    public void testOriginalRequest() throws Exception {
        SyspropUtil.setProperty(Message.PROPERTY_ENABLE_ORIGINAL_DOCUMENT, "true");
        Message.setDefaultEnableOriginalDocument(true);
        Message req = new Message(XmlUtil.stringAsDocument(REQUEST_BODY));
        PolicyEnforcementContext c = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, new Message());
        addTextNode(c.getRequest(), "reqfoo");
        String newreq = "<myrequest>reqfoo</myrequest>";
        expandAndCheck(c, "${request.originalmainpart}", REQUEST_BODY);
        expandAndCheck(c, "${request.mainpart}", newreq);
    }

    @Test
    public void testHttpMethod() throws Exception {
        PolicyEnforcementContext context = contextHttp();
        expandAndCheck(context, "${request.http.method}", "POST");

        context = context();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("ArgleBlargle");
        mockRequest.setContentType(ContentTypeHeader.XML_DEFAULT.getFullValue());
        mockRequest.addHeader(HttpConstants.HEADER_CONTENT_TYPE, ContentTypeHeader.XML_DEFAULT.getFullValue());
        context.getRequest().attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));
        expandAndCheck(context, "${request.http.method}", "ArgleBlargle");
    }

    @Test
    public void testHttpParameter() throws Exception {
        PolicyEnforcementContext context = contextHttp();
        expandAndCheck(context, "${request.http.parameter.single}", "1");
        expandAndCheck(context, "${request.http.parameter.multi}", "1");
        expandAndCheck(context, "${request.http.parameter.invalid}", "");
    }

    @Test
    public void testHttpHeader() throws Exception {
        PolicyEnforcementContext context = contextHttp();

        expandAndCheck(context, "${request.http.headernames}",
                HttpConstants.HEADER_CONNECTION + ", " + HttpConstants.HEADER_CONTENT_TYPE + ", " + HttpConstants.HEADER_COOKIE);

        expandAndCheck(context, "${request.http.allheadervalues[1]}",
                HttpConstants.HEADER_CONTENT_TYPE + ':' + ContentTypeHeader.XML_DEFAULT.getFullValue());

        expandAndCheck(context, "${request.http.allheadervalues[0]}",
                HttpConstants.HEADER_CONNECTION + ':' + ContentTypeHeader.XML_DEFAULT.getFullValue() + ", " + ContentTypeHeader.TEXT_DEFAULT.getFullValue());


    }

    @BugNumber(8428)
    @Test
    public void testRequestHttpParameters() throws Exception {
        PolicyEnforcementContext context = contextHttp();
        expandAndCheck(context, "${request.http.parameters.single}", "1");
        expandAndCheck(context, "${request.http.parameters.multi}", "1, 2, 3, 4, 5");
        expandAndCheck(context, "${request.http.parameters.multi|:}", "1:2:3:4:5");
        expandAndCheck(context, "${request.http.parameters.multi[0]}", "1");
        expandAndCheck(context, "${request.http.parameters.multi[1]}", "2");
        expandAndCheck(context, "${request.http.parameters.multi[2]}", "3");
        expandAndCheck(context, "${request.http.parameters.multi[3]}", "4");
        expandAndCheck(context, "${request.http.parameters.multi[4]}", "5");
        expandAndCheck(context, "${request.http.parameters.multi[5]}", "");
        expandAndCheck(context, "${request.http.parameters.invalid}", "");
    }

    /*
    * Test the service.url context variable and associated suffixes
    * : host, protocol, path, file, query
    * */
    @Test
    @SuppressWarnings({"deprecation"})
    public void testServiceUrlContextVariables() throws Exception {
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(REQUEST_BODY));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(RESPONSE_BODY));
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        String host = "servername.l7tech.com";
        String protocol = "http";
        String port = "8080";
        String filePath = "/HelloTestService";
        String query = "?query";
        String url = protocol + "://" + host + ":" + port + filePath + query;
        HttpRoutingAssertion hRA = new HttpRoutingAssertion(url);
        ServerHttpRoutingAssertion sHRA = new ServerHttpRoutingAssertion(hRA, applicationContext);
        try {
            sHRA.checkRequest(pec);
        } catch (Exception ex) {
            //This is expected, we don't expect to rouet to the url, just want
            //tryUrl to get called so that it sets the routed url property
        }

        String serviceurl = BuiltinVariables.PREFIX_SERVICE + "." + BuiltinVariables.SERVICE_SUFFIX_URL;

        // Test the deprecated context variable service.url
        String variableName = serviceurl;
        String variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url", url, variableValue);
        // Test the new context variable httpRouting.url
        variableName = HttpRoutingAssertion.VAR_HTTP_ROUTING_URL;
        variableValue = (String) pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url", url, variableValue);

        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_HOST;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url host", host, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlHost();
        variableValue = (String) pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url host", host, variableValue);

        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_PROTOCOL;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url protocol", protocol, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlProtocol();
        variableValue = (String) pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url protocol", protocol, variableValue);

        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_PORT;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url port", port, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlPort();
        variableValue = pec.getVariable(variableName).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url port", port, variableValue);

        //file expects the query string
        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_FILE;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url file", filePath + query, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlFile();
        variableValue = (String) pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url file", filePath + query, variableValue);

        //path doesn't expect the query string
        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_PATH;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url path", filePath, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlPath();
        variableValue = (String) pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url path", filePath, variableValue);

        variableName = serviceurl + "." + BuiltinVariables.SERVICE_SUFFIX_QUERY;
        variableValue = ServerVariables.get(variableName, pec).toString();
        Assert.assertEquals("ServerVariable should equal httpRouting url query", query, variableValue);
        variableName = HttpRoutingAssertion.getVarHttpRoutingUrlQuery();
        variableValue = (String) pec.getVariable(variableName);
        Assert.assertEquals("ServerVariable should equal httpRouting url query", query, variableValue);
    }

    @Test
    public void testNonAuditSinkCtx_base() throws Exception {
        Assert.assertNotNull(context().getVariable("audit"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testNonAuditSinkCtx_request() throws Exception {
        context().getVariable("audit.request");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testNonAuditSinkCtx_response() throws Exception {
        context().getVariable("audit.response");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testNonAuditSinkCtx_requestContentLength() throws Exception {
        context().getVariable("audit.requestContentLength");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testNonAuditSinkCtx_responseContentLength() throws Exception {
        context().getVariable("audit.responseContentLength");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testNonAuditSinkCtx_var() throws Exception {
        context().getVariable("audit.var.request.clientid");
    }

    @Test
    public void testAuditRecordFields() throws Exception {
        final AuditSinkPolicyEnforcementContext c = sinkcontext();
        expandAndCheck(c, "${audit.type}", "message");
        expandAndCheck(c, "${audit.id}", Goid.toString(new Goid(1234,9777L)));
        expandAndCheck(c, "${audit.level}", "800");
        expandAndCheck(c, "${audit.levelStr}", "INFO");
        expandAndCheck(c, "${audit.name}", "ACMEWarehouse");
        expandAndCheck(c, "${audit.sequenceNumber}", String.valueOf(c.getAuditRecord().getSequenceNumber()));
        expandAndCheck(c, "${audit.nodeId}", "node1");
        expandAndCheck(c, "${audit.requestId}", "0000000000000222-333");
        expandAndCheck(c, "${audit.time}", String.valueOf(c.getAuditRecord().getMillis()));
        expandAndCheck(c, "${audit.message}", "Message processed successfully");
        expandAndCheck(c, "${audit.ipAddress}", "3.2.1.1");
        expandAndCheck(c, "${audit.user.name}", "alice");
        expandAndCheck(c, "${audit.user.id}", "41123");
        expandAndCheck(c, "${audit.user.idProv}", "0000000000000000fffffffffffffffe");
        expandAndCheck(c, "${audit.authType}", "HTTP Basic");
        expandAndCheck(c, "${audit.thrown}", "");
        expandAndCheck(c, "${audit.entity.class}", ""); // only for admin records
        expandAndCheck(c, "${audit.entity.oid}", "");  // only for admin records
        expandAndCheck(c, "${audit.details[0]}", c.getAuditRecord().getDetailsInOrder()[0].toString());
    }

    @Test
    public void testMessageAuditRecordFields() throws Exception {
        SyspropUtil.setProperty(Message.PROPERTY_ENABLE_ORIGINAL_DOCUMENT, "false");
        Message.setDefaultEnableOriginalDocument(false);
        final AuditSinkPolicyEnforcementContext c = sinkcontext();
        expandAndCheck(c, "${audit.mappingValuesOid}", "0000000000000000000000000000c1b1");
        expandAndCheck(c, "${audit.operationName}", "listProducts");
        expandAndCheck(c, "${audit.requestContentLength}", String.valueOf(REQUEST_BODY.getBytes().length));
        expandAndCheck(c, "${audit.responseContentLength}", String.valueOf(RESPONSE_BODY.getBytes().length));
        expandAndCheck(c, "${audit.request.mainpart}", REQUEST_BODY);
        expandAndCheck(c, "${audit.response.mainpart}", RESPONSE_BODY);
        expandAndCheck(c, "${audit.filteredrequest}", AUDITED_REQUEST_XML);
        expandAndCheck(c, "${audit.filteredresponse}", AUDITED_RESPONSE_XML);
        expandAndCheck(c, "${audit.request.originalmainpart}", "");
        expandAndCheck(c, "${audit.response.originalmainpart}", "");
        expandAndCheck(c, "${audit.var.request.mainpart}", REQUEST_BODY);
        expandAndCheck(c, "${audit.var.response.mainpart}", RESPONSE_BODY);
        expandAndCheck(c, "${audit.var.request.size}", String.valueOf(REQUEST_BODY.getBytes().length));
        expandAndCheck(c, "${audit.requestSavedFlag}", "true");
        expandAndCheck(c, "${audit.responseSavedFlag}", "true");
        expandAndCheck(c, "${audit.routingLatency}", "232");
        expandAndCheck(c, "${audit.serviceOid}", new Goid(0,8859).toHexString());
        expandAndCheck(c, "${audit.status}", "0");
    }

    @Test
    public void testMessageAuditRecordFields_with_OrigRequest() throws Exception {
        SyspropUtil.setProperty(Message.PROPERTY_ENABLE_ORIGINAL_DOCUMENT, "true");
        Message.setDefaultEnableOriginalDocument(true);
        final AuditSinkPolicyEnforcementContext c = sinkcontext();
        addTextNode(c.getOriginalRequest(), "reqfoo");
        addTextNode(c.getOriginalResponse(), "respfoo");
        String newreq = "<myrequest>reqfoo</myrequest>";
        String newresp = "<myresponse>respfoo</myresponse>";
        expandAndCheck(c, "${audit.mappingValuesOid}", "0000000000000000000000000000c1b1");
        expandAndCheck(c, "${audit.operationName}", "listProducts");
        expandAndCheck(c, "${audit.request.mainpart}", newreq);
        expandAndCheck(c, "${audit.response.mainpart}", newresp);
        expandAndCheck(c, "${audit.requestContentLength}", String.valueOf(newreq.getBytes().length));
        expandAndCheck(c, "${audit.responseContentLength}", String.valueOf(newresp.getBytes().length));
        expandAndCheck(c, "${audit.var.request.mainpart}", newreq);
        expandAndCheck(c, "${audit.var.response.mainpart}", newresp);
        expandAndCheck(c, "${audit.var.request.size}", String.valueOf(newreq.getBytes().length));
        expandAndCheck(c, "${audit.request.originalmainpart}", REQUEST_BODY);
        expandAndCheck(c, "${audit.response.originalmainpart}", RESPONSE_BODY);
        expandAndCheck(c, "${audit.requestSavedFlag}", "true");
        expandAndCheck(c, "${audit.responseSavedFlag}", "true");
        expandAndCheck(c, "${audit.routingLatency}", "232");
        expandAndCheck(c, "${audit.serviceOid}", new Goid(0,8859).toHexString());
        expandAndCheck(c, "${audit.status}", "0");
    }

    @Test
    public void testSystemAuditRecordFields() throws Exception {
        SystemAuditRecord rec = new SystemAuditRecord(Level.WARNING, "node1", Component.GW_CSR_SERVLET, "CSR servlet is dancing!", true, PersistentEntity.DEFAULT_GOID, null, null, "Dancing", "1.2.3.4");
        final AuditSinkPolicyEnforcementContext c = new AuditSinkPolicyEnforcementContext(rec, delegate(), context());
        expandAndCheck(c, "${audit.type}", "system");
        expandAndCheck(c, "${audit.levelStr}", "WARNING");
        expandAndCheck(c, "${audit.name}", "Certificate Signing Service");
        expandAndCheck(c, "${audit.nodeId}", "node1");
        expandAndCheck(c, "${audit.requestId}", "");
        expandAndCheck(c, "${audit.action}", "Dancing");
        expandAndCheck(c, "${audit.component}", "Certificate Signing Service");
        expandAndCheck(c, "${audit.time}", String.valueOf(c.getAuditRecord().getMillis()));
        expandAndCheck(c, "${audit.message}", "CSR servlet is dancing!");
        expandAndCheck(c, "${audit.ipAddress}", "1.2.3.4");
        expandAndCheck(c, "${audit.user.name}", "");
        expandAndCheck(c, "${audit.user.id}", "");
        expandAndCheck(c, "${audit.user.idProv}", "0000000000000000ffffffffffffffff");
        expandAndCheck(c, "${audit.authType}", "");
    }

    @Test
    public void testAdminAuditRecordFields() throws Exception {
        AdminAuditRecord rec = new AdminAuditRecord(Level.WARNING, "node1", new Goid(346,31), "com.l7tech.MyEntity", "thename", 'U', "Updated thename", new Goid(0,-2), "alice", "483", "1.4.2.1");
        rec.setStrRequestId(new RequestId("222-333").toString());
        final AuditSinkPolicyEnforcementContext c = new AuditSinkPolicyEnforcementContext(rec, delegate(), context());
        expandAndCheck(c, "${audit.type}", "admin");
        expandAndCheck(c, "${audit.name}", "thename");
        expandAndCheck(c, "${audit.sequenceNumber}", String.valueOf(c.getAuditRecord().getSequenceNumber()));
        expandAndCheck(c, "${audit.nodeId}", "node1");
        expandAndCheck(c, "${audit.requestId}", "0000000000000222-333");
        expandAndCheck(c, "${audit.action}", "U");
        expandAndCheck(c, "${audit.time}", String.valueOf(c.getAuditRecord().getMillis()));
        expandAndCheck(c, "${audit.message}", "Updated thename");
        expandAndCheck(c, "${audit.ipAddress}", "1.4.2.1");
        expandAndCheck(c, "${audit.user.name}", "alice");
        expandAndCheck(c, "${audit.user.id}", "483");
        expandAndCheck(c, "${audit.user.idProv}", "0000000000000000fffffffffffffffe");
        expandAndCheck(c, "${audit.authType}", "");
        expandAndCheck(c, "${audit.thrown}", "");
        expandAndCheck(c, "${audit.entity.class}", "com.l7tech.MyEntity"); // only for admin records
        expandAndCheck(c, "${audit.entity.oid}", "000000000000015a000000000000001f");
    }

    @Test
    public void testAuditDetailFields() throws Exception {
        final AuditSinkPolicyEnforcementContext c = sinkcontext();
        AuditDetail[] details = c.getAuditRecord().getDetailsInOrder();
        expandAndCheck(c, "${audit.details}", details[0].toString() + ", " + details[1].toString());
        expandAndCheck(c, "${audit.details[0]}", details[0].toString());
        expandAndCheck(c, "${audit.details[1]}", details[1].toString());
        expandAndCheck(c, "${audit.details.1}", details[1].toString());
        expandAndCheck(c, "${audit.details.1.}", details[1].toString());
        expandAndCheck(c, "${audit.details.0.ordinal}", "0");
        expandAndCheck(c, "${audit.details.0.exception}", details[0].getException());
        expandAndCheck(c, "${audit.details.1.exception}", details[1].getException());
        expandAndCheck(c, "${audit.details.0.componentId}", "8711");
        expandAndCheck(c, "${audit.details.1.componentId}", "8712");
        expandAndCheck(c, "${audit.details.0.messageId}", "6");
        expandAndCheck(c, "${audit.details.1.messageId}", "4");
        expandAndCheck(c, "${audit.details.0.params}", "foomp");
        expandAndCheck(c, "${audit.details.0.params[0]}", "foomp");
        expandAndCheck(c, "${audit.details.1.params}", "twoomp, moretwoomp");
        expandAndCheck(c, "${audit.details.1.params[0]}", "twoomp");
        expandAndCheck(c, "${audit.details.1.params[1]}", "moretwoomp");
        expandAndCheck(c, "${audit.details.1.nonexistent}", "");
        expandAndCheck(c, "${audit.details.1.nonexistent[3]}", "");
        expandAndCheck(c, "${audit.details.}", "");
        expandAndCheck(c, "${audit.details..}", "");
        expandAndCheck(c, "${audit.details.4.}", "");
        expandAndCheck(c, "${audit.details.4.messageId}", "");

        c.getAuditRecord().getDetails().clear();
        expandAndCheck(c, "${audit.details}", "");
        expandAndCheck(c, "${audit.details[0]}", "");
        expandAndCheck(c, "${audit.details[1]}", "");
        expandAndCheck(c, "${audit.details.1}", "");
        expandAndCheck(c, "${audit.details.1.}", "");
        expandAndCheck(c, "${audit.details.0.ordinal}", "");
        expandAndCheck(c, "${audit.details.0.exception}", "");
        expandAndCheck(c, "${audit.details.1.exception}", "");
        expandAndCheck(c, "${audit.details.0.componentId}", "");
        expandAndCheck(c, "${audit.details.1.componentId}", "");
        expandAndCheck(c, "${audit.details.0.messageId}", "");
        expandAndCheck(c, "${audit.details.1.messageId}", "");
        expandAndCheck(c, "${audit.details.0.params}", "");
        expandAndCheck(c, "${audit.details.0.params[0]}", "");
        expandAndCheck(c, "${audit.details.1.params}", "");
        expandAndCheck(c, "${audit.details.1.params[0]}", "");
        expandAndCheck(c, "${audit.details.1.params[1]}", "");
        expandAndCheck(c, "${audit.details.1.nonexistent}", "");
        expandAndCheck(c, "${audit.details.0.nonexistent[3]}", "");
        expandAndCheck(c, "${audit.details.}", "");
        expandAndCheck(c, "${audit.details..}", "");
        expandAndCheck(c, "${audit.details.4.}", "");
        expandAndCheck(c, "${audit.details.4.messageId}", "");
    }

    @BugNumber(5485)
    @Test
    public void testIsPolicyExecutionAttempted() throws Exception {
        final AuditSinkPolicyEnforcementContext c = sinkcontext();
        expandAndCheck(c, "${audit.policyExecutionAttempted}", "false");
        c.getOriginalContext().setPolicyExecutionAttempted(true);
        expandAndCheck(c, "${audit.policyExecutionAttempted}", "true");
    }

    @Test
    public void testSecurePasswordSelector() throws Exception {
        SecurePassword test1 = new SecurePassword("test1");
        test1.setGoid(new Goid(0,990));
        test1.setDescription("Test Pass One");
        test1.setEncodedPassword("FOOOO");
        test1.setUsageFromVariable(true);

        SecurePassword test2 = new SecurePassword("test2");
        test2.setGoid(new Goid(0,991));
        test2.setDescription("Test Pass Two");
        test2.setEncodedPassword("BAAAAR");
        test2.setUsageFromVariable(true);

        SecurePasswordManager spm = new SecurePasswordManagerStub(test1, test2);
        ServerVariables.setSecurePasswordManager(spm);

        // Ensure usage from variables works as expected
        final PolicyEnforcementContext c = context();
        expandAndCheck(c, "${secpass.test1}", "test1");
        expandAndCheck(c, "${secpass.test1.name}", "test1");
        expandAndCheck(c, "${secpass.test1.description}", "Test Pass One");
        expandAndCheck(c, "${secpass.test1.plaintext}", "foooo");

        expandAndCheck(c, "${secpass.test2}", "test2");
        expandAndCheck(c, "${secpass.test2.name}", "test2");
        expandAndCheck(c, "${secpass.test2.description}", "Test Pass Two");
        expandAndCheck(c, "${secpass.test2.plaintext}", "baaaar");

        // Make sure usage from variable is respected
        test2.setUsageFromVariable(false);

        expandAndCheck(c, "${secpass.test2}", "");
        expandAndCheck(c, "${secpass.test2.name}", "");
        expandAndCheck(c, "${secpass.test2.description}", "");
        expandAndCheck(c, "${secpass.test2.plaintext}", "");
    }

    @Test
    public void testExpandPasswordOnlyVariable() throws Exception {
        final Audit audit = new LoggingAudit(logger);

        try {
            ServerVariables.setSecurePasswordManager( null );
            ServerVariables.expandPasswordOnlyVariable( audit, "${secpass.test1.plaintext}" );
            fail( "valid secpass references should hard fail if no secure password manager initialized" );
        } catch ( IllegalStateException e ) {
            // Ok
        }

        SecurePasswordManager spm = new SecurePasswordManagerStub(
                new SecurePassword("test1") {
                    {
                        setGoid(new Goid(0,1));
                        setEncodedPassword("TEST-PASSWORD1");
                        setUsageFromVariable(true);
                    } },
                new SecurePassword("test2") {
                    {
                        setGoid(new Goid(0,2));
                        setEncodedPassword("TEST-PASSWORD2");
                        setUsageFromVariable(true);
                    } },
                new SecurePassword("emptypass") {
                    {
                        setGoid(new Goid(0,3));
                        setEncodedPassword("");
                        setUsageFromVariable(true);
                    } },
                new SecurePassword("nullpass") {
                    {
                        setGoid(new Goid(0,4));
                        setEncodedPassword(null);
                        setUsageFromVariable(true);
                    } },
                new SecurePassword("nocontext") {
                    {
                        setGoid(new Goid(0,5));
                        setEncodedPassword("TEST-PASSWORD3");
                        setUsageFromVariable(false);
                    } }
        );
        ServerVariables.setSecurePasswordManager(spm);

        assertNull("null template shall expand to null", ServerVariables.expandPasswordOnlyVariable(audit, null));
        assertEquals("empty template shall expand to empty", "", ServerVariables.expandPasswordOnlyVariable(audit, ""));
        assertEquals("template with no variable references shall expand to itself", "asdfQEWER", ServerVariables.expandPasswordOnlyVariable(audit, "asdfQEWER"));
        assertEquals("template with invalid reference syntax shall expand to itself", "asdf${QEWER", ServerVariables.expandPasswordOnlyVariable(audit, "asdf${QEWER"));
        assertEquals("template referencing things other than secure passwords shall expand to itself", "${blarf}", ServerVariables.expandPasswordOnlyVariable(audit, "${blarf}"));
        assertEquals("template referencing things other than secure password plaintext shall expand to itself", "${secpass.test1.alias}", ServerVariables.expandPasswordOnlyVariable(audit, "${secpass.test1.alias}"));
        assertEquals("template referencing nonexistent secure password plaintext shall expand to itself", "${secpass.qwerasdf.plaintext}", ServerVariables.expandPasswordOnlyVariable(audit, "${secpass.qwerasdf.plaintext}"));
        assertEquals("template references valid secure pass plaintext shall expand to the plaintext", "test-password1", ServerVariables.expandPasswordOnlyVariable(audit, "${secpass.test1.plaintext}"));
        assertEquals("template referencing secure password that disallows use via context variable shall expand to itself", "${secpass.nocontext.plaintext}", ServerVariables.expandPasswordOnlyVariable(audit, "${secpass.nocontext.plaintext}"));
        assertEquals("template using an array deref 0 shall work normally", "test-password1", ServerVariables.expandPasswordOnlyVariable(audit, "${secpass.test1.plaintext[0]}"));
        assertEquals("template references to empty password expands to empty", "BEFOREAFTER", ServerVariables.expandPasswordOnlyVariable(audit, "BEFORE${secpass.emptypass.plaintext}AFTER"));
        assertEquals("template references to null password expands to empty", "BEFOREAFTER", ServerVariables.expandPasswordOnlyVariable(audit, "BEFORE${secpass.nullpass.plaintext}AFTER"));
        assertEquals("template using an array deref 1 shall fail as expected, expanding to empty string (lax mode)", "BEFOREAFTER", ServerVariables.expandPasswordOnlyVariable(audit, "BEFORE${secpass.test1.plaintext[1]}AFTER"));
        assertEquals("multiple references shall be expanded", "BEFOREtest-password1MIDDLEtest-password2AFTER", ServerVariables.expandPasswordOnlyVariable(audit, "BEFORE${secpass.test1.plaintext}MIDDLE${secpass.test2.plaintext}AFTER"));
    }

    @Test
    public void testExpandSinglePasswordOnlyVariable() throws Exception {
        final Audit audit = new LoggingAudit(logger);

        try {
            ServerVariables.setSecurePasswordManager( null );
            ServerVariables.expandPasswordOnlyVariable( audit, "${secpass.test1.plaintext}" );
            fail( "valid secpass references should hard fail if no secure password manager initialized" );
        } catch ( IllegalStateException e ) {
            // Ok
        }

        SecurePasswordManager spm = new SecurePasswordManagerStub(
                new SecurePassword("test1") {
                    {
                        setGoid(new Goid(0,1));
                        setEncodedPassword("TEST-PASSWORD1");
                        setUsageFromVariable(true);
                    } },
                new SecurePassword("test2") {
                    {
                        setGoid(new Goid(0,2));
                        setEncodedPassword("TEST-PASSWORD2");
                        setUsageFromVariable(true);
                    } },
                new SecurePassword("emptypass") {
                    {
                        setGoid(new Goid(0,3));
                        setEncodedPassword("");
                        setUsageFromVariable(true);
                    } },
                new SecurePassword("nullpass") {
                    {
                        setGoid(new Goid(0,4));
                        setEncodedPassword(null);
                        setUsageFromVariable(true);
                    } },
                new SecurePassword("nocontext") {
                    {
                        setGoid(new Goid(0,5));
                        setEncodedPassword("TEST-PASSWORD3");
                        setUsageFromVariable(false);
                    } }
        );
        ServerVariables.setSecurePasswordManager(spm);

        assertNull("null template shall expand to null", ServerVariables.expandSinglePasswordOnlyVariable(audit, null));
        assertEquals("empty template shall expand to empty", "", ServerVariables.expandSinglePasswordOnlyVariable(audit, ""));
        assertEquals("template with no variable references shall expand to itself", "asdfQEWER", ServerVariables.expandSinglePasswordOnlyVariable(audit, "asdfQEWER"));
        assertEquals("template with invalid reference syntax shall expand to itself", "asdf${QEWER", ServerVariables.expandSinglePasswordOnlyVariable(audit, "asdf${QEWER"));
        assertEquals("template referencing things other than secure passwords shall expand to itself", "${blarf}", ServerVariables.expandSinglePasswordOnlyVariable(audit, "${blarf}"));
        assertEquals("template referencing things other than secure password plaintext shall expand to itself", "${secpass.test1.alias}", ServerVariables.expandSinglePasswordOnlyVariable(audit, "${secpass.test1.alias}"));
        assertEquals("template referencing nonexistent secure password plaintext shall expand to itself", "${secpass.qwerasdf.plaintext}", ServerVariables.expandSinglePasswordOnlyVariable(audit, "${secpass.qwerasdf.plaintext}"));
        assertEquals("template references valid secure pass plaintext shall expand to the plaintext", "test-password1", ServerVariables.expandSinglePasswordOnlyVariable(audit, "${secpass.test1.plaintext}"));
        assertEquals("template referencing secure password that disallows use via context variable shall still be honored", "test-password3", ServerVariables.expandSinglePasswordOnlyVariable(audit, "${secpass.nocontext.plaintext}"));
        assertEquals("template using an array deref is not recognized or supported", "${secpass.test1.plaintext[0]}", ServerVariables.expandSinglePasswordOnlyVariable(audit, "${secpass.test1.plaintext[0]}"));
        assertEquals("template references with any leading text is not recognized", "BEFORE${secpass.emptypass.plaintext}", ServerVariables.expandSinglePasswordOnlyVariable(audit, "BEFORE${secpass.emptypass.plaintext}"));
        assertEquals("template references with any trailing text is not recognized", "${secpass.emptypass.plaintext}AFTER", ServerVariables.expandSinglePasswordOnlyVariable(audit, "${secpass.emptypass.plaintext}AFTER"));
        assertEquals("template references to null password expands to empty", "", ServerVariables.expandSinglePasswordOnlyVariable(audit, "${secpass.nullpass.plaintext}"));
        assertEquals("template using an array deref 1 shall not be recognized as a secpass ref", "${secpass.test1.plaintext[1]}", ServerVariables.expandSinglePasswordOnlyVariable(audit, "${secpass.test1.plaintext[1]}"));
        assertEquals("multiple references shall not be recognized", "${secpass.test1.plaintext}${secpass.test2.plaintext}", ServerVariables.expandSinglePasswordOnlyVariable(audit, "${secpass.test1.plaintext}${secpass.test2.plaintext}"));
    }

    @Test
    public void testSoapKnobContextVariables() throws Exception {
        final PolicyEnforcementContext c = context();
        expandAndCheck(c, "${request.soap.version}", "");
        expandAndCheck(c, "${request.soap.envElopeNs}", "");
        expandAndCheck(c, "${response.soap.version}", "");
        expandAndCheck(c, "${response.soap.envElopeNs}", "");
        c.getRequest().initialize(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT));
        c.getResponse().initialize(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT_S12));
        expandAndCheck(c, "${request.soap.version}", "1.1");
        expandAndCheck(c, "${request.soap.envElopeNs}", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);
        expandAndCheck(c, "${response.soap.version}", "1.2");
        expandAndCheck(c, "${response.soap.envElopeNs}", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE);
    }

    @BugId("DE269283")
    @Test
    public void testCannotGetSoapNamespaceDoNotLogStacktrace() throws Exception {
        final TestHandler handler = new TestHandler(false);
        handler.flush();
        serverVariablesLogger.addHandler(handler);
        final PolicyEnforcementContext c = context();
        c.getRequest().attachKnob(new GetPayloadNamesThrowingSoapKnob(), SoapKnob.class);

        try {
            String[] usedVars = Syntax.getReferencedNames(SOAP_NAMESPACE);
            Map<String, Object> vars = c.getVariableMap(usedVars, auditor);
            ExpandVariables.process(SOAP_NAMESPACE, vars, auditor);
            assertEquals(1, TestHandler.getNumLogs());
            assertTrue(TestHandler.isAuditPresentContaining("WARNING: Couldnt get SOAP namespace"));
            assertFalse(TestHandler.isAuditPresentContaining("mocking exception"));
        } finally {
            serverVariablesLogger.removeHandler(handler);
            handler.flush();
        }
    }

    @Test
    public void testBuildVariables() throws Exception {
        final PolicyEnforcementContext context = context();
        expandAndCheck(context, "${ssgnode.build.label}", BuildInfo.getProductVersion());
        expandAndCheck(context, "${ssgnode.build.detail}", BuildInfo.getLongBuildString());
        expandAndCheck(context, "${ssgnode.build.number}", BuildInfo.getBuildNumber());
        expandAndCheck(context, "${ssgnode.build.version}", BuildInfo.getFormalProductVersion());
        expandAndCheck(context, "${ssgnode.build.version.major}", BuildInfo.getProductVersionMajor());
        expandAndCheck(context, "${ssgnode.build.version.minor}", BuildInfo.getProductVersionMinor());
        expandAndCheck(context, "${ssgnode.build.version.subminor}", BuildInfo.getProductVersionSubMinor());
    }

    @Test
    public void testMessagePartMaxSize() throws Exception {
        final PolicyEnforcementContext context = context();
        expandAndCheck(context, "${request.parts.1.body}", "<myrequest/>");
        expandAndCheck(context, "${response.parts.1.body}", "<myresponse/>");

        SyspropUtil.setProperty(PROP_TEMPLATE_MAX_SIZE, "12");

        expandAndCheck(context, "${request.parts.1.body}", "<myrequest/>");
        expandAndCheck(context, "${response.parts.1.body}", "");

        SyspropUtil.setProperty(PROP_TEMPLATE_MAX_SIZE, "5");

        expandAndCheck(context, "${request.parts.1.body}", "");
        expandAndCheck(context, "${response.parts.1.body}", "");
    }

    @Test
    public void testGatewayTime_DefaultFormatting() throws Exception {
        testBuiltInVariablePreFangtooth_Formatting("${gateway.time}", ServerVariables.class, utcTimeZone);
    }

    @Test
    public void testRequestTime_DefaultFormatting() throws Exception {
        testBuiltInVariablePreFangtooth_Formatting("${request.time}", PolicyEnforcementContextFactory.class, utcTimeZone);
    }

    @Test
    public void testGatewayTime_LocalFormatting() throws Exception {
        testBuiltInVariablePreFangtooth_Formatting("${gateway.time.local}", ServerVariables.class, localTimeZone);
    }

    @Test
    public void testRequestTime_LocalFormatting() throws Exception {
        testBuiltInVariablePreFangtooth_Formatting("${request.time.local}", PolicyEnforcementContextFactory.class, localTimeZone);
    }

    @Test
    public void testGatewayTime_CustomFormatSuffix() throws Exception {
        testBuiltInVariablePreFangtooth_Formatting("${gateway.time.yyyyMMdd'T'HH:mm:ss}", ServerVariables.class, utcTimeZone, "yyyyMMdd'T'HH:mm:ss");
    }

    @Test
    public void testRequestTime_CustomFormatSuffix() throws Exception {
        testBuiltInVariablePreFangtooth_Formatting("${request.time.yyyyMMdd'T'HH:mm:ss}", PolicyEnforcementContextFactory.class, utcTimeZone, "yyyyMMdd'T'HH:mm:ss");
    }

    @Test
    public void testGatewayTime_LocalCustomFormatSuffix() throws Exception {
        testBuiltInVariablePreFangtooth_Formatting("${gateway.time.local.yyyyMMdd'T'HH:mm:ss}", ServerVariables.class, localTimeZone, "yyyyMMdd'T'HH:mm:ss");
    }

    @Test
    public void testRequestTime_LocalCustomFormatSuffix() throws Exception {
        testBuiltInVariablePreFangtooth_Formatting("${request.time.local.yyyyMMdd'T'HH:mm:ss}", PolicyEnforcementContextFactory.class, localTimeZone, "yyyyMMdd'T'HH:mm:ss");
    }

    @Test
    public void testGatewayTime_UtcCustomFormatSuffix() throws Exception {
        testBuiltInVariablePreFangtooth_Formatting("${gateway.time.utc.yyyyMMdd'T'HH:mm:ss}", ServerVariables.class, utcTimeZone, "yyyyMMdd'T'HH:mm:ss");
    }

    @Test
    public void testRequestTime_UtcCustomFormatSuffix() throws Exception {
        testBuiltInVariablePreFangtooth_Formatting("${request.time.utc.yyyyMMdd'T'HH:mm:ss}", PolicyEnforcementContextFactory.class, utcTimeZone, "yyyyMMdd'T'HH:mm:ss");
    }

    @Test
    public void testGatewayTime_Millis() throws Exception {
        testBuiltInVariablePreFangtooth_Timestamp("${gateway.time.millis}", ServerVariables.class, false);
    }

    @Test
    public void testRequestTime_Millis() throws Exception {
        testBuiltInVariablePreFangtooth_Timestamp("${request.time.millis}", PolicyEnforcementContextFactory.class, false);
    }

    @Test
    public void testGatewayTime_Seconds() throws Exception {
        testBuiltInVariablePreFangtooth_Timestamp("${gateway.time.seconds}", ServerVariables.class, true);
    }

    @Test
    public void testRequestTime_Seconds() throws Exception {
        testBuiltInVariablePreFangtooth_Timestamp("${request.time.seconds}", PolicyEnforcementContextFactory.class, true);
    }


    @Test
    public void testSystemVariables() throws Exception {
        final PolicyEnforcementContext context = context();
        expandAndCheck(context, "${ssgnode.hostname}", ServerConfig.getInstance().getHostname());
    }

    @BugId("SSG-8033")
    @Test
    public void testResponseCookieOverwritePath() throws Exception {
        final PolicyEnforcementContext context = context();
        // default is true
        assertTrue(context.isOverwriteResponseCookiePath());
        context.setVariable("response.cookie.overwritePath", false);
        assertFalse(context.isOverwriteResponseCookiePath());
    }

    @BugId("SSG-8033")
    @Test
    public void testResponseCookieOverwriteDomain() throws Exception {
        final PolicyEnforcementContext context = context();
        // default is true
        assertTrue(context.isOverwriteResponseCookieDomain());
        context.setVariable("response.cookie.overwriteDomain", false);
        assertFalse(context.isOverwriteResponseCookieDomain());
    }

    @Test
    public void testRequestMQTTMessageType() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMessageType(MQTTRequestKnob.MessageType.PUBLISH);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals("PUBLISH", context.getVariable("request.mqtt.messageType"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTMessageTypeNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.messageType");
    }

    @Test
    public void testRequestMQTTClientIdentifier() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        String clientId = UUID.randomUUID().toString();
        mqttRequestKnobStub.setClientIdentifier(clientId);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals(clientId, context.getVariable("request.mqtt.clientIdentifier"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTClientIdentifierNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.clientIdentifier");
    }

    @Test
    public void testRequestMQTTUserName() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        String userName = "MyUser";
        mqttRequestKnobStub.setUserName(userName);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals(userName, context.getVariable("request.mqtt.userName"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTUserNameNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.userName");
    }

    @Test
    public void testRequestMQTTUserPassword() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        String userPassword = "MyPassword";
        mqttRequestKnobStub.setUserPassword(userPassword);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals(userPassword, context.getVariable("request.mqtt.userPassword"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTUserPasswordNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.userPassword");
    }

    @Test
    public void testRequestMQTTConnectCleanSession() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTConnectParameters().setCleanSession(true);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals(true, context.getVariable("request.mqtt.connect.cleanSession"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectCleanSessionNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.connect.cleanSession");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectCleanSessionNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttConnectParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.connect.cleanSession");
    }

    @Test
    public void testRequestMQTTConnectKeepAlive() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTConnectParameters().setKeepAlive(123);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals(123, context.getVariable("request.mqtt.connect.keepAlive"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectKeepAliveNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.connect.keepAlive");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectKeepAliveNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttConnectParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.connect.keepAlive");
    }

    @Test
    public void testRequestMQTTConnectWillPresent() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTConnectParameters().setWillPresent(true);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals(true, context.getVariable("request.mqtt.connect.will.present"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectWillPresentNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.connect.will.present");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectWillPresentNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttConnectParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.connect.will.present");
    }

    @Test
    public void testRequestMQTTConnectWillTopic() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTConnectParameters().setWillPresent(true);
        mqttRequestKnobStub.getMQTTConnectParameters().setWillTopic("MyWill");
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals("MyWill", context.getVariable("request.mqtt.connect.will.topic"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectWillTopicNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.connect.will.topic");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectWillTopicNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttConnectParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.connect.will.topic");
    }

    @Test
    public void testRequestMQTTConnectWillMessage() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTConnectParameters().setWillPresent(true);
        mqttRequestKnobStub.getMQTTConnectParameters().setWillMessage("MyWillMessage");
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals("MyWillMessage", context.getVariable("request.mqtt.connect.will.message"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectWillMessageNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.connect.will.message");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectWillMessageNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttConnectParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.connect.will.message");
    }

    @Test
    public void testRequestMQTTConnectWillQOS() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTConnectParameters().setWillPresent(true);
        mqttRequestKnobStub.getMQTTConnectParameters().setWillQOS(2);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals(2, context.getVariable("request.mqtt.connect.will.qos"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectWillQOSNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.connect.will.qos");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectWillQOSNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttConnectParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.connect.will.qos");
    }

    @Test
    public void testRequestMQTTConnectWillRetain() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTConnectParameters().setWillPresent(true);
        mqttRequestKnobStub.getMQTTConnectParameters().setWillRetain(true);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals(true, context.getVariable("request.mqtt.connect.will.retain"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectWillRetainNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.connect.will.retain");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTConnectWillRetainNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttConnectParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.connect.will.retain");
    }

    @Test
    public void testRequestMQTTDisconnectForced() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTDisconnectParameters().setForced(true);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals(true, context.getVariable("request.mqtt.disconnect.forced"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTDisconnectForcedNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.disconnect.forced");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTDisconnectForcedNoMQTTKnobDisconnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttDisconnectParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.disconnect.forced");
    }

    @Test
    public void testRequestMQTTPublishTopic() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTPublishParameters().setTopic("MyTopic");
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals("MyTopic", context.getVariable("request.mqtt.publish.topic"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTPublishTopicNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.publish.topic");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTPublishTopicNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttPublishParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.publish.topic");
    }

    @Test
    public void testRequestMQTTPublishQOS() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTPublishParameters().setQos(2);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals(2, context.getVariable("request.mqtt.publish.qos"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTPublishQOSNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.publish.qos");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTPublishQOSNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttPublishParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.publish.qos");
    }

    @Test
    public void testRequestMQTTPublishRetain() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTPublishParameters().setRetain(true);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals(true, context.getVariable("request.mqtt.publish.retain"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTPublishRetainNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.publish.retain");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTPublishRetainNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttPublishParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.publish.retain");
    }

    @Test
    public void testRequestMQTTSubscribeSubscriptions() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTSubscribeParameters().setSubscriptions("MySubscriptions");
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals("MySubscriptions", context.getVariable("request.mqtt.subscribe.subscriptions"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTSubscribeSubscriptionsNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.subscribe.subscriptions");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTSubscribeSubscriptionsNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttSubscribeParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.subscribe.subscriptions");
    }

    @Test
    public void testRequestMQTTUnsubscribeSubscriptions() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        //noinspection ConstantConditions
        mqttRequestKnobStub.getMQTTUnsubscribeParameters().setSubscriptions("MySubscriptions");
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        assertEquals("MySubscriptions", context.getVariable("request.mqtt.unsubscribe.subscriptions"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTUnsubscribeSubscriptionsNoMQTTKnob() throws Exception {
        context().getVariable("request.mqtt.unsubscribe.subscriptions");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testRequestMQTTUnsubscribeSubscriptionsNoMQTTKnobConnectParameters() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTRequestKnobStub mqttRequestKnobStub = new MQTTRequestKnobStub();
        mqttRequestKnobStub.setMqttUnsubscribeParametersStub(null);
        context.getRequest().attachKnob(mqttRequestKnobStub, MQTTRequestKnob.class);
        context().getVariable("request.mqtt.unsubscribe.subscriptions");
    }
    @Test
    public void testResponseMQTTConnectResponseCodeGet() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTConnectResponseKnobStub mqttConnectResponseKnobStub = new MQTTConnectResponseKnobStub();
        //noinspection ConstantConditions
        mqttConnectResponseKnobStub.setResponseCode(2);
        context.getResponse().attachKnob(mqttConnectResponseKnobStub, MQTTConnectResponseKnob.class);
        assertEquals(2, context.getVariable("response.mqtt.connect.responseCode"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testResponseMQTTConnectResponseCodeGetNoMQTTKnob() throws Exception {
        context().getVariable("response.mqtt.connect.responseCode");
    }

    @Test
    public void testResponseMQTTConnectResponseCodeSet() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTConnectResponseKnobStub mqttConnectResponseKnobStub = new MQTTConnectResponseKnobStub();
        //noinspection ConstantConditions
        context.getResponse().attachKnob(mqttConnectResponseKnobStub, MQTTConnectResponseKnob.class);
        context.setVariable("response.mqtt.connect.responseCode", 2);
        assertEquals(2, context.getVariable("response.mqtt.connect.responseCode"));
        assertEquals(2, mqttConnectResponseKnobStub.getResponseCode());
    }

    @Test
    public void testResponseMQTTConnectResponseCodeSetNoKnob() throws Exception {
        final PolicyEnforcementContext context = context();
        context.setVariable("response.mqtt.connect.responseCode", 2);
        assertEquals(2, context.getVariable("response.mqtt.connect.responseCode"));
        assertNotNull(context.getResponse().getKnob(MQTTConnectResponseKnob.class));
        //noinspection ConstantConditions
        assertEquals(2, context.getResponse().getKnob(MQTTConnectResponseKnob.class).getResponseCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResponseMQTTConnectResponseCodeSetBadValue() throws Exception {
        final PolicyEnforcementContext context = context();
        context.setVariable("response.mqtt.connect.responseCode", "abc");
    }

    @Test
    public void testResponseMQTTConnectSessionPresentGet() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTConnectResponseKnobStub mqttConnectResponseKnobStub = new MQTTConnectResponseKnobStub();
        //noinspection ConstantConditions
        mqttConnectResponseKnobStub.setSessionPresent("true");
        context.getResponse().attachKnob(mqttConnectResponseKnobStub, MQTTConnectResponseKnob.class);
        assertEquals("true", context.getVariable("response.mqtt.connect.sessionPresent"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testResponseMQTTConnectSessionPresentGetNoMQTTKnob() throws Exception {
        context().getVariable("response.mqtt.connect.sessionPresent");
    }

    @Test
    public void testResponseMQTTConnectSessionPresentSet() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTConnectResponseKnobStub mqttConnectResponseKnobStub = new MQTTConnectResponseKnobStub();
        //noinspection ConstantConditions
        context.getResponse().attachKnob(mqttConnectResponseKnobStub, MQTTConnectResponseKnob.class);
        context.setVariable("response.mqtt.connect.sessionPresent", "true");
        assertEquals("true", context.getVariable("response.mqtt.connect.sessionPresent"));
        assertEquals("true", mqttConnectResponseKnobStub.isSessionPresent());
    }

    @Test
    public void testResponseMQTTConnectSessionPresentSetNoKnob() throws Exception {
        final PolicyEnforcementContext context = context();
        context.setVariable("response.mqtt.connect.sessionPresent", "true");
        assertEquals("true", context.getVariable("response.mqtt.connect.sessionPresent"));
        assertNotNull(context.getResponse().getKnob(MQTTConnectResponseKnob.class));
        //noinspection ConstantConditions
        assertEquals("true", context.getResponse().getKnob(MQTTConnectResponseKnob.class).isSessionPresent());
    }


    @Test
    public void testResponseMQTTSubscribeGrantedQOSGet() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTSubscribeResponseKnobStub mqttSubscribeResponseKnobStub = new MQTTSubscribeResponseKnobStub();
        //noinspection ConstantConditions
        mqttSubscribeResponseKnobStub.setGrantedQOS("{1,2,0}");
        context.getResponse().attachKnob(mqttSubscribeResponseKnobStub, MQTTSubscribeResponseKnob.class);
        assertEquals("{1,2,0}", context.getVariable("response.mqtt.subscribe.grantedQOS"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void testResponseMQTTSubscribeGrantedQOSGetNoMQTTKnob() throws Exception {
        context().getVariable("response.mqtt.subscribe.grantedQOS");
    }

    @Test
    public void testResponseMQTTSubscribeGrantedQOSSet() throws Exception {
        final PolicyEnforcementContext context = context();
        MQTTSubscribeResponseKnobStub mqttSubscribeResponseKnobStub = new MQTTSubscribeResponseKnobStub();
        //noinspection ConstantConditions
        context.getResponse().attachKnob(mqttSubscribeResponseKnobStub, MQTTSubscribeResponseKnob.class);
        context.setVariable("response.mqtt.subscribe.grantedQOS", "{1,2,0}");
        assertEquals("{1,2,0}", context.getVariable("response.mqtt.subscribe.grantedQOS"));
        assertEquals("{1,2,0}", mqttSubscribeResponseKnobStub.getGrantedQOS());
    }

    @Test
    public void testResponseMQTTSubscribeGrantedQOSSetNoKnob() throws Exception {
        final PolicyEnforcementContext context = context();
        context.setVariable("response.mqtt.subscribe.grantedQOS", "{1,2,0}");
        assertEquals("{1,2,0}", context.getVariable("response.mqtt.subscribe.grantedQOS"));
        assertNotNull(context.getResponse().getKnob(MQTTSubscribeResponseKnob.class));
        //noinspection ConstantConditions
        assertEquals("{1,2,0}", context.getResponse().getKnob(MQTTSubscribeResponseKnob.class).getGrantedQOS());
    }

    // - PRIVATE

    private void testBuiltInVariablePreFangtooth_Formatting(final String builtInDateVarExp,
                                                            final Class classWithTimeSource,
                                                            TimeZone timeZone) throws Exception{
        testBuiltInVariablePreFangtooth_Formatting(builtInDateVarExp, classWithTimeSource, timeZone, DateUtils.ISO8601_PATTERN);
    }

    private void testBuiltInVariablePreFangtooth_Formatting(final String builtInDateVarExp,
                                                            final Class classWithTimeSource,
                                                            TimeZone expectedTimeZone,
                                                            String expectedFormat)
            throws Exception{
        final Pair<Long, String> expectedActualPair = processWithTimeSource(builtInDateVarExp, classWithTimeSource);

        final SimpleDateFormat format = new SimpleDateFormat(expectedFormat);
        format.setTimeZone(expectedTimeZone);
        format.setLenient(false);
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(expectedActualPair.left);
        assertEquals(format.format(cal.getTime()), expectedActualPair.right);
    }

    private void testBuiltInVariablePreFangtooth_Timestamp(final String builtInDateVarExp,
                                                           final Class classWithTimeSource,
                                                           final boolean seconds)
            throws Exception {

        final Pair<Long, String> expectedActualPair = processWithTimeSource(builtInDateVarExp, classWithTimeSource);
        if (seconds) {
            assertEquals(Long.valueOf(expectedActualPair.left / 1000L), Long.valueOf(expectedActualPair.right));
        } else {
            assertEquals(expectedActualPair.left, Long.valueOf(expectedActualPair.right));
        }
    }

    private Pair<Long, String> processWithTimeSource(final String builtInDateVarExp, Class classWithTimeSource) throws Exception {
        return doWithTimeSource(classWithTimeSource, new Functions.Nullary<String>() {
                @Override
                public String call() {
                    final PolicyEnforcementContext context = context();
                    String[] usedVars = Syntax.getReferencedNames(builtInDateVarExp);
                    Map<String, Object> vars = context.getVariableMap(usedVars, auditor);
                    String actual = ExpandVariables.process(builtInDateVarExp, vars, auditor);
                    assertFalse("No audits are expected", auditor.iterator().hasNext());
                    System.out.println("Actual: " + actual);
                    return actual;
                }
            });
    }

    private <T> Pair<Long, T> doWithTimeSource(final Class classWithTimeSource, final Functions.Nullary<T> function)
            throws Exception{
        final long currentTime = System.currentTimeMillis();
        final Method setTimeSourceMethod = classWithTimeSource.getDeclaredMethod("setTimeSource", TimeSource.class);
        try {
            setTimeSourceMethod.invoke(classWithTimeSource, new TimeSource() {
                @Override
                public long currentTimeMillis() {
                    return currentTime;
                }
            });

            final T returnVal = function.call();
            return new Pair<>(currentTime, returnVal);
        } finally {
            setTimeSourceMethod.invoke(classWithTimeSource, new TimeSource());
        }
    }

    private PolicyEnforcementContext context() {
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(REQUEST_BODY));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(RESPONSE_BODY));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private PolicyEnforcementContext contextHttp() {
        PolicyEnforcementContext context = context();

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("POST");
        mockRequest.setContentType(ContentTypeHeader.XML_DEFAULT.getFullValue());
        mockRequest.addHeader(HttpConstants.HEADER_CONTENT_TYPE, ContentTypeHeader.XML_DEFAULT.getFullValue());
        mockRequest.setParameter("single", "1");
        mockRequest.setParameter("multi", new String[]{"1", "2", "3", "4", "5"});
        mockRequest.setQueryString("single=1&multi=1&multi=2&multi=3&multi=4&multi=5");
        mockRequest.addHeader(HttpConstants.HEADER_CONNECTION, ContentTypeHeader.XML_DEFAULT.getFullValue());
        mockRequest.addHeader(HttpConstants.HEADER_CONNECTION, ContentTypeHeader.TEXT_DEFAULT.getFullValue());
        mockRequest.addHeader(HttpConstants.HEADER_COOKIE, ContentTypeHeader.XML_DEFAULT.getFullValue());
        context.getRequest().attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        final HeadersKnob headersKnob = context.getRequest().getHeadersKnob();
        headersKnob.addHeader(HttpConstants.HEADER_CONTENT_TYPE, ContentTypeHeader.XML_DEFAULT.getFullValue(), HEADER_TYPE_HTTP);
        headersKnob.addHeader(HttpConstants.HEADER_CONNECTION, ContentTypeHeader.XML_DEFAULT.getFullValue(), HEADER_TYPE_HTTP);
        headersKnob.addHeader(HttpConstants.HEADER_CONNECTION, ContentTypeHeader.TEXT_DEFAULT.getFullValue(), HEADER_TYPE_HTTP);
        headersKnob.addHeader(HttpConstants.HEADER_COOKIE, ContentTypeHeader.XML_DEFAULT.getFullValue(), HEADER_TYPE_HTTP);

        return context;
    }

    private PolicyEnforcementContext delegate() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
    }

    private AuditSinkPolicyEnforcementContext sinkcontext() {
        return new AuditSinkPolicyEnforcementContext(auditRecord(), delegate(), context());
    }

    @SuppressWarnings({"deprecation"})
    AuditRecord auditRecord() {
        AuditRecord auditRecord = new MessageSummaryAuditRecord(Level.INFO, "node1", "req4545", AssertionStatus.NONE, "3.2.1.1", AUDITED_REQUEST_XML, 4833, AUDITED_RESPONSE_XML, 9483, 200, 232, new Goid(0,8859), "ACMEWarehouse",
                "listProducts", true, SecurityTokenType.HTTP_BASIC, new Goid(0,-2), "alice", "41123", new Functions.Nullary<Goid>() {
            @Override
            public Goid call() {
                return new Goid(0, 49585);
            }
        });
        //noinspection deprecation
        auditRecord.setGoid(new Goid(1234,9777L));
        auditRecord.setStrRequestId(new RequestId("222-333").toString());
        final AuditDetail detail1 = new AuditDetail(Messages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"foomp"}, new IllegalArgumentException("Exception for foomp detail"));
        detail1.setOrdinal(0);
        detail1.setComponentId(8711);
        auditRecord.getDetails().add(detail1);
        final AuditDetail detail2 = new AuditDetail(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"twoomp", "moretwoomp"}, new IllegalArgumentException("Exception for twoomp detail"));
        detail2.setOrdinal(1);
        detail2.setComponentId(8712);
        auditRecord.getDetails().add(detail2);
        return auditRecord;
    }

    private void expandAndCheck(PolicyEnforcementContext context, String expression, String expectedValue) throws IOException, PolicyAssertionException {
        String[] usedVars = Syntax.getReferencedNames(expression);
        Map<String, Object> vars = context.getVariableMap(usedVars, auditor);
        String expanded = ExpandVariables.process(expression, vars, auditor);
        assertEquals(expression, expectedValue, expanded);
    }

    private PolicyEnforcementContext context(User user) {
        final PolicyEnforcementContext context = context();
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(user, new OpaqueSecurityToken()));
        return context;
    }

    private PolicyEnforcementContext responseContext(User user) {
        final PolicyEnforcementContext context = context();
        context.getAuthenticationContext(context.getResponse()).addAuthenticationResult(new AuthenticationResult(user, new OpaqueSecurityToken()));
        return context;
    }

    private void addTextNode(Message message, String text) throws Exception {
        Document doc = message.getXmlKnob().getDocumentWritable();
        doc.getDocumentElement().appendChild(doc.createTextNode(text));
    }

    private class MQTTRequestKnobStub implements MQTTRequestKnob {
        private MessageType messageType;
        private String clientIdentifier;
        private String userName;
        private String userPassword;
        private MQTTConnectParametersStub mqttConnectParametersStub = new MQTTConnectParametersStub();
        private MQTTPublishParametersStub mqttPublishParametersStub = new MQTTPublishParametersStub();
        private MQTTSubscribeParametersStub mqttSubscribeParametersStub = new MQTTSubscribeParametersStub();
        private MQTTUnsubscribeParametersStub mqttUnsubscribeParametersStub = new MQTTUnsubscribeParametersStub();
        private MQTTDisconnectParametersStub mqttDisconnectParametersStub = new MQTTDisconnectParametersStub();

        @NotNull
        @Override
        public MessageType getMessageType() {
            return messageType;
        }

        @Nullable
        @Override
        public String getClientIdentifier() {
            return clientIdentifier;
        }

        @Nullable
        @Override
        public String getUserName() {
            return userName;
        }

        @Nullable
        @Override
        public String getUserPassword() {
            return userPassword;
        }

        @Nullable
        @Override
        public X509Certificate[] getClientCertificate() throws IOException {
            return null;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Nullable
        @Override
        public MQTTConnectParametersStub getMQTTConnectParameters() {
            return mqttConnectParametersStub;
        }

        public void setMqttConnectParametersStub(MQTTConnectParametersStub mqttConnectParametersStub) {
            this.mqttConnectParametersStub = mqttConnectParametersStub;
        }

        @Nullable
        @Override
        public MQTTDisconnectParametersStub getMQTTDisconnectParameters() {
            return mqttDisconnectParametersStub;
        }

        public void setMqttDisconnectParametersStub(MQTTDisconnectParametersStub mqttDisconnectParametersStub) {
            this.mqttDisconnectParametersStub = mqttDisconnectParametersStub;
        }

        @Nullable
        @Override
        public MQTTPublishParametersStub getMQTTPublishParameters() {
            return mqttPublishParametersStub;
        }

        public void setMqttPublishParametersStub(MQTTPublishParametersStub mqttPublishParametersStub) {
            this.mqttPublishParametersStub = mqttPublishParametersStub;
        }

        @Nullable
        @Override
        public MQTTSubscribeParametersStub getMQTTSubscribeParameters() {
            return mqttSubscribeParametersStub;
        }

        public void setMqttSubscribeParametersStub(MQTTSubscribeParametersStub mqttSubscribeParametersStub) {
            this.mqttSubscribeParametersStub = mqttSubscribeParametersStub;
        }

        @Nullable
        @Override
        public MQTTUnsubscribeParametersStub getMQTTUnsubscribeParameters() {
            return mqttUnsubscribeParametersStub;
        }

        @Nullable
        @Override
        public MQTTQOS2Proxy getMQTTQOS2Proxy() {
            return null;
        }

        public void setMqttUnsubscribeParametersStub(MQTTUnsubscribeParametersStub mqttUnsubscribeParametersStub) {
            this.mqttUnsubscribeParametersStub = mqttUnsubscribeParametersStub;
        }

        @Override
        public String getRemoteAddress() {
            return null;
        }

        @Override
        public String getRemoteHost() {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public String getLocalAddress() {
            return null;
        }

        @Override
        public String getLocalHost() {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 0;
        }

        @Override
        public int getLocalListenerPort() {
            return 0;
        }

        public void setMessageType(MessageType messageType) {
            this.messageType = messageType;
        }

        public void setClientIdentifier(String clientIdentifier) {
            this.clientIdentifier = clientIdentifier;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public void setUserPassword(String userPassword) {
            this.userPassword = userPassword;
        }

        private class MQTTConnectParametersStub implements MQTTConnectParameters{
            private boolean cleanSession;
            private int keepAlive;
            private boolean willPresent;
            private String willTopic;
            private String willMessage;
            private int willQOS;
            private boolean willRetain;

            @Override
            public boolean isCleanSession() {
                return cleanSession;
            }

            @Override
            public int getKeepAlive() {
                return keepAlive;
            }

            @Override
            public boolean isWillPresent() {
                return willPresent;
            }

            @Override
            public String getWillTopic() {
                return willTopic;
            }

            @Override
            public String getWillMessage() {
                return willMessage;
            }

            @Override
            public int getWillQOS() {
                return willQOS;
            }

            @Override
            public boolean isWillRetain() {
                return willRetain;
            }

            public void setCleanSession(boolean cleanSession) {
                this.cleanSession = cleanSession;
            }

            public void setKeepAlive(int keepAlive) {
                this.keepAlive = keepAlive;
            }

            public void setWillPresent(boolean willPresent) {
                this.willPresent = willPresent;
            }

            public void setWillTopic(String willTopic) {
                this.willTopic = willTopic;
            }

            public void setWillMessage(String willMessage) {
                this.willMessage = willMessage;
            }

            public void setWillQOS(int willQOS) {
                this.willQOS = willQOS;
            }

            public void setWillRetain(boolean willRetain) {
                this.willRetain = willRetain;
            }
        }

        private class MQTTPublishParametersStub implements MQTTPublishParameters {
            private boolean retain;
            private int qos;
            private String topic;

            @Override
            public String getTopic() {
                return topic;
            }

            @Override
            public int getQOS() {
                return qos;
            }

            @Override
            public boolean isRetain() {
                return retain;
            }

            public void setRetain(boolean retain) {
                this.retain = retain;
            }

            public void setQos(int qos) {
                this.qos = qos;
            }

            public void setTopic(String topic) {
                this.topic = topic;
            }
        }

        private class MQTTSubscribeParametersStub implements MQTTSubscribeParameters {
            private String subscriptions;

            @Override
            public String getSubscriptions() {
                return subscriptions;
            }

            public void setSubscriptions(String subscriptions) {
                this.subscriptions = subscriptions;
            }
        }

        private class MQTTUnsubscribeParametersStub implements MQTTUnsubscribeParameters {
            private String subscriptions;

            @Override
            public String getSubscriptions() {
                return subscriptions;
            }

            public void setSubscriptions(String subscriptions) {
                this.subscriptions = subscriptions;
            }
        }

        private class MQTTDisconnectParametersStub implements MQTTDisconnectParameters{
            private boolean forced;

            @Override
            public boolean isForced() {
                return forced;
            }

            public void setForced(boolean forced) {
                this.forced = forced;
            }

        }
    }

    private class MQTTConnectResponseKnobStub implements MQTTConnectResponseKnob {
        private int responseCode = -1;
        private String sessionPresent;

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        @Override
        public String isSessionPresent() {
            return sessionPresent;
        }

        @Override
        public void setSessionPresent(String sessionPresent) {
            this.sessionPresent = sessionPresent;
        }

        @Nullable
        @Override
        public MQTTQOS2Proxy getMQTTQOS2Proxy() {
            return null;
        }
    }

    private class MQTTSubscribeResponseKnobStub implements MQTTSubscribeResponseKnob{
        private String grantedQOS;

        @Override
        public String getGrantedQOS() {
            return grantedQOS;
        }

        @Override
        public void setGrantedQOS(String grantedQOS) {
            this.grantedQOS = grantedQOS;
        }
    }

    private class GetPayloadNamesThrowingSoapKnob implements SoapKnob {

        @Override
        public QName[] getPayloadNames() throws IOException, SAXException, NoSuchPartException {
            throw new RuntimeException("mocking exception");
        }

        @Override
        public boolean isFault() throws SAXException, IOException, InvalidDocumentFormatException {
            return false;
        }

        @Override
        public SoapFaultDetail getFaultDetail() throws SAXException, IOException, InvalidDocumentFormatException {
            return null;
        }

        @Override
        public void setFaultDetail(SoapFaultDetail faultDetail) {

        }

        @Override
        public boolean isSecurityHeaderPresent() throws NoSuchPartException, IOException, SAXException {
            return false;
        }

        @Override
        public void invalidate() {

        }

        @Override
        public String getSoapAction() throws IOException, SAXException, NoSuchPartException {
            return null;
        }

        @Override
        public SoapVersion getSoapVersion() throws IOException, SAXException {
            return null;
        }

        @Override
        public String getSoapEnvelopeUri() throws IOException, SAXException {
            return null;
        }
    }
}
