package com.l7tech.external.assertions.ldapupdate.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.ldapupdate.LDAPUpdateAssertion;
import com.l7tech.external.assertions.ldapupdate.server.resource.JAXBResourceUnmarshaller;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.util.MockInjector;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.util.MockTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.support.GenericApplicationContext;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Timer;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test the LDAPUpdateAssertion.
 */
public class ServerLDAPUpdateAssertionTest {

    private static final Logger log = Logger.getLogger(ServerLDAPUpdateAssertionTest.class.getName());

    private ServerPolicyFactory serverPolicyFactory;
    @Mock
    private IdentityProviderFactory identityProviderFactory;
    @Mock
    private DirContext dirContext;
    @Mock
    private LdapIdentityProvider identityProvider;
    @Mock
    private InternalIdentityProvider internalIdentityProvider;
    @Mock
    LdapIdentityProviderConfig ldapIdentityProviderConfig;
    @Mock
    IdentityProviderType identityProviderType;
    @Mock
    JAXBResourceUnmarshaller unmarshaller;

    private LDAPUpdateAssertion assertion;

    private PolicyEnforcementContext peCtx;

    private final static String PROVIDER_NAME = "myldap";
    private final static Goid PROVIDER_GOID = new Goid(0,3);

    private Timer timer = new MockTimer();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        assertion = new LDAPUpdateAssertion();
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        Collection<IdentityProvider> ldapIdentityProviders = new ArrayList<IdentityProvider>();
        ldapIdentityProviders.add(identityProvider);
        when(ldapIdentityProviderConfig.getName()).thenReturn(PROVIDER_NAME);
        when(identityProvider.getConfig()).thenReturn(ldapIdentityProviderConfig);
        when(identityProvider.getConfig().getGoid()).thenReturn(PROVIDER_GOID);
        when(identityProvider.getConfig().type()).thenReturn(identityProviderType);
        when(identityProvider.getConfig().getSerializedProps()).thenReturn("<java version=\"1.6.0_01\" class=\"java.beans.XMLDecoder\"><object class=\"java.util.HashMap\"><void method=\"put\"><string>originalTemplateName</string><string>GenericLDAP</string></void></object></java>");
        when(identityProviderFactory.getProvider((Goid)any())).thenReturn(identityProvider);
        when(identityProviderFactory.findAllIdentityProviders()).thenReturn(ldapIdentityProviders);
        when(identityProvider.getBrowseContext()).thenReturn(dirContext);
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("identityProviderFactory", identityProviderFactory);
            put("managedBackgroundTimer", timer);
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        peCtx.setVariable(LDAPUpdateAssertion.PROVIDER_NAME, PROVIDER_NAME);
        peCtx.setVariable(LDAPUpdateAssertion.OPERATION, ServerLDAPUpdateAssertion.MANAGE);
    }

    @After
    public void after() throws Exception {
        ((MockTimer) timer).runNext();
    }


    @Test
    public void testCreateOperation() throws Exception {
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, CREATE_OPERATION_XML);
        peCtx.setVariable(LDAPUpdateAssertion.INJECTION_PROTECTION, "false");
        assertEquals(AssertionStatus.NONE, sass.checkRequest(peCtx));
        assertEquals(200, peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_STATUS));
        assertEquals("SUCCESS", peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_DETAIL));
        verify(dirContext, times(1)).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        verify(dirContext).close();
        assertEquals(1, sass.getCacheCount());
        peCtx.setVariable(LDAPUpdateAssertion.OPERATION, ServerLDAPUpdateAssertion.CLEAR_CACHE);
        assertEquals(AssertionStatus.NONE, sass.checkRequest(peCtx));
        assertEquals(0, sass.getCacheCount());
    }

    @Test
    public void testUpdateOperation() throws Exception {
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, UPDATE_OPERATION_XML);
        assertEquals(AssertionStatus.NONE, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, times(1)).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        verify(dirContext).close();
    }

    @Test
    public void testDeleteOperation() throws Exception {
        System.setProperty("com.l7tech.external.assertions.ldapupdate.enableDelete", "true");
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, DELETE_OPERATION_XML);
        assertEquals(AssertionStatus.NONE, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, times(1)).destroySubcontext(anyString());
        verify(dirContext).close();
    }

    @Test
    public void testMultipleOperation() throws Exception {
        System.setProperty("com.l7tech.external.assertions.ldapupdate.enableDelete", "true");
        System.setProperty("com.l7tech.external.assertions.ldapupdate.enableMultipleOperation", "true");
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, MULTPLE_OPERATION_XML);
        assertEquals(AssertionStatus.NONE, sass.checkRequest(peCtx));
        verify(dirContext, times(2)).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, times(2)).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, times(2)).destroySubcontext(anyString());
        verify(dirContext, times(6)).close();
    }

    @Test
    public void testDeleteOperationNoProperty() throws Exception {
        System.setProperty("com.l7tech.external.assertions.ldapupdate.enableDelete", "false");
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, DELETE_OPERATION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        verify(dirContext, never()).close();
    }

    @Test
    public void testMultipleOperationNoProperty() throws Exception {
        System.setProperty("com.l7tech.external.assertions.ldapupdate.enableMultipleOperation", "false");
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, MULTPLE_OPERATION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        verify(dirContext, never()).close();
    }

    @Test
    public void testMissingProviderName() throws Exception {
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.PROVIDER_NAME, "");
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        assertEquals(500, peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_STATUS));
        assertEquals(LDAPUpdateAssertion.PROVIDER_NAME + " was empty", peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_DETAIL));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
    }

    @Test
    public void testNotValidLDAPOperation() throws Exception {
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, INVALID_OPERATION_ACTION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        assertEquals(500, peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_STATUS));
        assertEquals("Unsupported operation - BLAH\n", peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_DETAIL));

        assertion = new LDAPUpdateAssertion();
        sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, EMPTY_OPERATION_ACTION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        assertEquals(500, peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_STATUS));
        assertEquals("operation is null\n", peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_DETAIL));
    }

    @Test
    public void testInvalidXML() throws Exception {
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, INVALID_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
    }

    @Test
    public void testNonOperationXML() throws Exception {
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, VALID_XML_BUT_NOT_LDAP_OPERATION);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
    }

    @Test
    public void testResourceMissing() throws Exception {
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
    }

    @Test
    public void testFindException() throws Exception {
        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        when(identityProviderFactory.findAllIdentityProviders()).thenThrow(new FindException());
        when(identityProvider.getBrowseContext()).thenReturn(dirContext);
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("identityProviderFactory", identityProviderFactory);
            put("managedBackgroundTimer", new MockTimer());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        peCtx.setVariable(LDAPUpdateAssertion.PROVIDER_NAME, PROVIDER_NAME);

        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, CREATE_OPERATION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
    }

    @Test
    public void testFindExceptionEmptyIdentityProviders() throws Exception {
        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        when(identityProviderFactory.findAllIdentityProviders()).thenReturn(new ArrayList<IdentityProvider>());
        when(identityProvider.getBrowseContext()).thenReturn(dirContext);
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("identityProviderFactory", identityProviderFactory);
            put("managedBackgroundTimer", new MockTimer());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        peCtx.setVariable(LDAPUpdateAssertion.PROVIDER_NAME, PROVIDER_NAME);

        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, CREATE_OPERATION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
    }

    @Test
    public void testNullIdentityProviders() throws Exception {
        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        when(identityProviderFactory.findAllIdentityProviders()).thenReturn(null);
        when(identityProvider.getBrowseContext()).thenReturn(dirContext);
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("identityProviderFactory", identityProviderFactory);
            put("managedBackgroundTimer", new MockTimer());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        peCtx.setVariable(LDAPUpdateAssertion.PROVIDER_NAME, PROVIDER_NAME);

        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, CREATE_OPERATION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
    }

    @Test
    public void testFindExceptionMissingOid() throws Exception {
        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        Collection<IdentityProvider> ldapIdentityProviders = new ArrayList<IdentityProvider>();
        ldapIdentityProviders.add(identityProvider);
        when(ldapIdentityProviderConfig.getName()).thenReturn(PROVIDER_NAME);
        when(ldapIdentityProviderConfig.getGoid()).thenReturn(PROVIDER_GOID);
        when(identityProvider.getConfig()).thenReturn(ldapIdentityProviderConfig);
        when(identityProviderFactory.getProvider((Goid)any())).thenReturn(null);
        when(identityProviderFactory.findAllIdentityProviders()).thenReturn(ldapIdentityProviders);
        when(identityProvider.getBrowseContext()).thenReturn(dirContext);
        when(dirContext.createSubcontext(anyString(), any(Attributes.class))).thenThrow(new NamingException("WILL_NOT_PERFORM"));
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("identityProviderFactory", identityProviderFactory);
            put("managedBackgroundTimer", new MockTimer());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        peCtx.setVariable(LDAPUpdateAssertion.PROVIDER_NAME, PROVIDER_NAME);
        peCtx.setVariable(LDAPUpdateAssertion.OPERATION, ServerLDAPUpdateAssertion.MANAGE);

        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, CREATE_OPERATION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, times(1)).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        verify(dirContext).close();
    }


    @Test
    public void testLDAPException() throws Exception {
        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        Collection<IdentityProvider> ldapIdentityProviders = new ArrayList<IdentityProvider>();
        ldapIdentityProviders.add(identityProvider);
        when(ldapIdentityProviderConfig.getName()).thenReturn(PROVIDER_NAME);
        when(ldapIdentityProviderConfig.getGoid()).thenReturn(PROVIDER_GOID);
        when(identityProvider.getConfig()).thenReturn(ldapIdentityProviderConfig);
        when(identityProviderFactory.getProvider((Goid)any())).thenReturn(identityProvider);
        when(identityProviderFactory.findAllIdentityProviders()).thenReturn(ldapIdentityProviders);
        when(identityProvider.getBrowseContext()).thenReturn(dirContext);
        when(dirContext.createSubcontext(anyString(), any(Attributes.class))).thenThrow(new NamingException("WILL_NOT_PERFORM"));
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("identityProviderFactory", identityProviderFactory);
            put("managedBackgroundTimer", new MockTimer());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        peCtx.setVariable(LDAPUpdateAssertion.PROVIDER_NAME, PROVIDER_NAME);
        peCtx.setVariable(LDAPUpdateAssertion.OPERATION, ServerLDAPUpdateAssertion.MANAGE);

        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, CREATE_OPERATION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, times(1)).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        verify(dirContext).close();
    }

    @Test
    public void testInvalidLDAPProvider() throws Exception {
        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        Collection<IdentityProvider> ldapIdentityProviders = new ArrayList<IdentityProvider>();
        ldapIdentityProviders.add(internalIdentityProvider);
        when(ldapIdentityProviderConfig.getName()).thenReturn(PROVIDER_NAME);
        when(ldapIdentityProviderConfig.getGoid()).thenReturn(PROVIDER_GOID);
        when(internalIdentityProvider.getConfig()).thenReturn(ldapIdentityProviderConfig);
        when(identityProviderFactory.getProvider((Goid)any())).thenReturn(internalIdentityProvider);
        when(identityProviderFactory.findAllIdentityProviders()).thenReturn(ldapIdentityProviders);
        when(identityProvider.getBrowseContext()).thenReturn(dirContext);
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("identityProviderFactory", identityProviderFactory);
            put("managedBackgroundTimer", new MockTimer());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        peCtx.setVariable(LDAPUpdateAssertion.PROVIDER_NAME, PROVIDER_NAME);

        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, CREATE_OPERATION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        verify(dirContext, never()).close();
    }

    @Test
    public void testValidateOperation() throws Exception {
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.OPERATION, "validateProvider");
        assertEquals(AssertionStatus.NONE, sass.checkRequest(peCtx));
        assertEquals("<l7:LDAPValidate  xmlns:l7=\"http://ns.l7tech.com/2013/01/ldap-manage\">\n" +
                "<l7:Name>myldap</l7:Name>\n" +
                "<l7:TemplateName>GenericLDAP</l7:TemplateName>\n" +
                "<l7:Type>null</l7:Type>\n" +
                "<l7:TypeVal>0</l7:TypeVal>\n" +
                "</l7:LDAPValidate>", peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_RESOURCE));
        assertEquals(200, peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_STATUS));
        assertEquals("SUCCESS", peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_DETAIL));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        verify(dirContext, never()).close();
    }

    @Test
    public void testClearCacheOperation() throws Exception {
        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.OPERATION, "clearCache");
        assertEquals(AssertionStatus.NONE, sass.checkRequest(peCtx));
        assertEquals(200, peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_STATUS));
        assertEquals("SUCCESS", peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_DETAIL));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        verify(dirContext, never()).close();
    }

    @Test
    public void testValidateOperationFindException() throws Exception {
        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        when(identityProviderFactory.findAllIdentityProviders()).thenThrow(new FindException());
        when(identityProvider.getBrowseContext()).thenReturn(dirContext);
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("identityProviderFactory", identityProviderFactory);
            put("managedBackgroundTimer", new MockTimer());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        peCtx.setVariable(LDAPUpdateAssertion.PROVIDER_NAME, PROVIDER_NAME);

        ServerLDAPUpdateAssertion sass = (ServerLDAPUpdateAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx.setVariable(LDAPUpdateAssertion.OPERATION, "validate");
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
        verify(dirContext, never()).close();
    }

    @Test
    public void testUnmarshallJaxbException() throws Exception {
        when(unmarshaller.unmarshal(anyString(), any(Class.class))).thenThrow(new JAXBException("dummy error"));
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("identityProviderFactory", identityProviderFactory);
            put("managedBackgroundTimer", new MockTimer());
        }}));

        ServerLDAPUpdateAssertion sass = new ServerLDAPUpdateAssertion(assertion, applicationContext, unmarshaller);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, CREATE_OPERATION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        assertEquals(500, peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_STATUS));
        assertEquals("Error unmarshalling to resource:dummy error", peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_DETAIL));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
    }

    @Test
    public void testUnmarshallAnyException() throws Exception {
        when(unmarshaller.unmarshal(anyString(), any(Class.class))).thenThrow(new RuntimeException("general error"));
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("identityProviderFactory", identityProviderFactory);
            put("managedBackgroundTimer", new MockTimer());
        }}));

        ServerLDAPUpdateAssertion sass = new ServerLDAPUpdateAssertion(assertion, applicationContext, unmarshaller);
        peCtx.setVariable(LDAPUpdateAssertion.RESOURCE, CREATE_OPERATION_XML);
        assertEquals(AssertionStatus.FALSIFIED, sass.checkRequest(peCtx));
        assertEquals(500, peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_STATUS));
        assertEquals("Error processing request:general error", peCtx.getVariable(LDAPUpdateAssertion.RESPONSE_DETAIL));
        verify(dirContext, never()).createSubcontext(anyString(), any(Attributes.class));
        verify(dirContext, never()).modifyAttributes(anyString(), any(ModificationItem[].class));
        verify(dirContext, never()).destroySubcontext(anyString());
    }


    private final static String CREATE_OPERATION_XML = "<l7:LDAPOperation xmlns:l7=\"http://ns.l7tech.com/2013/01/ldap-manage\">\n" +
            "\t<l7:operation>CREATE</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t<l7:attributes>\n" +
            "\t\t<l7:attribute>\n" +
            "\t\t\t<l7:name>objectClass</l7:name>\n" +
            "\t\t\t<l7:values>\n" +
            "\t\t\t<l7:value>top</l7:value>\n" +
            "\t\t\t<l7:value>person</l7:value>\n" +
            "\t\t\t<l7:value>inetOrgPerson</l7:value>\n" +
            "\t\t\t<l7:value>organizationalPerson</l7:value>\n" +
            "\t\t\t</l7:values>\n" +
            "\t\t</l7:attribute>\n" +
            "\t\t<l7:attribute>\n" +
            "\t\t\t<l7:name>sn</l7:name>\n" +
            "\t\t\t<l7:value>Richard</l7:value>\n" +
            "\t\t</l7:attribute>\n" +
            "\t\t<l7:attribute>\n" +
            "\t\t\t<l7:name>cn</l7:name>\n" +
            "\t\t\t<l7:value>chard2</l7:value>\n" +
            "\t\t</l7:attribute>\n" +
            "\t\t<l7:attribute>\t\t\t\n" +
            "\t\t\t<l7:name>userPassword</l7:name>\n" +
            "\t\t\t<l7:value>password</l7:value>\n" +
            "\t\t\t<l7:format>unicodePwd</l7:format>\n" +
            "\t\t</l7:attribute>\t\t\n" +
            "\t\t<l7:attribute>\t\t\t\n" +
            "\t\t\t<l7:name>userPassword</l7:name>\n" +
            "\t\t\t<l7:value>password</l7:value>\n" +
            "\t\t\t<l7:format>digest-md5</l7:format>\n" +
            "\t\t</l7:attribute>\t\n" +
            "\t\t<l7:attribute>\t\t\t\n" +
            "\t\t\t<l7:name>userPassword</l7:name>\n" +
            "\t\t\t<l7:value>password</l7:value>\n" +
            "\t\t\t<l7:format>digest-sha1</l7:format>\n" +
            "\t\t</l7:attribute>\t\n" +
            "\t\t<l7:attribute>\t\t\t\n" +
            "\t\t\t<l7:name>userPassword</l7:name>\n" +
            "\t\t\t<l7:value>password</l7:value>\n" +
            "\t\t\t<l7:format>encodeBase64</l7:format>\n" +
            "\t\t</l7:attribute>\t\n" +
            "\t\t<l7:attribute>\t\t\t\n" +
            "\t\t\t<l7:name>userPassword</l7:name>\n" +
            "\t\t\t<l7:value>password</l7:value>\n" +
            "\t\t\t<l7:format>decodeBase64</l7:format>\n" +
            "\t\t</l7:attribute>\t\n" +
            "\t\t<l7:attribute>\t\t\t\n" +
            "\t\t\t<l7:name>userPassword</l7:name>\n" +
            "\t\t\t<l7:value>100d232c3616fd6b22d1610485d025c2e4c34cf0</l7:value>\n" +
            "\t\t\t<l7:format>sha1-base64</l7:format>\n" +
            "\t\t</l7:attribute>\t\n" +
            "\t\t<l7:attribute>\t\t\t\n" +
            "\t\t\t<l7:name>userPassword</l7:name>\n" +
            "\t\t\t<l7:value>password</l7:value>\n" +
            "\t\t\t<l7:format>whatever</l7:format>\n" +
            "\t\t</l7:attribute>\t\n" +
            "\t</l7:attributes>\n" +
            "</l7:LDAPOperation>";

    private final static String UPDATE_OPERATION_XML = "<l7:LDAPOperation xmlns:l7=\"http://ns.l7tech.com/2013/01/ldap-manage\">\n" +
            "\t<l7:operation>UPDATE</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t<l7:attributes>\n" +
            "\t\t<l7:attribute>\n" +
            "\t\t\t<l7:name>objectClass</l7:name>\n" +
            "\t\t\t<l7:values>\n" +
            "\t\t\t<l7:value>top</l7:value>\n" +
            "\t\t\t<l7:value>person</l7:value>\n" +
            "\t\t\t<l7:value>inetOrgPerson</l7:value>\n" +
            "\t\t\t<l7:value>organizationalPerson</l7:value>\n" +
            "\t\t\t</l7:values>\n" +
            "\t\t</l7:attribute>\n" +
            "\t\t<l7:attribute>\n" +
            "\t\t\t<l7:name>sn</l7:name>\n" +
            "\t\t\t<l7:value>Richard</l7:value>\n" +
            "\t\t\t<l7:action>DELETE</l7:action>\n" +
            "\t\t</l7:attribute>\n" +
            "\t\t<l7:attribute>\n" +
            "\t\t\t<l7:name>cn</l7:name>\n" +
            "\t\t\t<l7:value>chard2</l7:value>\n" +
            "\t\t\t<l7:action>ADD</l7:action>\n" +
            "\t\t</l7:attribute>\n" +
            "\t\t<l7:attribute>\t\t\t\n" +
            "\t\t\t<l7:name>userPassword</l7:name>\n" +
            "\t\t\t<l7:value>password</l7:value>\n" +
            "\t\t</l7:attribute>\t\n" +
            "\t</l7:attributes>\n" +
            "</l7:LDAPOperation>";

    private final static String DELETE_OPERATION_XML = "<l7:LDAPOperation xmlns:l7=\"http://ns.l7tech.com/2013/01/ldap-manage\">\n" +
            "\t<l7:operation>DELETE</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "</l7:LDAPOperation>";

    private final static String MULTPLE_OPERATION_XML = "<l7:LDAPOperations xmlns:l7=\"http://ns.l7tech.com/2013/01/ldap-manage\">\n" +
            "\t<l7:LDAPOperation>\n" +
            "\t<l7:operation>DELETE</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t</l7:LDAPOperation>\n" +
            "\t<l7:LDAPOperation>\n" +
            "\t<l7:operation>UPDATE</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t<l7:attributes>\n" +
            "\t\t<l7:attribute>\n" +
            "\t\t\t<l7:name>objectClass</l7:name>\n" +
            "\t\t\t<l7:values>\n" +
            "\t\t\t<l7:value>top</l7:value>\n" +
            "\t\t\t<l7:value>person</l7:value>\n" +
            "\t\t\t<l7:value>inetOrgPerson</l7:value>\n" +
            "\t\t\t<l7:value>organizationalPerson</l7:value>\n" +
            "\t\t\t</l7:values>\n" +
            "\t\t</l7:attribute>\n" +
            "\t</l7:attributes>\n" +
            "\t</l7:LDAPOperation>\n" +
            "\t<l7:LDAPOperation>\n" +
            "\t<l7:operation>CREATE</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t</l7:LDAPOperation>\n" +
            "\t<l7:LDAPOperation>\n" +
            "\t<l7:operation>UPDATE</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t<l7:attributes>\n" +
            "\t\t<l7:attribute>\n" +
            "\t\t\t<l7:name>objectClass</l7:name>\n" +
            "\t\t\t<l7:values>\n" +
            "\t\t\t<l7:value>top</l7:value>\n" +
            "\t\t\t<l7:value>person</l7:value>\n" +
            "\t\t\t<l7:value>inetOrgPerson</l7:value>\n" +
            "\t\t\t<l7:value>organizationalPerson</l7:value>\n" +
            "\t\t\t</l7:values>\n" +
            "\t\t</l7:attribute>\n" +
            "\t</l7:attributes>\n" +
            "\t</l7:LDAPOperation>\n" +
            "\t<l7:LDAPOperation>\n" +
            "\t<l7:operation>DELETE</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t</l7:LDAPOperation>\n" +
            "\t<l7:LDAPOperation>\n" +
            "\t<l7:operation>CREATE</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t</l7:LDAPOperation>\n" +
            "</l7:LDAPOperations>";

    private final static String INVALID_OPERATION_ACTION_XML = "<l7:LDAPOperation xmlns:l7=\"http://ns.l7tech.com/2013/01/ldap-manage\">\n" +
            "\t<l7:operation>BLAH</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "</l7:LDAPOperation>";

    private final static String EMPTY_OPERATION_ACTION_XML = "<l7:LDAPOperation xmlns:l7=\"http://ns.l7tech.com/2013/01/ldap-manage\">\n" +
            "\t<l7:operation></l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "</l7:LDAPOperation>";

    private final static String INVALID_XML = "blah";

    private final static String VALID_XML_BUT_NOT_LDAP_OPERATION = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:AuditAssertion>\n" +
            "            <L7p:SaveRequest booleanValue=\"true\"/>\n" +
            "            <L7p:SaveResponse booleanValue=\"true\"/>\n" +
            "        </L7p:AuditAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n" +
            "";

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = makeMessage(req);
        Message response = makeMessage(res);
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private Message makeMessage(String str) {
        Message message = new Message();
        try {
            message.initialize(XmlUtil.stringAsDocument(str));
        } catch (Exception e) {
        }
        return message;
    }

}