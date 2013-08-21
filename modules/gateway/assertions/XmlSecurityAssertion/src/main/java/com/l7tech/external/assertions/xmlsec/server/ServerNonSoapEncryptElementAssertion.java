package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.xml.XmlElementEncryptionResolvedConfig;
import com.l7tech.security.xml.XmlElementEncryptor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.security.XmlElementEncryptorConfigUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.InvalidXpathException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;

/**
 * Server side implementation of the XmlSecurityAssertion.
 *
 * @see com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion
 */
public class ServerNonSoapEncryptElementAssertion extends ServerNonSoapSecurityAssertion<NonSoapEncryptElementAssertion> {

    private final String[] varsUsed;

    public ServerNonSoapEncryptElementAssertion(NonSoapEncryptElementAssertion assertion)
            throws PolicyAssertionException, InvalidXpathException, IOException, CertificateException, NoSuchAlgorithmException, NoSuchVariableException {
        super(assertion);
        this.varsUsed = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> elementsToEncrypt) throws Exception {
        final XmlElementEncryptionResolvedConfig encryptionResolvedConfig =
                XmlElementEncryptorConfigUtils.getXmlElementEncryptorConfig(assertion.config(), context.getVariableMap(varsUsed, getAudit()), getAudit());
        final XmlElementEncryptor elementEncryptor = new XmlElementEncryptor(encryptionResolvedConfig);

        Pair<Element, SecretKey> ek = elementEncryptor.createEncryptedKey(doc, assertion.config().isUseOaep(), null);

        for (Element element : elementsToEncrypt)
            elementEncryptor.encryptAndReplaceElement(element, ek);
        return AssertionStatus.NONE;
    }
}
