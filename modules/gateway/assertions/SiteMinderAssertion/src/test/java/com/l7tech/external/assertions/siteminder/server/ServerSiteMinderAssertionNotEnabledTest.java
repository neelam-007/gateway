package com.l7tech.external.assertions.siteminder.server;

import com.l7tech.external.assertions.siteminder.SiteMinderAuthenticateAssertion;
import com.l7tech.external.assertions.siteminder.SiteMinderAuthorizeAssertion;
import com.l7tech.external.assertions.siteminder.SiteMinderCheckProtectedAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
import com.l7tech.server.siteminder.SiteMinderConfigurationManagerStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.when;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 8/26/13
 */
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class ServerSiteMinderAssertionNotEnabledTest {

    @Mock
    ApplicationContext mockAppCtx;

    @Before
    public void setUp() throws Exception {
       System.setProperty(AbstractServerSiteMinderAssertion.SYSTEM_PROPERTY_SITEMINDER_ENABLED, "false");
        when(mockAppCtx.getBean("siteMinderConfigurationManager", SiteMinderConfigurationManager.class)).thenReturn(new SiteMinderConfigurationManagerStub());
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty(AbstractServerSiteMinderAssertion.SYSTEM_PROPERTY_SITEMINDER_ENABLED);

    }

    @Test(expected = PolicyAssertionException.class)
    public void smAuthenticateAssertionShouldThrowExceptionWhenSmSdkNotEnabled() throws Exception {
        ServerSiteMinderAuthenticateAssertion serverSiteMinderAuthenticateAssertion = new ServerSiteMinderAuthenticateAssertion(new SiteMinderAuthenticateAssertion(), mockAppCtx);
    }

    @Test(expected = PolicyAssertionException.class)
    public void smCheckProtectedShouldThrowExceptionWhenSmSdkNotEnabled() throws Exception {
        ServerSiteMinderCheckProtectedAssertion serverSiteMinderCheckProtectedAssertion = new ServerSiteMinderCheckProtectedAssertion(new SiteMinderCheckProtectedAssertion(), mockAppCtx);
    }

    @Test(expected = PolicyAssertionException.class)
    public void smAuthorizeAssertionShouldThrowExceptionWhenSmSdkNotEnabled() throws Exception {
        ServerSiteMinderAuthorizeAssertion serverSiteMinderAuthorizeAssertion = new ServerSiteMinderAuthorizeAssertion(new SiteMinderAuthorizeAssertion(), mockAppCtx);
    }
}
