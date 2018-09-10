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
import com.l7tech.objectmodel.Goid;
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
import com.l7tech.test.BugId;
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
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Test the CsrSignerAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerCsrSignerAssertionTest {

    private static final String OVERRIDE_SUBJECT_DN="cn=Firstname LastName, ou=People, o=WorkOrg, c=CAN";

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
        caEntry = new SsgKeyEntry(new Goid(0,77), "my_ca_key", new X509Certificate[] { ca.left }, ca.right );

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
        ass.setNonDefaultKeystoreId(new Goid(0,77));
        ass.setCsrVariableName("csr_var");
        ass.setOutputPrefix(null);

        testAudit = new TestAudit();
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
        when(ssgKeyStoreManager.lookupKeyByKeyAlias("my_ca_key", new Goid(0,77))).thenReturn(caEntry);
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
        when(ssgKeyStoreManager.lookupKeyByKeyAlias("my_ca_key", new Goid(0,77))).thenThrow(new ObjectNotFoundException("no such key"));

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
        SsgKeyEntry unrecoverableEntry = new SsgKeyEntry(new Goid(0,77), "my_ca_key", caEntry.getCertificateChain(), null);
        when(ssgKeyStoreManager.lookupKeyByKeyAlias("my_ca_key", new Goid(0,77))).thenReturn(unrecoverableEntry);

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


    // This will test the default behaviour when the Expiry Age field and DN Override fields are empty.
    // The default expiry age is 5(years)*365=1825 days as hardcoded in the gateway code will be used
    // to calculate the expiry date.
    @Test
    @BugId("DE337487")
    public void testSuccess_csrValidSigningWithEmptyExpiryAgeAndNoDnOverride() throws Exception {

        haveCaKey();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(csrBytes));

        context.setVariable("csr_var", mess);
        ass.setExpiryAgeDays(null); //

        checkRequest(sass(), context, AssertionStatus.NONE);

        X509Certificate cert = (X509Certificate) context.getVariable(CsrSignerAssertion.VAR_CERT);
        assertNotNull(cert);
        assertEquals(cert.getSubjectX500Principal().getName(X500Principal.CANONICAL), new X500Principal("cn=joeblow").getName(X500Principal.CANONICAL));

        validateCertificateExpiryYearAndExpiryDaysOfYear(cert, CsrSignerAssertion.DEFAULT_EXPIRY_AGE_DAYS_NO_DN_OVERRIDE);
    }


    // This will test the default behaviour when the Expiry Age field is empty but a DN Override value is specified.
    // The default expiry age of 365 days is used to calculate the expiry date.
    @Test
    @BugId("DE337487")
    public void testSuccess_csrValidSigningWithEmptyExpiryAgeButDnOverride() throws Exception {

        haveCaKey();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(csrBytes));

        context.setVariable("csr_var", mess);
        context.setVariable("csr_override_dn_var", OVERRIDE_SUBJECT_DN);
        ass.setCertDNVariableName("csr_override_dn_var");

        ass.setExpiryAgeDays("");

        checkRequest(sass(), context, AssertionStatus.NONE);

        X509Certificate cert = (X509Certificate) context.getVariable(CsrSignerAssertion.VAR_CERT);
        assertNotNull(cert);
        assertEquals(cert.getSubjectX500Principal().getName(X500Principal.CANONICAL), new X500Principal(OVERRIDE_SUBJECT_DN).getName(X500Principal.CANONICAL));

        validateCertificateExpiryYearAndExpiryDaysOfYear(cert, CsrSignerAssertion.DEFAULT_EXPIRY_AGE_DAYS_DN_OVERRIDE);
    }


    // This will test when the Expiry Age field is set and the DN Override field is empty.
    // The expiry age specified will be used to calculate the expiry date.
    @Test
    @BugId("DE337487")
    public void testSuccess_csrValidSigningWithSpecifiedExpiryAgeNoDnOverride() throws Exception {

        final String EXPIRY_AGE_DAYS = "20";

        haveCaKey();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(csrBytes));
        context.setVariable("csr_var", mess);
        ass.setExpiryAgeDays(EXPIRY_AGE_DAYS);

        checkRequest(sass(), context, AssertionStatus.NONE);

        X509Certificate cert = (X509Certificate) context.getVariable(CsrSignerAssertion.VAR_CERT);
        assertNotNull(cert);
        assertEquals(cert.getSubjectX500Principal().getName(X500Principal.CANONICAL), new X500Principal("cn=joeblow").getName(X500Principal.CANONICAL));

        validateCertificateExpiryYearAndExpiryDaysOfYear(cert,Integer.parseInt(EXPIRY_AGE_DAYS));
    }


    // This will test when the Expiry Age field and the DN Override field is set. Enter a numeric value to the Expiry Age field.
    // The expiry age specified will be used to calculate the expiry date.
    @Test
    @BugId("DE337487")
    public void testSuccess_csrValidSigningWithSpecifiedExpiryAgeAndDnOverride() throws Exception {

        haveCaKey();
        final String EXPIRY_AGE_DAYS = "22";

        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(csrBytes));

        context.setVariable("csr_override_dn_var", OVERRIDE_SUBJECT_DN);
        context.setVariable("csr_var", mess);
        ass.setCertDNVariableName("csr_override_dn_var");
        ass.setExpiryAgeDays(EXPIRY_AGE_DAYS);

        checkRequest(sass(), context, AssertionStatus.NONE);

        X509Certificate cert = (X509Certificate) context.getVariable(CsrSignerAssertion.VAR_CERT);
        assertNotNull(cert);
        assertEquals(cert.getSubjectX500Principal().getName(X500Principal.CANONICAL), new X500Principal(OVERRIDE_SUBJECT_DN).getName(X500Principal.CANONICAL));

        validateCertificateExpiryYearAndExpiryDaysOfYear(cert,Integer.parseInt(EXPIRY_AGE_DAYS));
    }


    // This will test when the Expiry Age field and the DN Override field is set. Enter a context variable for the Expiry Age field.
    // The expiry age specified will be used to calculate the expiry date.
    @Test
    @BugId("DE337487")
    public void testSuccess_csrValidSigningWithSpecifiedExpiryAgeUsingContextVarAndDnOverride() throws Exception {

        haveCaKey();
        final String EXPIRY_AGE_DAYS = "22";

        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(csrBytes));

        context.setVariable("csr_override_dn_var", OVERRIDE_SUBJECT_DN);
        context.setVariable("csr_var", mess);
        context.setVariable("expiry_age", EXPIRY_AGE_DAYS);
        ass.setCertDNVariableName("csr_override_dn_var");
        ass.setExpiryAgeDays("${expiry_age}");

        checkRequest(sass(), context, AssertionStatus.NONE);

        X509Certificate cert = (X509Certificate) context.getVariable(CsrSignerAssertion.VAR_CERT);
        assertNotNull(cert);
        assertEquals(cert.getSubjectX500Principal().getName(X500Principal.CANONICAL), new X500Principal(OVERRIDE_SUBJECT_DN).getName(X500Principal.CANONICAL));

        validateCertificateExpiryYearAndExpiryDaysOfYear(cert,Integer.parseInt(EXPIRY_AGE_DAYS));
    }


    // This will test when the Expiry age specified exceeds the maximum value. The signing of the CSR should fail.
    @Test
    @BugId("DE337487")
    public void test_csrInvalidSigningWithExpiryAgeExceedingMaxAndDnOverride() throws Exception {

        haveCaKey();
        final String EXPIRY_AGE_DAYS = String.valueOf(CsrSignerAssertion.MAX_CSR_AGE + 1);

        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(csrBytes));

        context.setVariable("csr_override_dn_var", OVERRIDE_SUBJECT_DN);
        context.setVariable("csr_var", mess);
        context.setVariable("expiry_age", EXPIRY_AGE_DAYS);
        ass.setCertDNVariableName("csr_override_dn_var");
        ass.setExpiryAgeDays("${expiry_age}");

        checkRequest(sass(), context, AssertionStatus.FAILED);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);

        assertTrue(testAudit.isAuditPresentContaining(CsrSignerAssertion.ERR_EXPIRY_AGE_MUST_BE_IN_RANGE));
    }


    // This will test when the Expiry age specified is less than the minimum value. The signing of the CSR should fail.
    @Test
    @BugId("DE337487")
    public void test_csrInvalidSigningWithExpiryAgeUnderMinimumAndDnOverride() throws Exception {

        haveCaKey();
        final String EXPIRY_AGE_DAYS = String.valueOf(CsrSignerAssertion.MAX_CSR_AGE + 1);

        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
        Message mess = new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream(csrBytes));

        context.setVariable("csr_override_dn_var", OVERRIDE_SUBJECT_DN);
        context.setVariable("csr_var", mess);
        context.setVariable("expiry_age", EXPIRY_AGE_DAYS);
        ass.setCertDNVariableName("csr_override_dn_var");
        ass.setExpiryAgeDays("${expiry_age}");

        checkRequest(sass(), context, AssertionStatus.FAILED);

        assertNoSuchVar(CsrSignerAssertion.VAR_CERT);
        assertNoSuchVar(CsrSignerAssertion.VAR_CHAIN);

        assertTrue(testAudit.isAuditPresentContaining(CsrSignerAssertion.ERR_EXPIRY_AGE_MUST_BE_IN_RANGE));
    }


    // This is a helper method to validate the expiry date of a given cert.  The year and the days of year
    // of the expiry date are compared with the expected expiry date computed from the given the targetExpiryAgeDays.
    private void validateCertificateExpiryYearAndExpiryDaysOfYear(X509Certificate cert, int targetExpiryAgeDays) {

        // Get the current date and add the target expiry age in days.
        LocalDate currDt = LocalDate.now();
        LocalDate targetDt = currDt.plusDays(targetExpiryAgeDays);

        // Get the NotAfter (the last day the cert is valid) from the certificate
        Date signedValidityNotAfterDate = cert.getNotAfter();
        Calendar certCalDate = Calendar.getInstance();
        certCalDate.setTime(signedValidityNotAfterDate);

        // Verify the signed certificate's expiry year and expiry day of year are valid.
        assertTrue(certCalDate.get(Calendar.YEAR) == targetDt.getYear());
        assertTrue(certCalDate.get(Calendar.DAY_OF_YEAR) == targetDt.getDayOfYear());
    }

}