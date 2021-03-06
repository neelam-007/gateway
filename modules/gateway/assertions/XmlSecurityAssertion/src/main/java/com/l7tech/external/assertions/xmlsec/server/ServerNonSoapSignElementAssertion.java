package com.l7tech.external.assertions.xmlsec.server;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.external.assertions.xmlsec.NonSoapSignElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidXpathException;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server implementation of signing arbitrary XML elements in a non-SOAP message.
 */
public class ServerNonSoapSignElementAssertion extends ServerNonSoapSecurityAssertion<NonSoapSignElementAssertion> {
    private final BeanFactory beanFactory;

    private final FullQName attrname;
    private final boolean enableImplicitEmptyUriDocRef;

    public ServerNonSoapSignElementAssertion(NonSoapSignElementAssertion assertion, BeanFactory beanFactory) throws InvalidXpathException, ParseException {
        super(assertion);
        this.beanFactory = beanFactory;
        String attrNameStr = assertion.getCustomIdAttributeQname();
        this.attrname = attrNameStr == null || attrNameStr.length() < 1 ? null : FullQName.valueOf(attrNameStr);
        this.enableImplicitEmptyUriDocRef = "".equals(attrNameStr);
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> affectedElements) throws Exception {
        SignerInfo signer = ServerAssertionUtils.getSignerInfo(beanFactory, assertion);

        final String detachedVar = assertion.getDetachedSignatureVariableName();
        if (detachedVar != null) {
            HashMap<String, Element> elementsToSignWithIDs = generateIds(affectedElements);
            Element signature = DsigUtil.createSignature(elementsToSignWithIDs, doc, signer.getCertificate(), signer.getPrivate(), null, null, assertion.getDigestAlgName(), assertion.getRefDigestAlgName(), assertion.isForceEnvelopedTransform(), enableImplicitEmptyUriDocRef);
            context.setVariable(detachedVar, signature);
        } else {
            int count = 1;
            for (Element elementToSign : affectedElements) {
                count = signElement(count, elementToSign, signer);
            }
        }

        return AssertionStatus.NONE;
    }

    private int signElement(int count, Element elementToSign, SignerInfo signer) throws SignatureException, SignatureStructureException, XSignatureException, UnrecoverableKeyException {
        final String idValue;

        // Avoid generating the reference id for the root element if requested.
        // Root element is unique to the given document and hence not necessary to attach ID attribute to it to reference inside signature/signedinfo/reference element.
        if (enableImplicitEmptyUriDocRef && elementToSign.getParentNode().getNodeType() == Node.DOCUMENT_NODE) {
            idValue = "";
        } else {
            final Pair<Integer, String> p = generateId(count, elementToSign);
            count = p.left;
            idValue = p.right;
        }

        final Map<String, Element> elementsToSignWithIDs = new HashMap<>();
        elementsToSignWithIDs.put(idValue, elementToSign);
        Element signature = DsigUtil.createSignature(elementsToSignWithIDs, elementToSign.getOwnerDocument(), signer.getCertificate(), signer.getPrivate(), null, null, assertion.getDigestAlgName(), assertion.getRefDigestAlgName(), true, enableImplicitEmptyUriDocRef);

        Node firstChild = elementToSign.getFirstChild();
        if (NonSoapSignElementAssertion.SignatureLocation.FIRST_CHILD.equals(assertion.getSignatureLocation()) && firstChild != null) {
            elementToSign.insertBefore(signature, firstChild);
        } else {
            elementToSign.appendChild(signature);
        }

        return count;
    }

    private HashMap<String, Element> generateIds(List<Element> elementsToSign) {
        int count = 0;
        HashMap<String, Element> output = new HashMap<String, Element>();
        for (Element el : elementsToSign) {
            if (enableImplicitEmptyUriDocRef && el == el.getOwnerDocument().getDocumentElement()) {
                output.put("", el); // Use empty string reference to implicitly point at document root
            } else {
                String id = generateId(count++, el).getValue();
                output.put(id, el);
            }
        }
        return output;
    }

    private Pair<Integer, String> generateId(int count, Element element) {
        String ns;
        String name;
        if (attrname == null || attrname.getLocal() == null || attrname.getLocal().length() < 1) {
            ns = null;
            name = DsigUtil.getIdAttribute(element);
        } else {
            // Custom (may modify DOM to locate/create appropriate namespace declarations if a global attribute is specified)
            ns = attrname.getNsUri();
            if (ns == null) {
                // Treat as local attr, possibly with forced use of undeclared ns prefix if so configured
                name = attrname.getFullName();
            } else {
                // Treat as global attribue; find a prefix to use, adding new xmlns decl if necessary
                String desiredPrefix = attrname.getPrefix();
                if (desiredPrefix == null || desiredPrefix.length() < 1)
                    desiredPrefix = "ns";
                String prefix = DomUtils.getOrCreatePrefixForNamespace(element, ns, desiredPrefix);
                name = prefix == null
                        ? attrname.getLocal()
                        : prefix + ":" + attrname.getLocal();
            }
        }

        String id = ns == null ? element.getAttribute( name ) : element.getAttributeNS( ns, name );
        if (id != null && id.trim().length() > 0)
            return new Pair<Integer, String>(count, id);

        byte[] randbytes = new byte[16];
        RandomUtil.nextBytes(randbytes);
        id = element.getLocalName() + "-" + count++ + "-" + HexUtils.hexDump(randbytes);

        if (ns == null) {
            element.setAttributeNS( XMLConstants.NULL_NS_URI, name, id );
        } else {
            element.setAttributeNS( ns, name, id);
        }
        
        return new Pair<Integer, String>(count, id);
    }    
}
