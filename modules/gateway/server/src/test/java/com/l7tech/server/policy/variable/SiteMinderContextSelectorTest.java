package com.l7tech.server.policy.variable;

import com.ca.siteminder.SiteMinderContext;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.jndi.ExpectedLookupTemplate;
import org.springframework.test.annotation.ExpectedException;

import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/11/13
 */
@RunWith(MockitoJUnitRunner.class)
public class SiteMinderContextSelectorTest {

    public static final String SSO_TOKEN = "abcdefghijklmnopqrstuvwxyz01234567890==";
    public static final String AGENT_ID = "agent1";
    SiteMinderContext context;
    SiteMinderContextSelector fixture;
    List<SiteMinderContext.AuthenticationScheme> authschemes;

    @Mock
    Syntax.SyntaxErrorHandler mockSyntaxErrorHandler;

    @Before
    public void setUp() throws Exception {
        context = new SiteMinderContext();
        List<Pair<String,Object>> attrs = new ArrayList<>();
        attrs.add(new Pair<String, Object>("ATTR_USERDN", "cn=user,dc=l7tech,dc=com"));
        attrs.add(new Pair<String, Object>("ATTR_USERNAME", "User Name"));
        context.setAttrList(attrs);
        context.setSsoToken(SSO_TOKEN);
        context.setAgentId(AGENT_ID);
        authschemes = new ArrayList<>();
        authschemes.add(SiteMinderContext.AuthenticationScheme.FORM);
        authschemes.add(SiteMinderContext.AuthenticationScheme.X509CERT);
        authschemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
        authschemes.add(SiteMinderContext.AuthenticationScheme.NTCHALLENGE);
        context.setAuthSchemes(authschemes);
        fixture = new SiteMinderContextSelector();

    }

    @Test
    public void shouldReturnAttributeName() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.siteMinderContext", context, "attributes.0.name", mockSyntaxErrorHandler, false);
        assertEquals("ATTR_USERDN", actual.getSelectedValue());
    }

    @Test
    public void shouldReturnAttributeValue() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.siteMinderContext", context, "attributes.1.value", mockSyntaxErrorHandler, false);
        assertEquals("User Name", actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnAttributeLength() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.siteMinderContext", context, "attributes.length", mockSyntaxErrorHandler, false);
        assertEquals("2", actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnSsoToken() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.siteMinderContext", context, "ssoToken", mockSyntaxErrorHandler, false);
        assertEquals(SSO_TOKEN, actual.getSelectedValue());
    }

    @Test
    public void shouldReturnAgentId() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "agent", mockSyntaxErrorHandler, false);
        assertEquals(AGENT_ID, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnAuthenticationSchemeList() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "authschemes", mockSyntaxErrorHandler, false);

        assertEquals(Arrays.asList(new String[] {"FORM", "X509CERT", "BASIC","NTCHALLENGE"}), actual.getSelectedValue());
        assertTrue(actual.getRemainingName() != null);
    }

    @Test
    public void shouldReturnAuthenticationScheme_BASIC() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "authschemes[2]", mockSyntaxErrorHandler, false);
        assertEquals("BASIC", actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotReturnAuthenticationScheme_notFound() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "authschemes[5]", mockSyntaxErrorHandler, false);
        assertEquals(Boolean.FALSE.toString(), actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnAuthenticationSchemeLength() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "authschemes.length", mockSyntaxErrorHandler, false);
        assertEquals("4", actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

}
