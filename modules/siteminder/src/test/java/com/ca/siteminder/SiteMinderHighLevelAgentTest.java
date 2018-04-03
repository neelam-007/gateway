package com.ca.siteminder;

import com.ca.siteminder.util.SiteMinderUtil;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import netegrity.siteminder.javaagent.AgentAPI;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.HashMap;

import static com.ca.siteminder.SiteMinderConfig.SYSTEM_PROP_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SiteMinderHighLevelAgentTest {

    private Config config;
    private SiteMinderLowLevelAgent lowLevelAgent;
    private SiteMinderHighLevelAgent highLevelAgent;
    private SiteMinderContext context;

    @BeforeClass
    public static void setupBeforeClass() {
        ConfigFactory.clearCachedConfig();
        System.setProperty("com.l7tech.server.siteminder.enableJavaCompatibilityMode", "false");
    }

    @Before
    public void setupBefore() {
        config = mock(Config.class);
        lowLevelAgent = mock(SiteMinderLowLevelAgent.class);
        highLevelAgent = new SiteMinderHighLevelAgent(config, new SiteMinderAgentContextCacheManagerImpl());

        context = new SiteMinderContext();
        context.setAgent(lowLevelAgent);
    }

    /**
     * Test Case - Validating Idle Session Timeout
     * @throws SiteMinderApiClassException
     */

    @Test
    public void testSSOTokenValidationWithTimeouts() throws SiteMinderApiClassException {
        final String ssoToken = "ssotoken123";

        buildContextForTimeoutTests(3600, 7200);

        when(config.getProperty(eq(SYSTEM_PROP_PREFIX + SiteMinderConfig.AGENT_AUTHORIZATION_CACHE_SIZE_PROPNAME))).thenReturn("0");
        when(lowLevelAgent.decodeSessionToken(eq(context), anyString())).thenReturn(SiteMinderHighLevelAgent.SUCCESS);
        when(lowLevelAgent.authorize(eq(ssoToken), anyString(), anyString(), eq(context))).thenReturn(AgentAPI.YES);

        // User should be given access if the Idle and Max session timeouts are not expired
        context.getSessionDef().setSessionStartTime(SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000) - 5000);
        context.getSessionDef().setSessionLastTime(SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000) - 1000);
        int result = highLevelAgent.processAuthorizationRequest(null, ssoToken, context, true);
        assertEquals(result, AgentAPI.YES);

        // User should be challenged for credentials when Idle Session time out is expired but not Max Session Tiemout
        context.getSessionDef().setSessionLastTime(SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000) - 4000);
        result = highLevelAgent.processAuthorizationRequest(null, ssoToken, context, true);
        assertEquals(result, SiteMinderHighLevelAgent.CHALLENGE);
    }

    /**
     * Test Case - Avoid validation when IdleTimeOut is Set to 0
     * @throws SiteMinderApiClassException
     */

    @Test
    public void testSSOTokenValidationWithoutTimeouts() throws SiteMinderApiClassException {
        final String ssoToken = "ssotoken123";

        buildContextForTimeoutTests(0, 7200);

        when(config.getProperty(eq(SYSTEM_PROP_PREFIX + SiteMinderConfig.AGENT_AUTHORIZATION_CACHE_SIZE_PROPNAME))).thenReturn("0");
        when(lowLevelAgent.decodeSessionToken(eq(context), anyString())).thenReturn(SiteMinderHighLevelAgent.SUCCESS);
        when(lowLevelAgent.authorize(eq(ssoToken), anyString(), anyString(), eq(context))).thenReturn(AgentAPI.YES);

        // When Idle timeout is set to 0, we should ignore it and consider only Max Session TimeOut
        context.getSessionDef().setSessionStartTime(SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000) - 5000);
        context.getSessionDef().setSessionLastTime(SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000) - 1000);
        int result = highLevelAgent.processAuthorizationRequest(null, ssoToken, context, true);
        assertEquals(result, AgentAPI.YES);
    }


    /**
     * Test Case - Challenging the user when Max Session Time out is expired but not Idle Session Time Out
     * @throws SiteMinderApiClassException
     */

    @Test
    public void testSSOTokenValidationWithExceededIdleTimeout() throws SiteMinderApiClassException {
        final String ssoToken = "ssotoken123";

        buildContextForTimeoutTests(3600, 7200);

        when(config.getProperty(eq(SYSTEM_PROP_PREFIX + SiteMinderConfig.AGENT_AUTHORIZATION_CACHE_SIZE_PROPNAME))).thenReturn("0");
        when(lowLevelAgent.decodeSessionToken(eq(context), anyString())).thenReturn(SiteMinderHighLevelAgent.SUCCESS);
        when(lowLevelAgent.authorize(eq(ssoToken), anyString(), anyString(), eq(context))).thenReturn(AgentAPI.YES);

        // User should get challenged for credentials as max session time is expired, even though Idle Session time is not expired
        context.getSessionDef().setSessionStartTime(SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000) - 7300);
        context.getSessionDef().setSessionLastTime(SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000) - 1500);
        int result = highLevelAgent.processAuthorizationRequest(null, ssoToken, context, true);
        assertEquals(result, AgentAPI.CHALLENGE);
    }


    /**
     * Test Case - No seesion expiration when Max Session Time out and Idle session timeouts are set to 0
     * @throws SiteMinderApiClassException
     */

    @Test
    public void testSSOTokenValidationWithZeroTimeouts() throws SiteMinderApiClassException {
        final String ssoToken = "ssotoken123";

        buildContextForTimeoutTests(0, 0);

        when(config.getProperty(eq(SYSTEM_PROP_PREFIX + SiteMinderConfig.AGENT_AUTHORIZATION_CACHE_SIZE_PROPNAME))).thenReturn("0");
        when(lowLevelAgent.decodeSessionToken(eq(context), anyString())).thenReturn(SiteMinderHighLevelAgent.SUCCESS);
        when(lowLevelAgent.authorize(eq(ssoToken), anyString(), anyString(), eq(context))).thenReturn(AgentAPI.YES);

        // When the timeout are set to 0, we should ignore them if Max Session TimeOut is 0, Siteminder treats it as no Session Timeout
        context.getSessionDef().setSessionStartTime(SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000) - 5000);
        context.getSessionDef().setSessionLastTime(SiteMinderUtil.safeLongToInt(System.currentTimeMillis() / 1000) - 1000);
        int result = highLevelAgent.processAuthorizationRequest(null, ssoToken, context, true);
        assertEquals(result, AgentAPI.YES);
    }


    private void buildContextForTimeoutTests(final int idleTimeout, final int maxTimeout) {
        context.setConfig(new SiteMinderConfiguration());
        context.getConfig().setGoid(Goid.DEFAULT_GOID);
        context.getConfig().setProperties(new HashMap<>());

        context.setResContextDef(new SiteMinderContext.ResourceContextDef());
        context.getResContextDef().setAgent("agent");
        context.getResContextDef().setAction("POST");
        context.getResContextDef().setResource("/protected-resource");

        context.setSessionDef(new SiteMinderContext.SessionDef());
        context.getSessionDef().setId("id1");
        context.getSessionDef().setIdleTimeout(idleTimeout);
        context.getSessionDef().setMaxTimeout(maxTimeout);
    }
}
