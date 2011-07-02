package com.l7tech.external.assertions.xmlsec.server;

import com.ibm.xml.enc.KeyInfoResolvingException;
import com.ibm.xml.enc.StructureException;
import com.l7tech.external.assertions.xmlsec.NonSoapSecurityAssertionBase;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathVariableContext;
import com.l7tech.xml.xpath.XpathVariableFinderVariableContext;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Utility superclass for server implementations of non-SOAP XML encryption/decryption/signature/verification assertions.
 */
public abstract class ServerNonSoapSecurityAssertion<AT extends NonSoapSecurityAssertionBase> extends AbstractServerAssertion<AT>  {
    protected final AssertionStatus ASSERTION_STATUS_BAD_MESSAGE;

    protected final DOMXPath xpath;
    protected final String verb;
    protected final String unableMessage;

    /**
     * Create support class for server-side impl of non-SOAP XML security assertion.
     *
     * @param assertion the policy assertion bean.  Required.
     * @throws InvalidXpathException if the XpathExpressioun will not compile
     */
    public ServerNonSoapSecurityAssertion(AT assertion) throws InvalidXpathException {
        super(assertion);
        String verb = assertion.getVerb();
        if (verb == null) verb = "process";
        this.verb = verb;
        this.unableMessage = "Unable to " + verb + " elements(s): ";
        XpathExpression xpe = assertion.getXpathExpression();
        if (xpe == null || xpe.getExpression() == null)
            throw new InvalidXpathException("XPath expression not set");
        try {
            this.xpath = new DOMXPath(xpe.getExpression());
            xpe.getNamespaces();
            for (Map.Entry<String, String> entry : xpe.getNamespaces().entrySet()) {
                this.xpath.addNamespace(entry.getKey(), entry.getValue());
            }
            this.xpath.setVariableContext(new XpathVariableFinderVariableContext(null));
        } catch (JaxenException e) {
            throw new InvalidXpathException(e);
        }
        ASSERTION_STATUS_BAD_MESSAGE = TargetMessageType.REQUEST.equals(assertion.getTarget()) ? AssertionStatus.BAD_REQUEST : AssertionStatus.SERVER_ERROR;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            final Message message = context.getTargetMessage(assertion);
            // TODO use read-only DOM view whenever we know for a fact no Element vars will ever be saved that are later used to modify the DOM
            final Document doc = message.getXmlKnob().getDocumentWritable();
            List<Element> affectedElements = findElements(context, doc);

            return processAffectedElements(context, message, doc, affectedElements);

        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.MESSAGE_TARGET_ERROR, e.getVariable(), ExceptionUtils.getMessage(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (SAXException e) {
            logAndAudit(AssertionMessages.XPATH_MESSAGE_NOT_XML, assertion.getTargetName());
            return ASSERTION_STATUS_BAD_MESSAGE;
        } catch (JaxenException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Unable to evaluate XPath expression: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        } catch (InvalidDocumentFormatException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { unableMessage + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return ASSERTION_STATUS_BAD_MESSAGE;
        } catch (GeneralSecurityException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { unableMessage + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (KeyInfoResolvingException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { unableMessage + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (StructureException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { unableMessage + ExceptionUtils.getMessage(e) }, e);
            return ASSERTION_STATUS_BAD_MESSAGE;
        } catch (ProcessorException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { unableMessage + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.BAD_REQUEST;
        } catch (AssertionStatusException e) {
            return e.getAssertionStatus();
        } catch (Exception e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { unableMessage + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.SERVER_ERROR;
        }
    }

    protected abstract AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> affectedElements) throws Exception;

    protected List<Element> findElements(PolicyEnforcementContext context, final Document doc) throws Exception {
        return XpathVariableContext.doWithVariableFinder(new PolicyEnforcementContextXpathVariableFinder(context), new Callable<List<Element>>() {
            @Override
            public List<Element> call() throws Exception {
                List nodes = xpath.selectNodes(doc);
                if (nodes == null || nodes.isEmpty()) {
                    final String msg = "XPath evaluation did not match any elements";
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, msg);
                    throw new AssertionStatusException(AssertionStatus.FALSIFIED, msg);
                }
                List<Element> affectedElements = new ArrayList<Element>();
                for (Object node : nodes) {
                    if (node instanceof Element) {
                        affectedElements.add((Element) node);
                    } else {
                        final String msg = "XPath evaluation produced non-Element result of type " + node.getClass();
                        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, msg);
                        throw new AssertionStatusException(msg);
                    }
                }
                return affectedElements;
            }
        });
    }
}
