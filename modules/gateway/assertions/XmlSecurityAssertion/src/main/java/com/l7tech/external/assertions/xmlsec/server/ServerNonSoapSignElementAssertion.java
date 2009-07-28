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
import com.l7tech.util.HexUtils;
import com.l7tech.xml.InvalidXpathException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Server imlementation of signing arbitrary XML elements in a non-SOAP message.
 */
public class ServerNonSoapSignElementAssertion extends ServerNonSoapSecurityAssertion<NonSoapSignElementAssertion> {
    private static final Logger logger = Logger.getLogger(ServerNonSoapSignElementAssertion.class.getName());
    private final BeanFactory beanFactory;

    private static final Random random = new SecureRandom();

    public ServerNonSoapSignElementAssertion(NonSoapSignElementAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub) throws InvalidXpathException {
        super(assertion, logger, beanFactory, eventPub);
        this.beanFactory = beanFactory;
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> affectedElements) throws Exception {
        SignerInfo signer = ServerAssertionUtils.getSignerInfo(beanFactory, assertion);

        int count = 1;
        for (Element elementToSign : affectedElements) {
            count = signElement(count, elementToSign, signer);
        }

        return AssertionStatus.NONE;
    }

    private int signElement(int count, Element elementToSign, SignerInfo signer) throws SignatureException, SignatureStructureException, XSignatureException {
        count = generateId(count, elementToSign);
        Element signature = DsigUtil.createEnvelopedSignature(elementToSign, signer.getCertificate(), signer.getPrivate(), null, null, null);
        elementToSign.appendChild(signature);
        return count;
    }

    private int generateId(int count, Element element) {
        String id = element.getAttribute("Id");
        if (id != null && id.trim().length() > 0)
            return count;

        byte[] randbytes = new byte[16];
        random.nextBytes(randbytes);
        id = element.getLocalName() + "-" + count++ + "-" + HexUtils.hexDump(randbytes);
        element.setAttribute("Id", id);
        return count;
    }    
}
