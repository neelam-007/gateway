package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.NonSoapCheckVerifyResultsAssertion;
import com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.InvalidXpathException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
public class ServerNonSoapCheckVerifyResultsAssertion extends ServerNonSoapSecurityAssertion<NonSoapCheckVerifyResultsAssertion> {
    private static final Logger logger = Logger.getLogger(ServerNonSoapCheckVerifyResultsAssertion.class.getName());

    private final boolean allowMultiple;
    private final String elementsVerifiedVarName;
    private final String signatureMethodUrisVarName;
    private final String digestMethodUrisVarName;
    private final String signingCertificatesVarName;
    private final Set<String> permittedSignatureMethodUris;
    private final Set<String> permittedDigestMethodUris;

    public ServerNonSoapCheckVerifyResultsAssertion(NonSoapCheckVerifyResultsAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub) throws InvalidXpathException {
        super(assertion, logger, beanFactory, eventPub);
        allowMultiple = assertion.isAllowMultipleSigners();
        elementsVerifiedVarName = assertion.prefix(NonSoapVerifyElementAssertion.VAR_ELEMENTS_VERIFIED);
        signatureMethodUrisVarName = assertion.prefix(NonSoapVerifyElementAssertion.VAR_SIGNATURE_METHOD_URIS);
        digestMethodUrisVarName = assertion.prefix(NonSoapVerifyElementAssertion.VAR_DIGEST_METHOD_URIS);
        signingCertificatesVarName = assertion.prefix(NonSoapVerifyElementAssertion.VAR_SIGNING_CERTIFICATES);
        permittedDigestMethodUris = new HashSet<String>(Arrays.asList(assertion.getPermittedDigestMethodUris()));
        permittedSignatureMethodUris = new HashSet<String>(Arrays.asList(assertion.getPermittedSignatureMethodUris()));
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> shouldBeSignedElements) throws Exception {
        assert !shouldBeSignedElements.isEmpty();

        Set<X509Certificate> signingCerts = makeCertCollector();
        Map<Element, List<Integer>> rowIndexesByVerifiedElement = getRowIndexesByVerifiedElement(context);
        String[] columnSignatureMethodUris = getContextVariableOfClass(context, signatureMethodUrisVarName, String[].class);
        String[] columnsDigestMethodUris = getContextVariableOfClass(context, digestMethodUrisVarName, String[].class);
        X509Certificate[] columnSigningCertificates = getContextVariableOfClass(context, signingCertificatesVarName, X509Certificate[].class);

        boolean first = true;
        for (Element element : shouldBeSignedElements) {
            List<Integer> indexes = rowIndexesByVerifiedElement.get(element);
            if (indexes == null || indexes.isEmpty()) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Required element was not covered by signature: " + element.getNodeName());
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            if (!allowMultiple && indexes.size() > 1) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Element was covered by more than one signature and multiple signatures are not permitted: " + element.getNodeName());
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            Set<X509Certificate> certsSigningThisElement = makeCertCollector();
            for (Integer idx : indexes) {
                checkPermitted(columnSignatureMethodUris, idx, permittedSignatureMethodUris, "SignatureMethod", element);
                checkPermitted(columnsDigestMethodUris, idx, permittedDigestMethodUris, "DigestMethod", element);
                checkIndexAndNotNull(columnSigningCertificates, idx, "signingCertificates", element);

                X509Certificate signerCert = columnSigningCertificates[idx];

                if (first) {
                    // Start by including all certs that signed the first element
                    signingCerts.add(signerCert);
                } else {
                    // Collect them all so we can compute the intersection
                    certsSigningThisElement.add(signerCert);
                }
            }

            if (!first) {
                // Intersect with signers of previously verified elements.
                // The intent here is that only certs which signed _every_ element matching the XPath will be kept and considered as credentials.
                certsSigningThisElement.retainAll(signingCerts);
                if (certsSigningThisElement.isEmpty()) {
                    // This element has no signing certs in common with the earlier elements.
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Element has no signing certificates in common with other signed elements: " + element.getNodeName());
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                }
                signingCerts.retainAll(certsSigningThisElement);
                if (signingCerts.isEmpty()) {
                    // This element has no signing certs in common with the earlier elements.
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Other signed elements have no signing certificate in common with element: " + element.getNodeName());
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED);
                }
            }

            first = false;
        }

        if (signingCerts.isEmpty()) {
            // Can't happen
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "No signing certificates were found");
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }

        if (!allowMultiple && signingCerts.size() > 1) {
            // Can't happen
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "More than one signing certificate was found and multiple signatures are not permitted");
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }

        if (assertion.isGatherCertificateCredentials()) {
            for (X509Certificate signingCert : signingCerts) {
                NonSoapSecurityServerUtil.addObjectAsCredentials(context.getAuthenticationContext(message), signingCert, NonSoapCheckVerifyResultsAssertion.class);
            }
        }

        // All is well
        return AssertionStatus.NONE;
    }

    private void checkPermitted(String[] values, int idx, Set<String> permitted, String digestOrSignature, Element element) throws AssertionStatusException {
        checkIndexAndNotNull(values, idx, digestOrSignature, element);

        if (!permitted.contains(values[idx])) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    "Element was not signed using a permitted " + digestOrSignature + ": " + element.getNodeName());
            throw new AssertionStatusException(AssertionStatus.FALSIFIED);
        }
    }

    private void checkIndexAndNotNull(Object[] values, int idx, String digestOrSignature, Element element) {
        assert idx >= 0;

        if (idx >= values.length) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    "Invalid signature results: not enough " + digestOrSignature + " results for element " + element.getNodeName() + " (index " + idx + ")");
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }

        if (values[idx] == null) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    "Invalid signature results: " + digestOrSignature + " result is null for element " + element.getNodeName() + " (index " + idx + ")");
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
        }
    }

    /*
     * Creates a table that can quickly find all row indexes in the singature results table that pertain to each element that was verified. 
     */
    private Map<Element, List<Integer>> getRowIndexesByVerifiedElement(PolicyEnforcementContext context) throws NoSuchVariableException {
        Element[] elms = getContextVariableOfClass(context, elementsVerifiedVarName, Element[].class);
        Map<Element, List<Integer>> ret = new HashMap<Element, List<Integer>>();
        for (int i = 0; i < elms.length; i++) {
            final Element elm = elms[i];
            List<Integer> indexes = ret.get(elm);
            if (indexes == null) {
                indexes = new ArrayList<Integer>();
                ret.put(elm, indexes);
            }
            indexes.add(i);
        }
        return ret;
    }

    private <T> T getContextVariableOfClass(PolicyEnforcementContext context, String varname, Class<T> expectedClass) throws NoSuchVariableException {
        final Object val = context.getVariable(varname);
        if (val == null)
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Variable " + varname + " value is null");
        final Class valClass = val.getClass();
        if (!(expectedClass.isAssignableFrom(valClass)))
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Variable " + varname + " value is of unexpected type " + valClass.getSimpleName());
        //noinspection unchecked
        return (T)val;
    }

    /**
     * @return a Set that will collate certificates using their encoded forms
     */
    static TreeSet<X509Certificate> makeCertCollector() {
        return new TreeSet<X509Certificate>(new Comparator<X509Certificate>() {
            @Override
            public int compare(X509Certificate a, X509Certificate b) {
                if (a == b)
                    return 0;
                try {
                    return ArrayUtils.compareArrays(a.getEncoded(), b.getEncoded());
                } catch (CertificateEncodingException e) {
                    throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Unable to encode certificate: " + ExceptionUtils.getMessage(e), e);
                }
            }
        });
    }
}
