package com.l7tech.external.assertions.csrsigner.server;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.csrsigner.CsrSignerAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.cert.BouncyCastleCertUtils;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Test the CsrSignerAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerCsrSignerAssertionTest {
    @Mock
    SsgKeyStoreManager ssgKeyStoreManager;
    @Mock
    DefaultKey defaultKey;
    TestAudit testAudit;
    static SsgKeyEntry caEntry;
    static KeyPair clientKeyPair;
    static byte[] csrBytes;

    CsrSignerAssertion ass;
    ServerCsrSignerAssertion sass;
    PolicyEnforcementContext context;


    @BeforeClass
    public static void setUpStatics() throws Exception {
        Pair<X509Certificate, PrivateKey> ca = TestKeys.getCertAndKey("RSA_1024");
        caEntry = new SsgKeyEntry(77, "my_ca_key", new X509Certificate[] { ca.left }, ca.right );

        Pair<X509Certificate, PrivateKey> client = TestKeys.getCertAndKey("RSA_1536");
        clientKeyPair = new KeyPair(client.left.getPublicKey(), client.right);

        csrBytes = BouncyCastleCertUtils.makeCertificateRequest("joeblow", clientKeyPair, null).getEncoded();
    }

    @Before
    public void setUp() throws Exception {
        // Set up defaults for test -- individual tests can override before calling sass()
        ass = new CsrSignerAssertion();
        ass.setUsesDefaultKeyStore(false);
        ass.setKeyAlias("my_ca_key");
        ass.setNonDefaultKeystoreId(77);
        ass.setCsrVariableName("csr_var");
        ass.setOutputPrefix(null);

        testAudit = new TestAudit();

        System.out.println(HexUtils.encodeBase64(csrBytes));
    }

    @Test
    public void testSuccess_csrVarBytes() throws Exception {
        haveCaKey();

        checkRequest(sass(), context(), AssertionStatus.NONE);

        checkCert(context);
        checkChain(context);
    }

    @Test
    public void testSuccess_csrVarMessage() throws Exception {
        haveCaKey();

        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(csrBytes));
        context().setVariable("csr_var", mess);
        checkRequest(sass(), context, AssertionStatus.NONE);

        checkCert(context);
        checkChain(context);
    }

    private void haveCaKey() throws FindException, KeyStoreException {
        // Configure keystore manager mock to have entry for 77:my_ca_key returning caEntry
        when(ssgKeyStoreManager.lookupKeyByKeyAlias("my_ca_key", 77)).thenReturn(caEntry);
    }

    @Test
    public void testCsrVarInvalidType() throws Exception {
        haveCaKey();

        context().setVariable("csr_var", "some string");
        checkRequest(sass(), context, AssertionStatus.SERVER_ERROR);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);
        assertTrue(testAudit.isAuditPresentContaining("unsupported CSR variable type"));
    }

    @Test
    public void testCsrVarValueMissing() throws Exception {
        haveCaKey();

        context().setVariable("csr_var", null);
        checkRequest(sass(), context, AssertionStatus.SERVER_ERROR);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.NO_SUCH_VARIABLE));
    }

    @Test
    public void testCsrVarNull() throws Exception {
        haveCaKey();
        ass.setCsrVariableName(null);

        checkRequest(sass(), context(), AssertionStatus.SERVER_ERROR);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.ASSERTION_MISCONFIGURED));
        assertTrue(testAudit.isAuditPresentContaining("no CSR variable name configured"));
    }

    @Test
    public void testCsrVarEmpty() throws Exception {
        haveCaKey();
        ass.setCsrVariableName("");

        checkRequest(sass(), context(), AssertionStatus.SERVER_ERROR);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.ASSERTION_MISCONFIGURED));
        assertTrue(testAudit.isAuditPresentContaining("no CSR variable name configured"));
    }

    @Test
    public void testCsrVarWhitespace() throws Exception {
        haveCaKey();
        ass.setCsrVariableName("   ");

        checkRequest(sass(), context(), AssertionStatus.SERVER_ERROR);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.ASSERTION_MISCONFIGURED));
        assertTrue(testAudit.isAuditPresentContaining("no CSR variable name configured"));
    }

    @Test
    public void testPrivateKeyInaccessible() throws Exception {
        when(ssgKeyStoreManager.lookupKeyByKeyAlias("my_ca_key", 77)).thenThrow(new ObjectNotFoundException("no such key"));

        checkRequest(sass(), context(), AssertionStatus.SERVER_ERROR);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);
        assertTrue(testAudit.isAuditPresentContaining("Unable to access CA private key"));
    }

    @Test
    public void testInvalidCsrBytes() throws Exception {
        haveCaKey();

        context().setVariable("csr_var", "some string that doesn't encode to valid CSR bytes".getBytes());
        checkRequest(sass(), context, AssertionStatus.FAILED);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);
        assertTrue(testAudit.isAuditPresentContaining("Failed to generate certificate"));
    }

    @Test
    public void testUnrecoverableKey() throws Exception {
        SsgKeyEntry unrecoverableEntry = new SsgKeyEntry(77, "my_ca_key", caEntry.getCertificateChain(), null);
        when(ssgKeyStoreManager.lookupKeyByKeyAlias("my_ca_key", 77)).thenReturn(unrecoverableEntry);

        checkRequest(sass(), context(), AssertionStatus.SERVER_ERROR);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);
        assertTrue(testAudit.isAuditPresentContaining("Unable to access private key material"));
    }

    @Test
    public void testNoSuchPartException() throws Exception {
        haveCaKey();

        // Create message with main part input stream already consumed
        Message message = new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream("blah".getBytes()));
        message.getMimeKnob().getEntireMessageBodyAsInputStream(true);

        context().setVariable("csr_var", message);
        checkRequest(sass(), context, AssertionStatus.SERVER_ERROR);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.NO_SUCH_PART));
    }

    @Test
    public void testIOException() throws Exception {
        haveCaKey();

        // Create message with main part input stream already consumed
        Message message = new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new IOExceptionThrowingInputStream(new IOException("blah")));

        context().setVariable("csr_var", message);
        checkRequest(sass(), context, AssertionStatus.SERVER_ERROR);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);
        assertTrue(testAudit.isAuditPresentContaining("Unable to read CSR"));
    }

    @Test
    public void testCsrVarEmptyByteArray() throws Exception {
        haveCaKey();

        context().setVariable("csr_var", new byte[0]);
        checkRequest(sass(), context, AssertionStatus.SERVER_ERROR);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);
        assertTrue(testAudit.isAuditPresentContaining("CSR variable was empty"));
    }

    private AssertionStatus checkRequest(ServerCsrSignerAssertion sass, PolicyEnforcementContext context, AssertionStatus expected) throws Exception {
        try {
            AssertionStatus result = sass.checkRequest(context);
            if (expected != null)
                assertEquals(result, expected);
            return result;
        } catch (AssertionStatusException e) {
            return e.getAssertionStatus();
        }
    }

    private PolicyEnforcementContext context() throws InvalidKeyException, SignatureException {
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
        context.setVariable("csr_var", csrBytes);
        return context;
    }

    private ServerCsrSignerAssertion sass() throws PolicyAssertionException {
        sass = new ServerCsrSignerAssertion(ass, testAudit.factory());
        sass.ssgKeyStoreManager = ssgKeyStoreManager;
        sass.defaultKey = defaultKey;
        return sass;
    }

    private X509Certificate checkCert(PolicyEnforcementContext context) throws Exception {
        X509Certificate cert = (X509Certificate) context.getVariable(CsrSignerAssertion.VAR_CERT);
        assertNotNull(cert);
        assertEquals(cert.getSubjectX500Principal().getName(X500Principal.CANONICAL), new X500Principal("cn=joeblow").getName(X500Principal.CANONICAL));
        return cert;
    }

    private void checkChain(PolicyEnforcementContext context) throws Exception {
        X509Certificate cert = (X509Certificate) context.getVariable(CsrSignerAssertion.VAR_CERT);
        X509Certificate[] chain = (X509Certificate[]) context.getVariable(CsrSignerAssertion.VAR_CHAIN);
        assertNotNull(chain);
        assertEquals(2, chain.length);
        assertTrue(CertUtils.certsAreEqual(cert, chain[0]));
        assertTrue(CertUtils.certsAreEqual(caEntry.getCertificate(), chain[1]));
        assertArrayEquals(cert.getPublicKey().getEncoded(), clientKeyPair.getPublic().getEncoded());
    }

    private void assertNoSuchVar(String varName) {
        try {
            context.getVariable(varName);
            fail("expected exception not thrown: NoSuchVariableException for " + varName);
        } catch (NoSuchVariableException e) {
            // Ok
        }
    }
}
