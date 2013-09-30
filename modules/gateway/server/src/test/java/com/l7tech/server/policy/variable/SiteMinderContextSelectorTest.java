package com.l7tech.server.policy.variable;

import com.ca.siteminder.SiteMinderContext;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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
    public static final String TRANSACTION_ID = "12345656790transactionid";
    public static final String FORM_FCC = "/path/form.fcc";
    public static final String REALMDEF_NAME = "name";
    public static final String REALMDEF_OID = "oid";
    public static final String REALMDEF_DOMAIN_OID = "domoid";
    public static final String RESDEF_AGENT = "layer7-agent";
    public static final String RESDEF_SERVER = "server_name";
    public static final String RESDEF_RESOURCE = "/resource";
    public static final String RESDEF_ACTION = "POST";
    SiteMinderContext context;
    SiteMinderContextSelector fixture;
    List<SiteMinderContext.AuthenticationScheme> authschemes;

    @Mock
    Syntax.SyntaxErrorHandler mockSyntaxErrorHandler;
    private static final int SESSDEF_REASON = 1;
    private static final int SESSDEF_IDLETIMEOUT = 60;
    private static final int SESSDEF_MAXTIMEOUT = 7200;
    private static final int SESSDEF_CURRENTTIME = 1234546;
    private static final int SESSDEF_STARTTIME = 234566;
    private static final int SESSDEF_LASTTIME = 123454;
    private static final String SESSDEF_ID = "abcdefgh";
    private static final String SESSDEF_SPEC = "qwertyuioplkjhgfdsa][xcvbnm,./==";


    @Before
    public void setUp() throws Exception {
        context = new SiteMinderContext();
        List<Pair<String,Object>> attrs = new ArrayList<>();
        attrs.add(new Pair<String, Object>("ATTR_USERDN", "cn=user,dc=l7tech,dc=com"));
        attrs.add(new Pair<String, Object>("ATTR_USERNAME", "User Name"));
        context.setAttrList(attrs);
        context.setSsoToken(SSO_TOKEN);
        context.setTransactionId(TRANSACTION_ID);
        authschemes = new ArrayList<>();
        authschemes.add(SiteMinderContext.AuthenticationScheme.FORM);
        authschemes.add(SiteMinderContext.AuthenticationScheme.X509CERT);
        authschemes.add(SiteMinderContext.AuthenticationScheme.BASIC);
        authschemes.add(SiteMinderContext.AuthenticationScheme.NTCHALLENGE);
        context.setAuthSchemes(authschemes);
        SiteMinderContext.RealmDef realmDef = new SiteMinderContext.RealmDef(REALMDEF_NAME, REALMDEF_OID, REALMDEF_DOMAIN_OID,111, FORM_FCC);
        context.setRealmDef(realmDef);
        SiteMinderContext.ResourceContextDef resContextDef = new SiteMinderContext.ResourceContextDef(RESDEF_AGENT, RESDEF_SERVER, RESDEF_RESOURCE, RESDEF_ACTION);
        context.setResContextDef(resContextDef);
        SiteMinderContext.SessionDef sessionDef = new SiteMinderContext.SessionDef(SESSDEF_REASON, SESSDEF_IDLETIMEOUT, SESSDEF_MAXTIMEOUT, SESSDEF_CURRENTTIME, SESSDEF_STARTTIME, SESSDEF_LASTTIME, SESSDEF_ID, SESSDEF_SPEC);
        context.setSessionDef(sessionDef);
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

    @Test
    public void shouldReturnTransactionId() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "transactionid", mockSyntaxErrorHandler, false);
        assertEquals(TRANSACTION_ID, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnFormLocation() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "realmDef.formLocation", mockSyntaxErrorHandler, false);
        assertEquals(FORM_FCC, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnRealmDefName() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "realmDef.name", mockSyntaxErrorHandler, false);
        assertEquals(REALMDEF_NAME, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnRealmDefOid() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "realmDef.oid", mockSyntaxErrorHandler, false);
        assertEquals(REALMDEF_OID, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnRealmDefDomainOid() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "realmDef.domoid", mockSyntaxErrorHandler, false);
        assertEquals(REALMDEF_DOMAIN_OID, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnRealmCredentials() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "realmDef.credentials", mockSyntaxErrorHandler, false);
        assertEquals(Integer.toString(111), actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnResAgent() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "resourceDef.agent", mockSyntaxErrorHandler, false);
        assertEquals(RESDEF_AGENT, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnResAction() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "resourceDef.action", mockSyntaxErrorHandler, false);
        assertEquals(RESDEF_ACTION, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnResResource() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "resourceDef.resource", mockSyntaxErrorHandler, false);
        assertEquals(RESDEF_RESOURCE, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnResServer() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "resourceDef.server", mockSyntaxErrorHandler, false);
        assertEquals(RESDEF_SERVER, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnSessID() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "sessDef.id", mockSyntaxErrorHandler, false);
        assertEquals(SESSDEF_ID, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnSessSpec() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "sessDef.spec", mockSyntaxErrorHandler, false);
        assertEquals(SESSDEF_SPEC, actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnSessCurrentTime() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "sessDef.currenttime", mockSyntaxErrorHandler, false);
        assertEquals(Integer.toString(SESSDEF_CURRENTTIME), actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnSessStartTime() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "sessDef.starttime", mockSyntaxErrorHandler, false);
        assertEquals(Integer.toString(SESSDEF_STARTTIME), actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnSessLastTime() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "sessDef.lasttime", mockSyntaxErrorHandler, false);
        assertEquals(Integer.toString(SESSDEF_LASTTIME), actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnSessMaxTimeout() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "sessDef.maxtimeout", mockSyntaxErrorHandler, false);
        assertEquals(Integer.toString(SESSDEF_MAXTIMEOUT), actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnSessIdleTimeout() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "sessDef.idletimeout", mockSyntaxErrorHandler, false);
        assertEquals(Integer.toString(SESSDEF_IDLETIMEOUT), actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }

    @Test
    public void shouldReturnSessReason() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("siteminder.smcontext", context, "sessDef.reason", mockSyntaxErrorHandler, false);
        assertEquals(Integer.toString(SESSDEF_REASON), actual.getSelectedValue());
        assertTrue(actual.getRemainingName() == null);
    }
}
