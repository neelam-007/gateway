package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.external.assertions.saml2attributequery.SignResponseElementAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import org.bouncycastle.jce.X509Principal;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 28-Jan-2009
 * Time: 7:30:01 PM
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
public class ServerSignResponseElementAssertion extends AbstractServerAssertion<SignResponseElementAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSignResponseElementAssertion.class.getName());

    private SecurityTokenResolver securityTokenResolver;
    private final Auditor auditor;

    //- PUBLIC

    @SuppressWarnings({"UnusedDeclaration"})
    public ServerSignResponseElementAssertion( final SignResponseElementAssertion assertion,
                                             final ApplicationContext context )
        throws PolicyAssertionException
    {
        super(assertion);
        securityTokenResolver = (SecurityTokenResolver)context.getBean("securityTokenResolver");
        auditor = new Auditor(this, context, logger);
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Document doc;
            if(assertion.getInputMessageSource() == SignResponseElementAssertion.REQUEST_MESSAGE) {
                doc = context.getRequest().getXmlKnob().getDocumentWritable();
            } else if(assertion.getInputMessageSource() == SignResponseElementAssertion.RESPONSE_MESSAGE) {
                doc = context.getResponse().getXmlKnob().getDocumentWritable();
            } else if(assertion.getInputMessageSource() == SignResponseElementAssertion.MESSAGE_VARIABLE) {
                Object obj = (Object)context.getVariable(assertion.getInputMessageVariableName());
                if(obj == null) {
                    auditor.logAndAudit(AssertionMessages.MCM_VARIABLE_NOT_FOUND, assertion.getInputMessageVariableName());
                    return AssertionStatus.FAILED;
                } else if(!(obj instanceof Message)) {
                    auditor.logAndAudit(AssertionMessages.SAML2_AQ_REQUEST_DIGSIG_VAR_UNUSABLE );
                    return AssertionStatus.FAILED;
                }
                doc = ((Message)obj).getXmlKnob().getDocumentWritable();
            } else {
                return AssertionStatus.FAILED;
            }
            ElementCursor cursor = new DomElementCursor(doc);
            cursor.moveToRoot();
            XpathResult xpathResult = cursor.getXpathResult(assertion.getXpathExpression().compile(), new PolicyEnforcementContextXpathVariableFinder(context), true);

            if(xpathResult.getType() != XpathResult.TYPE_NODESET || xpathResult.getNodeSet().isEmpty()) {
                return AssertionStatus.FAILED;
            }

            String certificateDN = (String)context.getVariable(assertion.getPrivateKeyDnVariable());
            if(certificateDN == null) {
                return AssertionStatus.FAILED;
            }

            SignerInfo si = securityTokenResolver.lookupPrivateKeyByKeyName(certificateDN);

            if(si == null) {
                auditor.logAndAudit(AssertionMessages.SAML2_AQ_RESPONSE_ENCRYPT_SAML_ASSERTION_PK_NOT_FOUND, certificateDN);
                return AssertionStatus.FAILED;
            }

            KeyInfoInclusionType keyInfoInclusionType = KeyInfoInclusionType.CERT;
            if(KeyReference.SKI.getName().equals(assertion.getKeyReference())) {
                keyInfoInclusionType = KeyInfoInclusionType.STR_SKI;
            }

            for(XpathResultIterator it = xpathResult.getNodeSet().getIterator();it.hasNext();) {
                ElementCursor ec = it.nextElementAsCursor();
                RequestSigner.signSamlpRequest(doc, ec.asDomElement(), si.getPrivate(), si.getCertificateChain(), keyInfoInclusionType);
            }
        } catch(SAXException se) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { ExceptionUtils.getMessage(se) }, ExceptionUtils.getDebugException(se));
            return AssertionStatus.FAILED;
        } catch(SignatureException se) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { ExceptionUtils.getMessage(se) }, ExceptionUtils.getDebugException(se));
            return AssertionStatus.FAILED;
        } catch(InvalidXpathException ixe) {
            if (logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "Invalid xpath: " + ExceptionUtils.getMessage(ixe), ixe);
            auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID);
            return AssertionStatus.FAILED;
        } catch(XPathExpressionException xee) {
            if (logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "XPath expresion error: " + ExceptionUtils.getMessage(xee), xee);
            auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID);
            return AssertionStatus.FAILED;
        } catch(NoSuchVariableException nsve) {
            auditor.logAndAudit(AssertionMessages.VARIABLE_IS_NULL, assertion.getPrivateKeyDnVariable());
        } catch (UnrecoverableKeyException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }


        return AssertionStatus.NONE;
    }

    private static String reverseDN(String dn) {
        X509Principal p = new X509Principal(true, dn);
        return p.getName();
    }
}
