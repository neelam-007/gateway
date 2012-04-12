package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.XmlElementVerifierConfig;
import com.l7tech.server.security.XmlElementVerifier;
import com.l7tech.server.identity.cert.TrustedCertCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidXpathException;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.*;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;

import static com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion.*;

/**
 * Server side implementation of non-SOAP signature verification.
 */
public class ServerNonSoapVerifyElementAssertion extends ServerNonSoapSecurityAssertion<NonSoapVerifyElementAssertion> {
    private static final int COL_SIGNED_ELEMENT = 0;
    private static final int COL_SIGNER_CERT = 1;
    private static final int COL_SIG_METHOD_URI = 2;
    private static final int COL_DIG_METHOD_URI = 3;
    private static final int COL_VALIDATED_SIGNATURE_VALUES = 4;
    private static final int COL_SIGNATURE_ELEMENT = 5;

    private final XmlElementVerifier verifier;
    private final IdAttributeConfig idAttributeConfig;
    private final String[] varsUsed;

    public ServerNonSoapVerifyElementAssertion(NonSoapVerifyElementAssertion assertion, BeanFactory beanFactory) throws InvalidXpathException, ParseException, CertificateException {
        super(assertion);
        SecurityTokenResolver securityTokenResolver = beanFactory.getBean("securityTokenResolver", SecurityTokenResolver.class);
        TrustedCertCache trustedCertCache = beanFactory.getBean("trustedCertCache", TrustedCertCache.class);

        final FullQName[] idAttrsArray = assertion.getCustomIdAttrs();
        Collection<FullQName> ids = idAttrsArray == null || idAttrsArray.length < 1 ? XmlElementVerifierConfig.DEFAULT_ID_ATTRS : Arrays.asList(idAttrsArray);

        this.idAttributeConfig = IdAttributeConfig.makeIdAttributeConfig(ids);
        this.verifier = new XmlElementVerifier(assertion.config(), securityTokenResolver, trustedCertCache, getAudit(), logger);
        this.varsUsed = assertion.getVariablesUsed();
    }

    private static <T> Collection<T> getColumn(List<Object[]> table, int column, Class<T> clazz) {
        List<T> ret = new ArrayList<T>();
        for (Object[] row : table) {
            final Object obj = row[column];
            if (!clazz.isInstance(obj))
                throw new ClassCastException("Column " + column + " contains non-" + clazz);
            //noinspection unchecked
            ret.add((T)obj);
        }
        return ret;
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> affectedElements) throws Exception {
        List<Object[]> infos = new ArrayList<Object[]>();

        Map<String, Element> elementsById = DomUtils.getElementByIdMap(doc, idAttributeConfig);
        elementsById.put("", doc.getDocumentElement());

        Map<String, Object> variableMap = context.getVariableMap(varsUsed, getAudit());

        for (Element sigElement : affectedElements) {
            List<Object[]> results = verifier.verifySignature(sigElement, elementsById, variableMap);
            infos.addAll(results);
        }

        Collection<Element> signedElements = getColumn(infos, COL_SIGNED_ELEMENT, Element.class);
        Collection<X509Certificate> signerCerts = getColumn(infos, COL_SIGNER_CERT, X509Certificate.class);
        Collection<String> signatureMethodUris = getColumn(infos, COL_SIG_METHOD_URI, String.class);
        Collection<String> digestMethodUris = getColumn(infos, COL_DIG_METHOD_URI, String.class);
        Collection<String> signatureValues = getColumn(infos, COL_VALIDATED_SIGNATURE_VALUES, String.class);
        Collection<Element> signatureElements = getColumn(infos, COL_SIGNATURE_ELEMENT, Element.class);

        context.setVariable(assertion.prefix(VAR_ELEMENTS_VERIFIED), signedElements.toArray(new Element[signedElements.size()]));
        context.setVariable(assertion.prefix(VAR_SIGNING_CERTIFICATES), signerCerts.toArray(new X509Certificate[signerCerts.size()]));
        context.setVariable(assertion.prefix(VAR_SIGNATURE_METHOD_URIS), signatureMethodUris.toArray(new String[signatureMethodUris.size()]));
        context.setVariable(assertion.prefix(VAR_DIGEST_METHOD_URIS), digestMethodUris.toArray(new String[digestMethodUris.size()]));
        context.setVariable(assertion.prefix(VAR_SIGNATURE_VALUES), signatureValues.toArray(new String[signatureMethodUris.size()]));
        context.setVariable(assertion.prefix(VAR_SIGNATURE_ELEMENTS), signatureElements.toArray(new Element[signatureElements.size()]));

        return AssertionStatus.NONE;
    }
}
