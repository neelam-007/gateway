package com.l7tech.external.assertions.siteminder.server;

import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderHighLevelAgent;
import com.ca.siteminder.SiteMinderLowLevelAgent;
import com.l7tech.external.assertions.siteminder.SiteMinderCheckProtectedAssertion;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 8/26/13
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerSiteMinderCheckProtectedAssertionTest {
    @Mock
    SiteMinderContext mockContext;
    @Mock
    SiteMinderHighLevelAgent mockHla;
    @Mock
    ApplicationContext mockAppCtx;
    @Mock
    SiteMinderConfigurationManager mockSiteMinderConfigurationManager;
    @Mock
    SiteMinderLowLevelAgent mockLla;

    SiteMinderCheckProtectedAssertion assertion;
    ServerSiteMinderCheckProtectedAssertion fixture;
    private Message responseMsg;
    private Message requestMsg;
    private PolicyEnforcementContext pec;

    @BeforeClass
    public static void setUpBeforeClass() {
        System.setProperty("com.l7tech.server.siteminder.enableJavaCompatibilityMode","false");
    }

    @Before
    public void setUp() throws Exception {

        System.setProperty(AbstractServerSiteMinderAssertion.SYSTEM_PROPERTY_SITEMINDER_ENABLED, "true");
        when(mockAppCtx.getBean("siteMinderHighLevelAgent", SiteMinderHighLevelAgent.class)).thenReturn(mockHla);
        when(mockAppCtx.getBean("siteMinderConfigurationManager", SiteMinderConfigurationManager.class)).thenReturn(mockSiteMinderConfigurationManager);
        when(mockHla.checkProtected((String)isNull(), eq("agent"), eq("/protected"), eq("POST"), any(SiteMinderContext.class))).thenReturn(true);
        when(mockHla.checkProtected((String)isNull(), eq("agent"), eq("/unprotected"), eq("POST"), any(SiteMinderContext.class))).thenReturn(false);
        assertion = new SiteMinderCheckProtectedAssertion();
        //Setup Context
        requestMsg = new Message();
        responseMsg = new Message();
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMsg, responseMsg);


    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty(AbstractServerSiteMinderAssertion.SYSTEM_PROPERTY_SITEMINDER_ENABLED);
    }

    @Test
    public void testCheckRequest() throws Exception {
        assertion.setPrefix("siteminder");
        assertion.setProtectedResource("/protected");
        assertion.setAgentId("agent");
        assertion.setAgentGoid(Goid.DEFAULT_GOID);
        assertion.setAction("POST");
        assertion.setSmAgentName("agent");

        fixture = new ServerSiteMinderCheckProtectedAssertion(assertion, mockAppCtx);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
        verify(mockSiteMinderConfigurationManager, times(1)).getSiteMinderLowLevelAgent(Goid.DEFAULT_GOID);
        verify(mockHla, times(1)).checkProtected((String)isNull(), eq("agent"), eq(assertion.getProtectedResource()), eq(assertion.getAction()), any(SiteMinderContext.class));
    }

    @Test
    public void shouldFailWhenResourceNotProtected() throws  Exception {
        assertion.setPrefix("siteminder");
        assertion.setProtectedResource("/unprotected");
        assertion.setAgentId("agent");
        assertion.setAgentGoid(Goid.DEFAULT_GOID);
        assertion.setAction("POST");
        assertion.setSmAgentName("agent");

        fixture = new ServerSiteMinderCheckProtectedAssertion(assertion, mockAppCtx);
        assertTrue(AssertionStatus.FALSIFIED == fixture.checkRequest(pec));
        verify(mockSiteMinderConfigurationManager, times(1)).getSiteMinderLowLevelAgent(Goid.DEFAULT_GOID);
        verify(mockHla, times(1)).checkProtected((String) isNull(), eq("agent"), anyString(), anyString(), any(SiteMinderContext.class));
    }

    @Test
    public void shouldUseExistingSmContext() throws Exception {
        assertion.setPrefix("siteminder");
        assertion.setProtectedResource("/protected");
        assertion.setAgentId("agent");
        assertion.setAgentGoid(Goid.DEFAULT_GOID);
        assertion.setAction("POST");
        assertion.setSmAgentName("agent");

        pec.setVariable("siteminder.smcontext", mockContext);
        when(mockSiteMinderConfigurationManager.getSiteMinderLowLevelAgent(Goid.DEFAULT_GOID)).thenReturn(mockLla);
        fixture = new ServerSiteMinderCheckProtectedAssertion(assertion, mockAppCtx);
        assertTrue(AssertionStatus.NONE == fixture.checkRequest(pec));
        verify(mockSiteMinderConfigurationManager, times(1)).getSiteMinderLowLevelAgent(Goid.DEFAULT_GOID);
        verify(mockHla, times(1)).checkProtected((String)isNull(), eq("agent"), eq(assertion.getProtectedResource()), eq(assertion.getAction()), eq(mockContext));
        verify(mockContext,times(1)).setAgent(mockLla);
    }
}
