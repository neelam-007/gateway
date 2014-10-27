package com.l7tech.external.assertions.validatecertificate.server;

import com.l7tech.external.assertions.validatecertificate.ValidateCertificateAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServerValidateCertificateAssertionTest {
    private static final String TEST_CERT = "testCert";
    private ValidateCertificateAssertion assertion;
    private ServerValidateCertificateAssertion serverAssertion;
    private PolicyEnforcementContext context;
    private TestAudit testAudit;
    @Mock
    private CertValidationProcessor certValidator;
    @Mock
    private X509Certificate cert;

    @Before
    public void setup() throws PolicyAssertionException {
        assertion = new ValidateCertificateAssertion();
        assertion.setLogOnly(false);
        assertion.setSourceVariable(TEST_CERT);
        serverAssertion = new ServerValidateCertificateAssertion(assertion);
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable(TEST_CERT, cert);
        testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.MapBuilder.<String, Object>builder()
                .put("auditFactory", testAudit.factory())
                .put("certValidationProcessor", certValidator)
                .map());
        when(cert.getSubjectDN()).thenReturn(new Principal() {
            @Override
            public String getName() {
                return "CN=test";
            }
        });
    }

    @Test(expected = PolicyAssertionException.class)
    public void emptySourceVariable() throws Exception {
        assertion.setSourceVariable("");
        serverAssertion = new ServerValidateCertificateAssertion(assertion);
    }

    @Test(expected = PolicyAssertionException.class)
    public void emptyVariablePrefix() throws Exception {
        assertion.setVariablePrefix("");
        serverAssertion = new ServerValidateCertificateAssertion(assertion);
    }

    @Test
    public void variableNotFound() throws Exception {
        context.setVariable(TEST_CERT, null);
        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertFalse((Boolean) context.getVariable("certificateValidation.passed"));
        assertEquals("No certificate found for variable: " + TEST_CERT, context.getVariable("certificateValidation.error"));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CERT_NOT_FOUND));
    }

    @Test
    public void variableNotCertificate() throws Exception {
        context.setVariable(TEST_CERT, "not a certificate");
        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertFalse((Boolean) context.getVariable("certificateValidation.passed"));
        assertEquals("No certificate found for variable: " + TEST_CERT, context.getVariable("certificateValidation.error"));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CERT_NOT_FOUND));
    }

    @Test
    public void validationOk() throws Exception {
        for (final CertificateValidationType type : CertificateValidationType.values()) {
            testValidationOk(type);
        }
    }

    @Test
    public void validationNotOk() throws Exception {
        for (final CertificateValidationType type : CertificateValidationType.values()) {
            testValidationNotOk(type);
            testAudit.reset();
        }
    }

    @Test
    public void validationCertificateException() throws Exception {
        for (final CertificateValidationType type : CertificateValidationType.values()) {
            testValidationWithException(type, new CertificateException("mocking exception"));
            testAudit.reset();
        }
    }

    @Test
    public void validationOkLogOnly() throws Exception {
        assertion.setLogOnly(true);
        testValidationOk(CertificateValidationType.CERTIFICATE_ONLY);
    }

    @Test
    public void validationNotOkLogOnly() throws Exception {
        assertion.setLogOnly(true);
        setupMock(CertificateValidationType.CERTIFICATE_ONLY, CertificateValidationResult.UNKNOWN);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertFalse((Boolean) context.getVariable("certificateValidation.passed"));
        assertEquals("Certificate CN=test validation (" + CertificateValidationType.CERTIFICATE_ONLY + ") failed with status: " + CertificateValidationResult.UNKNOWN, context.getVariable("certificateValidation.error"));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CERT_VALIDATION_STATUS_FAILURE));
    }

    @Test
    public void validationWithCertificateExceptionLogOnly() throws Exception {
        assertion.setLogOnly(true);
        setupMock(CertificateValidationType.CERTIFICATE_ONLY, new CertificateException("mocking exception"));
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertFalse((Boolean) context.getVariable("certificateValidation.passed"));
        assertEquals("Certificate CN=test validation (" + CertificateValidationType.CERTIFICATE_ONLY + ") failed: mocking exception", context.getVariable("certificateValidation.error"));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CERT_VALIDATION_FAILURE));
    }

    private void testValidationWithException(final CertificateValidationType validationType, final Exception ex) throws Exception {
        assertion.setValidationType(validationType);
        setupMock(validationType, ex);
        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
        assertFalse((Boolean) context.getVariable("certificateValidation.passed"));
        assertEquals("Certificate CN=test validation (" + validationType + ") failed: " + ex.getMessage(), context.getVariable("certificateValidation.error"));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CERT_VALIDATION_FAILURE));
    }

    private void testValidationNotOk(final CertificateValidationType validationType) throws Exception {
        for (final CertificateValidationResult result : CertificateValidationResult.values()) {
            if (!result.equals(CertificateValidationResult.OK)) {
                assertion.setValidationType(validationType);
                setupMock(validationType, result);
                assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(context));
                assertFalse((Boolean) context.getVariable("certificateValidation.passed"));
                assertEquals("Certificate CN=test validation (" + validationType + ") failed with status: " + result, context.getVariable("certificateValidation.error"));
                assertTrue(testAudit.isAuditPresent(AssertionMessages.CERT_VALIDATION_STATUS_FAILURE));
                testAudit.reset();
            }
        }
    }

    private void testValidationOk(final CertificateValidationType validationType) throws Exception {
        assertion.setValidationType(validationType);
        setupMock(validationType, CertificateValidationResult.OK);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertTrue((Boolean) context.getVariable("certificateValidation.passed"));
        assertContextVariableDoesNotExist("certificateValidation.error");
        assertFalse(testAudit.isAuditPresent(AssertionMessages.CERT_VALIDATION_STATUS_FAILURE));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.CERT_VALIDATION_FAILURE));
    }

    private void assertContextVariableDoesNotExist(final String name) {
        try {
            context.getVariable(name);
            fail("Expected NoSuchVariableException");
        } catch (final NoSuchVariableException e) {
            // expected
        }
    }

    private void setupMock(final CertificateValidationType validationType, final CertificateValidationResult toReturn) throws Exception {
        when(certValidator.check(any(X509Certificate[].class), eq(validationType), eq(validationType),
                eq(CertValidationProcessor.Facility.OTHER), any(Audit.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                validateCert(invocationOnMock);
                return toReturn;
            }
        });
    }

    private void setupMock(final CertificateValidationType validationType, final Exception toThrow) throws Exception {
        when(certValidator.check(any(X509Certificate[].class), eq(validationType), eq(validationType),
                eq(CertValidationProcessor.Facility.OTHER), any(Audit.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                validateCert(invocationOnMock);
                throw toThrow;
            }
        });
    }

    private void validateCert(final InvocationOnMock invocationOnMock) {
        final X509Certificate[] certs = (X509Certificate[]) invocationOnMock.getArguments()[0];
        assertEquals(1, certs.length);
        assertEquals(cert, certs[0]);
    }

}
