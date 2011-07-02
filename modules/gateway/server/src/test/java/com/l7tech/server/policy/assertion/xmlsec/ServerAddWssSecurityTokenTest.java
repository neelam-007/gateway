package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestDocuments;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.TestDefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.credential.ServerXpathCredentialSource;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.xml.xpath.XpathExpression;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link com.l7tech.server.policy.assertion.xmlsec.ServerAddWssSecurityToken}.
 */
public class ServerAddWssSecurityTokenTest {
    public static final String GATHERED_USER = "gatheri";
    public static final String GATHERED_PASS = "sekrit";
    public static final String SPECIFIED_USER = "specificus";
    public static final String SPECIFIED_PASS = "shh";

    private static BeanFactory beanFactory;
    private static DefaultKey defaultKey;

    private PolicyEnforcementContext context;

    @BeforeClass
    public static void setupKeys() throws Exception {
        JceProvider.init();
        defaultKey = new TestDefaultKey();
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("defaultKey", new TestDefaultKey());
            put("securityTokenResolver", new SimpleSecurityTokenResolver(null, new SignerInfo[] { defaultKey.getSslInfo() }));
        }});
    }

    @Before
    public void setupContext() throws Exception {
        final Message request = new Message(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT));
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        contextCreds(GATHERED_USER, GATHERED_PASS);
    }

    @Test
    public void testUsernameTokenGatheredDefault() throws Exception {
        AddWssSecurityToken ass = new AddWssSecurityToken();
        ass.setTarget(TargetMessageType.REQUEST);
        ServerAddWssSecurityToken sass = new ServerAddWssSecurityToken(ass, beanFactory);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        DecorationRequirements dreq = context.getRequest().getSecurityKnob().getOrMakeDecorationRequirements();
        final UsernameToken utok = dreq.getUsernameTokenCredentials();
        assertTrue("Default token type of UsernameToken shall be added to decoration requirements", utok != null);
        assertEquals("Added token shall include the gathered username creds", utok.getUsername(), GATHERED_USER);
        assertNull("Password shall not be included by default", utok.getPassword());
        assertNull("Timestamp inclusion not currently supported when using gathered creds", utok.getCreated());
        assertNull("Nonce inclusion not currently supported when using gathered creds", utok.getNonce());
    }

    @Test
    public void testUsernameTokenGatheredCustom() throws Exception {
        AddWssSecurityToken ass = new AddWssSecurityToken();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setIncludeCreated(false);
        ass.setIncludeNonce(false);
        ass.setIncludePassword(true);
        ServerAddWssSecurityToken sass = new ServerAddWssSecurityToken(ass, beanFactory);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        DecorationRequirements dreq = context.getRequest().getSecurityKnob().getOrMakeDecorationRequirements();
        final UsernameToken utok = dreq.getUsernameTokenCredentials();
        assertTrue("Default token type of UsernameToken shall be added to decoration requirements", utok != null);
        assertEquals("Added token shall include the gathered username creds", utok.getUsername(), GATHERED_USER);
        assertEquals("Password shall be included when so configured", new String(utok.getPassword()), GATHERED_PASS);
        assertNull("Timestamp inclusion not currently supported when using gathered creds", utok.getCreated());
        assertNull("Nonce inclusion not currently supported when using gathered creds", utok.getNonce());
    }

    @Test
    public void testUsernameTokenSpecifiedDefault() throws Exception {
        AddWssSecurityToken ass = new AddWssSecurityToken();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setUseLastGatheredCredentials(false);
        ass.setUsername(SPECIFIED_USER);
        ass.setPassword(SPECIFIED_PASS);
        ServerAddWssSecurityToken sass = new ServerAddWssSecurityToken(ass, beanFactory);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        DecorationRequirements dreq = context.getRequest().getSecurityKnob().getOrMakeDecorationRequirements();
        final UsernameToken utok = dreq.getUsernameTokenCredentials();
        assertTrue("Default token type of UsernameToken shall be added to decoration requirements", utok != null);
        assertEquals("Added token shall include the specified username", utok.getUsername(), SPECIFIED_USER);
        assertNull("Password shall not be included by default", utok.getPassword());
        assertNotNull("Timestamp shall be included by default when using specified creds", utok.getCreated());
        assertNotNull("Nonce shall be included by default when using specified creds", utok.getNonce());
    }

    @Test
    public void testUsernameTokenSpecifiedCustom() throws Exception {
        AddWssSecurityToken ass = new AddWssSecurityToken();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setUseLastGatheredCredentials(false);
        ass.setUsername(SPECIFIED_USER);
        ass.setPassword(SPECIFIED_PASS);
        ass.setIncludeCreated(false);
        ass.setIncludeNonce(false);
        ass.setIncludePassword(true);
        ServerAddWssSecurityToken sass = new ServerAddWssSecurityToken(ass, beanFactory);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        DecorationRequirements dreq = context.getRequest().getSecurityKnob().getOrMakeDecorationRequirements();
        final UsernameToken utok = dreq.getUsernameTokenCredentials();
        assertTrue("Default token type of UsernameToken shall be added to decoration requirements", utok != null);
        assertEquals("Added token shall include the specified username", utok.getUsername(), SPECIFIED_USER);
        assertEquals("Added token shall include the specified password", new String(utok.getPassword()), SPECIFIED_PASS);
        assertTrue("Timestamp shall not be included when turned off", "".equals(utok.getCreated()));
        assertNull("Nonce shall not be included when turned off", utok.getNonce());
    }

    @Test
    @BugNumber(9677)
    public void testUsernameTokenSpecifiedCredentialsWithSignature() throws Exception {
        AddWssSecurityToken ass = new AddWssSecurityToken();
        ass.setTarget(TargetMessageType.REQUEST);
        ass.setUseLastGatheredCredentials(false);
        ass.setUsername(SPECIFIED_USER);
        ass.setProtectTokens(true);
        ServerAddWssSecurityToken sass = new ServerAddWssSecurityToken(ass, beanFactory);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        DecorationRequirements dreq = context.getRequest().getSecurityKnob().getOrMakeDecorationRequirements();
        assertNotNull("Signing certificate shall be configured", dreq.getSenderMessageSigningCertificate());
        assertTrue("Signing certificate shall be the default signer cert", dreq.getSenderMessageSigningCertificate() == defaultKey.getSslInfo().getCertificate());
        assertTrue("Signing private key shall be the default private key", dreq.getSenderMessageSigningPrivateKey() == defaultKey.getSslInfo().getPrivateKey());
    }

    // Set current credentials in context
    private void contextCreds(String user, String pass) throws IOException, PolicyAssertionException {
        XpathCredentialSource xcs = new XpathCredentialSource();
        xcs.setXpathExpression(new XpathExpression("\"" + user + "\""));
        xcs.setPasswordExpression(new XpathExpression("\"" + pass + "\""));
        AssertionStatus result = new ServerXpathCredentialSource(xcs).checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }
}
