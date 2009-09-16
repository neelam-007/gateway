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

    private final String elementsVerifiedVarName;
    private final Set<String> permittedSignatureMethodUris;
    private final Set<String> permittedDigestMethodUris;

    public ServerNonSoapCheckVerifyResultsAssertion(NonSoapCheckVerifyResultsAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub) throws InvalidXpathException {
        super(assertion, logger, beanFactory, eventPub);
        elementsVerifiedVarName = assertion.prefix(NonSoapVerifyElementAssertion.VAR_ELEMENTS_VERIFIED);
        permittedDigestMethodUris = new HashSet<String>(Arrays.asList(assertion.getPermittedDigestMethodUris()));
        permittedSignatureMethodUris = new HashSet<String>(Arrays.asList(assertion.getPermittedSignatureMethodUris()));
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> shouldBeSignedElements) throws Exception {
        Set<X509Certificate> signingCerts = makeCertCollector();
        Set<Element> verifiedElements = getVerifiedElements(context);

        for (Element shouldBeSignedElement : shouldBeSignedElements) {
            if (!verifiedElements.contains(shouldBeSignedElement)) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Required element was not present in " + elementsVerifiedVarName);
                throw new AssertionStatusException(AssertionStatus.FALSIFIED);
            }

            // TODO lookup sig method, dig method, and signer cert
        }
        return AssertionStatus.__NOT_YET_IMPLEMENTED;
    }

    private Set<Element> getVerifiedElements(PolicyEnforcementContext context) throws NoSuchVariableException {
        Object verified = context.getVariable(elementsVerifiedVarName);
        if (!(verified instanceof Element[]))
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Variable " + elementsVerifiedVarName + " was of unxpected type " + verified.getClass().getSimpleName());
        return new HashSet<Element>(Arrays.asList((Element[]) verified));
    }

    /**
     * @return a Set that will collate certificates using their encoded forms
     */
    private static TreeSet<X509Certificate> makeCertCollector() {
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
