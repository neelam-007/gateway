package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestDocuments;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.identity.cert.TrustedCertCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerLookupTrustedCertificateAssertionJavaTest {
    private static final String CERTIFICATE_NAME_ALICE = "alice";

    @Mock
    private TrustedCertCache cache;

    @Mock
    private SecurityTokenResolver securityTokenResolver;

    private X509Certificate aliceCert;
    private TrustedCert trustedCert;
    private TestAudit audit;
    private LookupTrustedCertificateAssertion assertion;
    private ServerLookupTrustedCertificateAssertion serverAssertion;
    private PolicyEnforcementContext pec;

    @Before
    public void setUp() throws Exception {
        aliceCert = TestDocuments.getWssInteropAliceCert();

        trustedCert = new TrustedCert();
        trustedCert.setCertificate(aliceCert);

        audit = new TestAudit();

        assertion = new LookupTrustedCertificateAssertion();
        assertion.setTrustedCertificateName(CERTIFICATE_NAME_ALICE);
        assertion.setAllowMultipleCertificates(false);

        serverAssertion = new ServerLookupTrustedCertificateAssertion(assertion);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                        .put("auditFactory", audit.factory())
                        .put("trustedCertCache", cache)
                        .put("securityTokenResolver", securityTokenResolver)
                        .unmodifiableMap()
        );

        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    /**
     * fail and audit if certificate not found
     */
    @Test
    public void testCheckRequest_NoCertificateForName_LookupFailureAuditedAndAssertionFalsified() throws Exception {
        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(pec));

        assertEquals(2, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_NAME));
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_NOTFOUND));
    }

    /**
     * succeed with an audit and set a single valued variable when certificate found
     */
    @Test
    public void testCheckRequest_CertificatePresentForName_CertificateSetToVariable() throws Exception {
        when(cache.findByName(CERTIFICATE_NAME_ALICE)).thenReturn(Collections.singleton(trustedCert));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_NAME));
        assertEquals(aliceCert, pec.getVariable("certificates"));
    }

    /**
     * succeed with an audit and set a multivalued variable when multiple certificates found
     */
    @Test
    public void testCheckRequest_MultipleCertificatesPresentForName_CertificateSetToMultivaluedVariable() throws Exception {
        assertion.setAllowMultipleCertificates(true);

        TrustedCert trustedCertBob = new TrustedCert();
        trustedCertBob.setCertificate(TestDocuments.getWssInteropBobCert());

        when(cache.findByName(CERTIFICATE_NAME_ALICE)).thenReturn(CollectionUtils.set(trustedCert, trustedCertBob));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_NAME));

        X509Certificate[] found = (X509Certificate[]) pec.getVariable("certificates");

        assertEquals(2, found.length);
        assertEquals(aliceCert, found[0]);
        assertEquals(trustedCertBob.getCertificate(), found[1]);
    }

    /**
     * succeed with an audit and set a single valued variable when thumbprintSha1 found
     */
    @Test
    public void testCheckRequest_CertificatePresentForThumbprint_CertificateSetToVariable() throws Exception {
        String thumbprint = "sha1blah";
        assertion.setLookupType(LookupTrustedCertificateAssertion.LookupType.CERT_THUMBPRINT_SHA1);
        assertion.setCertThumbprintSha1(thumbprint);

        when(securityTokenResolver.lookup(thumbprint)).thenReturn(aliceCert);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_NAME));
        assertEquals(aliceCert, pec.getVariable("certificates"));

        verify(securityTokenResolver).lookup(thumbprint);
    }

    /**
     * succeed with an audit and set a single valued variable when SKI found
     */
    @Test
    public void testCheckRequest_CertificatePresentForSKI_CertificateSetToVariable() throws Exception {
        String ski = "skiblah";
        assertion.setLookupType(LookupTrustedCertificateAssertion.LookupType.CERT_SKI);
        assertion.setCertSubjectKeyIdentifier(ski);

        when(securityTokenResolver.lookupBySki(ski)).thenReturn(aliceCert);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_NAME));
        assertEquals(aliceCert, pec.getVariable("certificates"));

        verify(securityTokenResolver).lookupBySki(ski);
    }

    /**
     * succeed with an audit and set a single valued variable when Subject DN found
     */
    @Test
    public void testCheckRequest_CertificatePresentForSubjectDN_CertificateSetToVariable() throws Exception {
        String subjectDn = "cn=blah";
        assertion.setLookupType(LookupTrustedCertificateAssertion.LookupType.CERT_SUBJECT_DN);
        assertion.setCertSubjectDn(subjectDn);

        when(securityTokenResolver.lookupByKeyName(subjectDn)).thenReturn(aliceCert);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_NAME));
        assertEquals(aliceCert, pec.getVariable("certificates"));

        verify(securityTokenResolver).lookupByKeyName(subjectDn);
    }

    /**
     * succeed with an audit and set a single valued variable when Issuer/Serial found
     */
    @Test
    public void testCheckRequest_CertificatePresentForIssuerAndSerial_CertificateSetToVariable() throws Exception {
        String issuerDn = "cn=issuerblah";
        String serialNumber = "8473";
        assertion.setLookupType(LookupTrustedCertificateAssertion.LookupType.CERT_ISSUER_SERIAL);
        assertion.setCertIssuerDn(issuerDn);
        assertion.setCertSerialNumber(serialNumber);

        when(securityTokenResolver.lookupByIssuerAndSerial(new X500Principal(issuerDn), new BigInteger(serialNumber))).thenReturn(aliceCert);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_NAME));
        assertEquals(aliceCert, pec.getVariable("certificates"));

        verify(securityTokenResolver).lookupByIssuerAndSerial(new X500Principal(issuerDn), new BigInteger(serialNumber));
    }

    /**
     * fail with an audit if multiple certificates found and not permitted
     */
    @Test
    public void testCheckRequest_MultipleCertificatesPresentButNotAllowed_LookupMultipleAuditedAndAssertionFalsified() throws Exception {
        TrustedCert trustedCertBob = new TrustedCert();
        trustedCertBob.setCertificate(TestDocuments.getWssInteropBobCert());

        when(cache.findByName(CERTIFICATE_NAME_ALICE)).thenReturn(CollectionUtils.set(trustedCert, trustedCertBob));

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(pec));

        assertEquals(2, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_NAME));
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_MULTIPLE));
    }

    /**
     * fail with an audit on FindException
     */
    @Test
    public void testCheckRequest_FindExceptionOnCertificateLookup_LookupErrorAuditedAndAssertionFails() throws Exception {
        when(cache.findByName(CERTIFICATE_NAME_ALICE)).thenThrow(new FindException());

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(pec));

        assertEquals(2, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_NAME));
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_ERROR));
    }

    /**
     * fail cleanly if given malformed subject DN
     */
    @Test
    @BugNumber(12232)
    public void testCheckRequest_MalformedSubjectDNSpecified_LookupErrorAuditedAndAssertionFails() throws Exception {
        assertion.setLookupType(LookupTrustedCertificateAssertion.LookupType.CERT_SUBJECT_DN);
        assertion.setCertSubjectDn("malformed");

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_ERROR));
    }

    /**
     * fail cleanly if given malformed issuer DN
     */
    @Test
    @BugNumber(12232)
    public void testCheckRequest_MalformedIssuerDNSpecified_LookupErrorAuditedAndAssertionFails() throws Exception {
        assertion.setLookupType(LookupTrustedCertificateAssertion.LookupType.CERT_ISSUER_SERIAL);
        assertion.setCertIssuerDn("malformed");
        assertion.setCertSerialNumber("8473");

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_ERROR));
    }

    /**
     * fail cleanly if given malformed cert serial number
     */
    @Test
    @BugNumber(12384)
    public void testCheckRequest_MalformedSerialNumberSpecified_LookupErrorAuditedAndAssertionFails() throws Exception {
        assertion.setLookupType(LookupTrustedCertificateAssertion.LookupType.CERT_ISSUER_SERIAL);
        assertion.setCertIssuerDn("cn=blah");
        assertion.setCertSerialNumber("234324 234"); // malformed

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_ERROR));
    }

    /**
     * not audit a stack trace by default if a FindException occurs
     */
    @Test
    @BugNumber(12386)
    public void testCheckRequest_FindExceptionWithCauseThrown_StackTraceNotAuditedByDefaultAndAssertionFails() throws Exception {
        when(cache.findByName(CERTIFICATE_NAME_ALICE)).thenThrow(new FindException("fail", new RuntimeException("cause")));

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(pec));

        assertEquals(2, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_NAME));
        assertTrue(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_ERROR));
        assertFalse(audit.isAuditPresent(AssertionMessages.CERT_ANY_LOOKUP_ERROR, true));
    }
}