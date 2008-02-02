package com.l7tech.external.assertions.ncesdeco.server;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.message.Message;
import com.l7tech.common.security.xml.KeyInfoInclusionType;
import com.l7tech.common.security.saml.NameIdentifierInclusionType;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.DecoratorException;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.external.assertions.ncesdeco.NcesDecoratorAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.xmlsec.ServerResponseWssSignature;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.safehaus.uuid.EthernetAddress;
import org.safehaus.uuid.UUIDGenerator;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.util.logging.Logger;

/**
 * Server side implementation of the NcesDecoratorAssertion.
 *
 * @see com.l7tech.external.assertions.ncesdeco.NcesDecoratorAssertion
 */
public class ServerNcesDecoratorAssertion extends AbstractServerAssertion<NcesDecoratorAssertion> {
    private static final Logger logger = Logger.getLogger(ServerNcesDecoratorAssertion.class.getName());

    private final Auditor auditor;
    private final EthernetAddress macAddress;
    private final SignerInfo signerInfo;
    private final SamlAssertionGenerator samlAssertionGenerator;
    private final WssDecorator martha;
    private final String[] varsUsed;

    public ServerNcesDecoratorAssertion(NcesDecoratorAssertion assertion, ApplicationContext spring) throws PolicyAssertionException {
        super(assertion);

        this.auditor = new Auditor(this, spring, logger);
        ClusterInfoManager cim = (ClusterInfoManager) spring.getBean("clusterInfoManager");
        this.macAddress = new EthernetAddress(cim.getSelfNodeInf().getMac());
        try {
            this.signerInfo = ServerResponseWssSignature.getSignerInfo(spring, assertion);
        } catch (KeyStoreException e) {
            throw new RuntimeException("Unable to read private key for outbound message decoration: " + ExceptionUtils.getMessage(e), e);
        }
        this.samlAssertionGenerator = new SamlAssertionGenerator(signerInfo);
        this.martha = new WssDecoratorImpl();
        this.varsUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message msg;
        switch(assertion.getTarget()) {
            case REQUEST:
                msg = context.getRequest();
                break;
            case RESPONSE:
                msg = context.getResponse();
                break;
            case OTHER:
                msg = context.getRequestMessage(assertion.getOtherTargetMessageVariable());
                break;
            default:
                throw new PolicyAssertionException(assertion, "Unsupported decoration target: " + assertion.getTarget());
        }

        try {
            if (!msg.isSoap()) {
                auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, NcesDecoratorAssertion.class.getSimpleName(), "Unable to decorate message: Message not SOAP");
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, new String[] { NcesDecoratorAssertion.class.getSimpleName(), "Unable to decorate message: Parse failure: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        }

        if (context.getCredentials() == null) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, NcesDecoratorAssertion.class.getSimpleName(), "Unable to decorate message: No credentials have been collected");
            return AssertionStatus.AUTH_REQUIRED;
        }

        DecorationRequirements decoReq = new DecorationRequirements();
        decoReq.setSecurityHeaderActor(null);
        decoReq.setSecurityHeaderReusable(true);

        Document doc;
        try {
            doc = msg.getXmlKnob().getDocumentWritable();
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, new String[] { NcesDecoratorAssertion.class.getSimpleName(), "Unable to decorate message: Parse failure: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        }

        final Element securityEl = SoapUtil.getOrMakeSecurityElement(doc);

        try {
            addBodySignature(doc, decoReq);
            if (assertion.isSamlIncluded()) addSaml(doc, securityEl, decoReq, context);
            addMessageId(doc, decoReq);
            addTimestamp(decoReq);
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, new String[] { NcesDecoratorAssertion.class.getSimpleName(), "Unable to decorate message: Invalid Document Format: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.BAD_REQUEST;
        }

        decoReq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
        decoReq.setSenderMessageSigningCertificate(signerInfo.getCertificateChain()[0]);

        try {
            martha.decorateMessage(msg, decoReq);
            return AssertionStatus.NONE;
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, new String[] { NcesDecoratorAssertion.class.getSimpleName(), "Unable to decorate message: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        } catch (GeneralSecurityException e) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, new String[] { NcesDecoratorAssertion.class.getSimpleName(), "Unable to decorate message: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        } catch (DecoratorException e) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, new String[] { NcesDecoratorAssertion.class.getSimpleName(), "Unable to decorate message: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN, new String[] { NcesDecoratorAssertion.class.getSimpleName(), "Unable to decorate message: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        }
    }

    private void addBodySignature(Document msg, DecorationRequirements decoReq) throws InvalidDocumentFormatException {
        decoReq.getElementsToSign().add(SoapUtil.getBodyElement(msg));
    }

    private void addMessageId(Document doc, DecorationRequirements decoReq) throws InvalidDocumentFormatException {
        String uuid;
        if (assertion.isNodeBasedUuid()) {
            uuid = UUIDGenerator.getInstance().generateTimeBasedUUID(macAddress).toString();
        } else {
            uuid = java.util.UUID.randomUUID().toString();
        }
        String wsaNs = assertion.getWsaNamespaceUri();
        if (wsaNs == null || wsaNs.length() == 0) wsaNs = SoapUtil.WSA_NAMESPACE;

        Document messageIdDoc = XmlUtil.createEmptyDocument(SoapUtil.MESSAGEID_EL_NAME, "wsa", wsaNs);
        Element messageIdEl = messageIdDoc.getDocumentElement();

        messageIdEl.setAttributeNS(SoapUtil.WSU_NAMESPACE, "wsu:Id", SoapUtil.generateUniqueId("MessageID", 1));
        messageIdEl.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:wsu", SoapUtil.WSU_NAMESPACE);
        StringBuilder sb = new StringBuilder();
        String prefix = assertion.getMessageIdUriPrefix();
        if (prefix != null && prefix.length() > 0) {
            sb.append(prefix);
        }
        sb.append(uuid);
        Text uuidText = messageIdDoc.createTextNode(sb.toString());
        messageIdEl.appendChild(uuidText);

        messageIdEl = (Element) doc.importNode(messageIdEl, true);
        Element header = SoapUtil.getHeaderElement(doc);
        header.appendChild(messageIdEl);

        decoReq.getElementsToSign().add(messageIdEl);
    }

    private void addTimestamp(DecorationRequirements decoReq) {
        decoReq.setSignTimestamp();
    }

    private void addSaml(Document doc, Element security, DecorationRequirements decoReq, PolicyEnforcementContext context) throws InvalidDocumentFormatException {
        Element samlEl;
        String template = assertion.getSamlAssertionTemplate();
        if (template != null && template.length() > 0) {
            try {
                samlEl = XmlUtil.parse(new StringReader(ExpandVariables.process(template, context.getVariableMap(varsUsed, auditor), auditor)), false).getDocumentElement();
            } catch (SAXException e) {
                throw new InvalidDocumentFormatException("Provided SAML Assertion template could not be parsed", e);
            } catch (IOException e) {
                throw new RuntimeException(e); // Can't happen
            }
        } else {
            SamlAssertionGenerator.Options opts = new SamlAssertionGenerator.Options();
            opts.setSignAssertion(false);
            opts.setVersion(assertion.getSamlAssertionVersion());
            try {
                opts.setClientAddress(InetAddress.getByName(context.getRequest().getTcpKnob().getRemoteAddress()));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e); // Can't happen
            }
            final SubjectStatement authnStmt = SubjectStatement.createAuthenticationStatement(context.getLastCredentials(), SubjectStatement.SENDER_VOUCHES, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);

            try {
                samlEl = samlAssertionGenerator.createAssertion(authnStmt, opts).getDocumentElement();
            } catch (GeneralSecurityException e) {
                // Can't happen as long as {@link Options#isSignAssertion()} == false
                throw new IllegalStateException("Assertion Generator failed with a signature exception, but no signature was required");
            }
        }

        if (assertion.isSamlUseStrTransform()) {
            decoReq.setSenderSamlToken(samlEl, true);
        } else {
            samlEl.setAttributeNS(SoapUtil.WSU_NAMESPACE, "wsu:Id", SoapUtil.generateUniqueId("SAMLAssertion", 1));
            samlEl.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:wsu", SoapUtil.WSU_NAMESPACE);
            samlEl = (Element) doc.importNode(samlEl, true);
            security.appendChild(samlEl);
            decoReq.setSuppressSamlStrTransform(true);
            decoReq.getElementsToSign().add(samlEl);
        }
    }
}
