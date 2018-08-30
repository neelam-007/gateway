package com.ca.siteminder;

import com.ca.siteminder.util.MockAgentAPIBuilder;
import com.ca.siteminder.util.MockAgentAPITestConstants;
import com.ca.siteminder.util.SiteMinderTestUtils;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.MockConfig;
import org.junit.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/21/13
 */
public class SiteMinderAgentTest {

    private SiteMinderLowLevelAgent lowAgent;
    private SiteMinderHighLevelAgent highAgent;
    private SiteMinderAgentContextCacheManager cacheManager;
    private MockAgentAPIBuilder mockAgentAPIBuilder;
    private SiteMinderContext context;
    private Map<String, String> ssgProperties = new HashMap<>();

    @BeforeClass
    public static void setupBeforeClass() {
        ConfigFactory.clearCachedConfig();
        System.setProperty("com.l7tech.server.siteminder.enableJavaCompatibilityMode", "false");
    }

    @Before
    public void setUp() throws Exception {
        mockAgentAPIBuilder = MockAgentAPIBuilder.getDefaultBuilder().tokenAuth();
        lowAgent = new SiteMinderLowLevelAgent(SiteMinderTestUtils.getSiteMinderConfig(), mockAgentAPIBuilder.build());
        cacheManager = new SiteMinderAgentContextCacheManagerImpl();
        highAgent = new SiteMinderHighLevelAgent(new MockConfig(ssgProperties), cacheManager);
        context = new SiteMinderContext();
        context.setAgent(lowAgent);
        context.setConfig(SiteMinderTestUtils.getSiteMinderConfiguration());
        context.setTransactionId(UUID.randomUUID().toString());
    }

    @After
    public void tearDown() throws Exception {
       lowAgent.unInitialize();
    }

    @Test
    public void testSiteMinderLowLevelAgent() throws Exception {
        assertTrue(lowAgent.isInitialized());
    }

    @Test
    public void testCheckProtectedWithPrivateResource() throws Exception {
        assertTrue(highAgent.checkProtected(
                MockAgentAPITestConstants.USER_IP,
                MockAgentAPITestConstants.RESDEF_AGENT,
                MockAgentAPITestConstants.RESDEF_SERVER,
                MockAgentAPITestConstants.RESDEF_PRIVATE_RESOURCE,
                MockAgentAPITestConstants.RESDEF_ACTION,
                context));
        assertEquals(MockAgentAPITestConstants.RESDEF_AGENT, context.getResContextDef().getAgent());
        assertEquals(MockAgentAPITestConstants.RESDEF_PRIVATE_RESOURCE, context.getResContextDef().getResource());
        assertEquals(MockAgentAPITestConstants.RESDEF_ACTION, context.getResContextDef().getAction());
        assertEquals(MockAgentAPITestConstants.RESDEF_SERVER, context.getResContextDef().getServer());
    }

    @Test
    public void testCheckProtectedWithPublicResource() throws Exception {
        assertFalse(highAgent.checkProtected(
                MockAgentAPITestConstants.USER_IP,
                MockAgentAPITestConstants.RESDEF_AGENT,
                MockAgentAPITestConstants.RESDEF_SERVER,
                MockAgentAPITestConstants.RESDEF_PUBLIC_RESOURCE,
                MockAgentAPITestConstants.RESDEF_ACTION,
                context));
    }

    @Test
    public void testAuthenticateUsingBasicAuthAndSsoToken() throws Exception {
        highAgent.checkProtected(
                MockAgentAPITestConstants.USER_IP,
                MockAgentAPITestConstants.RESDEF_AGENT,
                MockAgentAPITestConstants.RESDEF_SERVER,
                MockAgentAPITestConstants.RESDEF_RESOURCE,
                MockAgentAPITestConstants.RESDEF_ACTION,
                context);

        // Try with Basic Auth details
        final SiteMinderCredentials credentials = new SiteMinderCredentials();
        credentials.addUsernamePasswordCredentials(MockAgentAPITestConstants.AUTHN_USER_NAME, MockAgentAPITestConstants.AUTHN_PASSWORD);
        assertEquals(SiteMinderHighLevelAgent.YES,
                highAgent.processAuthenticationRequest(credentials, MockAgentAPITestConstants.USER_IP, null, context, true));
        assertTrue(context.getSsoToken().startsWith(MockAgentAPITestConstants.SSO_TOKEN));
        assertTrue(context.getSsoToken().contains(context.getTransactionId()));

        // Try with existing SSO token
        assertEquals(SiteMinderHighLevelAgent.YES,
                highAgent.processAuthenticationRequest(credentials, MockAgentAPITestConstants.USER_IP, context.getSsoToken(), context, false));
    }

    @Test
    public void testUsingAco() throws Exception {
        final String[] acoParams = new String[] {
          "SSOZoneName=SZ1",
          "CookieDomain=.example.com"
        };

        // Initialize context with ACO name
        lowAgent = new SiteMinderLowLevelAgent(SiteMinderTestUtils.getSiteMinderConfig(), mockAgentAPIBuilder.withAco(acoParams).build());
        context.setAgent(lowAgent);
        context.setAcoName(MockAgentAPITestConstants.ACO_NAME);

        // Check resource and ACO parameters
        highAgent.checkProtected(
                MockAgentAPITestConstants.USER_IP,
                MockAgentAPITestConstants.RESDEF_AGENT,
                MockAgentAPITestConstants.RESDEF_SERVER,
                MockAgentAPITestConstants.RESDEF_RESOURCE,
                MockAgentAPITestConstants.RESDEF_ACTION,
                context);

        final SiteMinderContext.Attribute zoneAttr = findAttributeByName(context.getAcoAttrList(), SiteMinderAgentConstants.ATTR_ACO_SSOZONE_NAME);
        assertNotNull(zoneAttr);
        assertTrue(acoParams[0].endsWith("=" + zoneAttr.getValueAsString()));

        final SiteMinderContext.Attribute cookieDomainAttr = findAttributeByName(context.getAcoAttrList(), SiteMinderAgentConstants.ATTR_ACO_COOKIE_DOMAIN);
        assertNotNull(cookieDomainAttr);
        assertTrue(acoParams[1].endsWith("=" + cookieDomainAttr.getValueAsString()));

        // Try with Basic Auth details and enable Cookie String generation
        final SiteMinderCredentials credentials = new SiteMinderCredentials();
        credentials.addUsernamePasswordCredentials(MockAgentAPITestConstants.AUTHN_USER_NAME, MockAgentAPITestConstants.AUTHN_PASSWORD);
        ssgProperties.put(SiteMinderConfig.GENERATE_SESSION_COOKIE_STRING_PROPERTY, "true");

        assertEquals(SiteMinderHighLevelAgent.YES,
                highAgent.processAuthenticationRequest(credentials, MockAgentAPITestConstants.USER_IP, null, context, true));
        assertTrue(context.getSsoToken().startsWith(MockAgentAPITestConstants.SSO_TOKEN));
        assertTrue(context.getSsoToken().contains(context.getTransactionId()));

        final SiteMinderContext.Attribute sessionCookieStringAttr = findAttributeByName(context.getAttrList(), SiteMinderAgentConstants.ATTR_SESSION_COOKIE_STRING);
        assertNotNull(sessionCookieStringAttr);

        final String sessionCookieString = sessionCookieStringAttr.getValueAsString();
        assertTrue(sessionCookieString.startsWith("SZ1SESSION=" + context.getSsoToken()));
        assertTrue(sessionCookieString.contains("Domain=.example.com"));

        assertTrue(cacheManager.
                getCache(Goid.DEFAULT_GOID, MockAgentAPITestConstants.RESDEF_AGENT).
                getSubCache(SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_ACO).
                getCache().size() > 0);

        // Try with existing SSO token
        assertEquals(SiteMinderHighLevelAgent.YES,
                highAgent.processAuthenticationRequest(credentials, MockAgentAPITestConstants.USER_IP, context.getSsoToken(), context, false));
    }

    private SiteMinderContext.Attribute findAttributeByName(final List<SiteMinderContext.Attribute> attributes, final String name) {
        for (SiteMinderContext.Attribute attr : attributes) {
            if (attr.getName().equals(name)) {
                return attr;
            }
        }

        return null;
    }

}
