package com.l7tech.external.assertions.ncesdeco.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.ncesdeco.NcesDecoratorAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlAssertionGenerator;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.soap.SoapUtil;
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
            this.signerInfo = ServerAssertionUtils.getSignerInfo(spring, assertion);
        } catch (KeyStoreException e) {
            throw new RuntimeException("Unable to read private key for outbound message decoration: " + ExceptionUtils.getMessage(e), e);
        }
        this.samlAssertionGenerator = new SamlAssertionGenerator(signerInfo);
        this.martha = new WssDecoratorImpl();
        this.varsUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message msg;
        try {
            msg = context.getTargetMessage(assertion);
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.MESSAGE_TARGET_ERROR, e.getVariable(), ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }
        final String what = assertion.getTargetName();

        try {
            if (!msg.isSoap()) {
                auditor.logAndAudit(AssertionMessages.NCESDECO_NOT_SOAP, what);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.NCESDECO_BAD_XML, new String[]{what, ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.BAD_REQUEST;
        }

        final String template = assertion.getSamlAssertionTemplate();
        if (assertion.isSamlIncluded() && (template == null || template.length() == 0) && context.getDefaultAuthenticationContext().getLastCredentials() == null) {
            auditor.logAndAudit(AssertionMessages.NCESDECO_NO_CREDS);
            if (assertion.getTarget() == TargetMessageType.REQUEST) {
                // No point setting these flags for creds missing from non-request
                context.setAuthenticationMissing();
                context.setRequestPolicyViolated();
                return AssertionStatus.AUTH_REQUIRED;
            } else {
                return AssertionStatus.FAILED;
            }
        }

        final DecorationRequirements decoReq;
        final boolean applyImmediately;
        if (assertion.getTarget() == TargetMessageType.RESPONSE && assertion.isDeferDecoration()) {
            decoReq = msg.getSecurityKnob().getOrMakeDecorationRequirements();
            applyImmediately = false;
        } else {
            decoReq = new DecorationRequirements();
            applyImmediately = true;
        }
        decoReq.setSecurityHeaderActor(null);
        decoReq.setSecurityHeaderReusable(true);
        decoReq.setTimestampResolution(DecorationRequirements.TimestampResolution.MILLISECONDS);            

        Document doc;
        try {
            doc = msg.getXmlKnob().getDocumentWritable();
        } catch (SAXException e) {
            // Extremely unlikely--isSoap() already succeeded
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Parse failure after isSoap() success");
        }

        final Element securityEl = SoapUtil.getOrMakeSecurityElement(doc);

        try {
            addBodySignature(doc, decoReq);
            if (assertion.isSamlIncluded()) addSaml(doc, securityEl, decoReq, context);
            addMessageId(doc, decoReq);
            addTimestamp(decoReq);
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.NCESDECO_IDFE, new String[]{what, ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.BAD_REQUEST;
        }

        decoReq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
        decoReq.setSenderMessageSigningCertificate(signerInfo.getCertificateChain()[0]);

        if (!applyImmediately) {
            // Response decoration will be applied later, all at once
            return AssertionStatus.NONE;
        }

        try {
            martha.decorateMessage(msg, decoReq);
            return AssertionStatus.NONE;
        } catch (InvalidDocumentFormatException e) {
            auditor.logAndAudit(AssertionMessages.NCESDECO_IDFE, new String[]{what, ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.FAILED;
        } catch (GeneralSecurityException e) {
            auditor.logAndAudit(AssertionMessages.NCESDECO_WARN_MISC, new String[]{what, ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.FAILED;
        } catch (DecoratorException e) {
            auditor.logAndAudit(AssertionMessages.NCESDECO_WARN_MISC, new String[]{what, ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.FAILED;
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.NCESDECO_BAD_XML, new String[]{what, ExceptionUtils.getMessage(e)}, e);
            return AssertionStatus.FAILED;
        }
    }

    private void addBodySignature(Document msg, DecorationRequirements decoReq) throws InvalidDocumentFormatException {
        decoReq.getElementsToSign().add(SoapUtil.getBodyElement(msg));
    }

    private void addMessageId( final Document doc,
                               final DecorationRequirements decoReq ) throws InvalidDocumentFormatException {
        final boolean reuseMessageId = assertion.isUseExistingWsa();

        String wsaNs = assertion.getWsaNamespaceUri();
        if (wsaNs == null || wsaNs.length() == 0) wsaNs = SoapUtil.WSA_NAMESPACE;

        final Element header = SoapUtil.getHeaderElement(doc);

        // bug 8340
        // Depending on a flag set in the assertion, an existing wsa:MessageID
        // will be either be re-created (per spec) or re-used
        final Element existingMessageIdEl = XmlUtil.findFirstChildElementByName(header, wsaNs, SoapUtil.MESSAGEID_EL_NAME);
        if ( existingMessageIdEl != null ) {
            if ( reuseMessageId ) {
                decoReq.getElementsToSign().add(existingMessageIdEl); // add in case an unknown addressing spec is in use
                return;
            } else {
                header.removeChild( existingMessageIdEl );
            }
        }

        final String uuid;
        if ( assertion.isNodeBasedUuid() ) {
            uuid = UUIDGenerator.getInstance().generateTimeBasedUUID(macAddress).toString();
        } else {
            uuid = java.util.UUID.randomUUID().toString();
        }

        final Element messageIdEl = XmlUtil.createAndAppendElementNS( header, SoapUtil.MESSAGEID_EL_NAME, wsaNs, "wsa");
        final StringBuilder sb = new StringBuilder();
        final String prefix = assertion.getMessageIdUriPrefix();
        if (prefix != null) {
            sb.append(prefix);
        }
        sb.append(uuid);
        final Text uuidText = doc.createTextNode(sb.toString());
        messageIdEl.appendChild(uuidText);

        decoReq.getElementsToSign().add(messageIdEl); // add in case an unknown addressing spec is in use
    }

    private void addTimestamp(DecorationRequirements decoReq) {
        decoReq.setSignTimestamp(true);
    }

    private void addSaml(Document doc, Element security, DecorationRequirements decoReq, PolicyEnforcementContext context) throws InvalidDocumentFormatException {
        Element samlEl;
        SamlAssertion samlAss;
        String template = assertion.getSamlAssertionTemplate();
        if (template != null && template.length() > 0) {
            try {
                samlEl = XmlUtil.parse(new StringReader(ExpandVariables.process(template, context.getVariableMap(varsUsed, auditor), auditor)), false).getDocumentElement();
                samlAss = SamlAssertion.newInstance(samlEl);
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
            final SubjectStatement authnStmt = SubjectStatement.createAuthenticationStatement(context.getDefaultAuthenticationContext().getLastCredentials(), SubjectStatement.SENDER_VOUCHES, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);

            try {
                samlEl = samlAssertionGenerator.createAssertion(authnStmt, opts).getDocumentElement();
                samlAss = SamlAssertion.newInstance(samlEl);
            } catch (GeneralSecurityException e) {
                // Can't happen as long as {@link Options#isSignAssertion()} == false
                throw new IllegalStateException("Assertion Generator failed with a signature exception, but no signature was required");
            } catch (SAXException e) {
                throw new InvalidDocumentFormatException("Generated SAML Assertion could not be parsed", e);
            }
        }

        if (assertion.isSamlUseStrTransform()) {
            decoReq.setSenderSamlToken(samlAss, true);
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
