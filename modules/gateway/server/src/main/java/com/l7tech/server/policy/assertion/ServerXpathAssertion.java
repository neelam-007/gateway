package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.ResponseXpathAssertion;
import com.l7tech.policy.assertion.SimpleXpathAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.server.message.HasOutputVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import static com.l7tech.xml.xpath.XpathResult.MultiValuedXpathResultAdapter;

/**
 * Abstract superclass for server assertions whose operation centers around running a single xpath against
 * either the request or the response message, possibly with variable capture.
 *
 * <p>Related function specifications:
 * <ul>
 *  <li><a href="http://sarek.l7tech.com/mediawiki/index.php?title=XML_Variables">XML Variables</a> (4.3)
 * </ul>
 */
public abstract class ServerXpathAssertion<AT extends SimpleXpathAssertion> extends ServerXpathBasedAssertion<AT> {
    private final boolean req; // true = operate on request; false = operate on response
    private final String staticFoundVar;
    private final String staticCountVar;
    private final String staticResultVar;
    private final String staticMultipleResultsVar;
    private final String staticElementVar;
    private final String staticMultipleElementsVar;
    private final boolean xpathContainsVariables;
    private final boolean xpathReferencesTargetDocument;
    private final Set<String> variablesUsedBySuccessors;
    private String vfound;
    private String vresult;
    private String vmultipleResults;
    private String vcount;
    private String velement;
    private String vmultipleElements;

    public ServerXpathAssertion(AT assertion, boolean isReq) {
        super(assertion);
        this.req = isReq;

        variablesUsedBySuccessors = PolicyVariableUtils.getVariablesUsedBySuccessors(assertion);
        staticFoundVar = variablesUsedBySuccessors.contains(assertion.foundVariable()) ? assertion.foundVariable() : null;
        staticResultVar = variablesUsedBySuccessors.contains(assertion.resultVariable()) ? assertion.resultVariable() : null;
        staticMultipleResultsVar = variablesUsedBySuccessors.contains(assertion.multipleResultsVariable()) ? assertion.multipleResultsVariable() : null;
        staticCountVar = variablesUsedBySuccessors.contains(assertion.countVariable()) ? assertion.countVariable() : null;
        staticElementVar = variablesUsedBySuccessors.contains(assertion.elementVariable()) ? assertion.elementVariable() : null;
        staticMultipleElementsVar = variablesUsedBySuccessors.contains(assertion.multipleElementsVariable()) ? assertion.multipleElementsVariable() : null;
        xpathContainsVariables = compiledXpathUsesVariables();
        xpathReferencesTargetDocument = compiledXpathReferencesTargetDocument();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        // Determines the message object to apply XPath to.
        final Message message;
        if (req) {
            message = context.getRequest();
        } else {
            if (!(assertion instanceof ResponseXpathAssertion)) {
                // Should never happen.
                throw new RuntimeException("Unexpect assertion type (expected=" + ResponseXpathAssertion.class + ", actual=" + assertion.getClass() + ").");
            }
            final ResponseXpathAssertion ass = (ResponseXpathAssertion)assertion;
            final String variableName = ass.getXmlMsgSrc();
            if (variableName == null) {
                message = context.getResponse();
            } else {
                try {
                    final Object value = context.getVariable(variableName);
                    if (!(value instanceof Message)) {
                        final String msg = "context variable of wrong type (expected=" + Message.class.getSimpleName() + ", actual=" + value.getClass().getSimpleName() + ")";
                        logAndAudit(AssertionMessages.MESSAGE_TARGET_ERROR, variableName, msg);
                        return AssertionStatus.SERVER_ERROR;
                    }
                    message = (Message) value;
                } catch (NoSuchVariableException e) {
                    logAndAudit(AssertionMessages.NO_SUCH_VARIABLE_WARNING, variableName);
                    return AssertionStatus.SERVER_ERROR;
                }
            }
        }

        // Use the statically-configured variable names, if possible
        if (context instanceof HasOutputVariables) {
            // User of this PEC might use variables -- we will have to check PEC outputs as well
            HasOutputVariables hasOutputVariables = (HasOutputVariables) context;
            Set<String> pecOutputs = hasOutputVariables.getOutputVariableNames();
            vfound = staticFoundVar != null ? staticFoundVar : pecOutputs.contains(assertion.foundVariable()) ? assertion.foundVariable() : null;
            vresult = staticResultVar != null ? staticResultVar : pecOutputs.contains(assertion.resultVariable()) ? assertion.resultVariable() : null;
            vmultipleResults = staticMultipleResultsVar != null ? staticMultipleResultsVar : pecOutputs.contains(assertion.multipleResultsVariable()) ? assertion.multipleResultsVariable() : null;
            vcount = staticCountVar != null ? staticCountVar : pecOutputs.contains(assertion.countVariable()) ? assertion.countVariable() : null;
            velement = staticElementVar != null ? staticElementVar : pecOutputs.contains(assertion.elementVariable()) ? assertion.elementVariable() : null;
            vmultipleElements = staticMultipleElementsVar != null ? staticMultipleElementsVar : pecOutputs.contains(assertion.multipleElementsVariable()) ? assertion.multipleElementsVariable() : null;
        } else {
            // Nobody uses our variables but our own policy, so it is safe to rely on the var names collected at compiled-time
            vfound = staticFoundVar;
            vresult = staticResultVar;
            vmultipleResults = staticMultipleResultsVar;
            vcount = staticCountVar;
            velement = staticElementVar;
            vmultipleElements = staticMultipleElementsVar;
        }

        context.setVariable(vfound, SimpleXpathAssertion.FALSE);
        context.setVariable(vcount, "0");
        context.setVariable(vresult, null);
        context.setVariable(vmultipleResults, null);
        context.setVariable(velement, null);
        context.setVariable(vmultipleElements, null);

        CompiledXpath compiledXpath = getCompiledXpath(context);
        if (compiledXpath == null) {
            logAndAudit( AssertionMessages.XPATH_PATTERN_INVALID_MORE_INFO, getXpath() );
            //the xpath could not be compiled, so the assertion cannot work ... FAILED
            return AssertionStatus.FAILED;
        }

        final ElementCursor cursor;
        try {
            boolean usePlaceholderMessage = false;
            if (!message.isXml(true)) {
                if (xpathReferencesTargetDocument) {
                    auditNotXml();

                    //the mesage isn't XML, so an XPath can't be applied ... NOT_APPLICABLE
                    return AssertionStatus.NOT_APPLICABLE;
                }

                // XPath does not refer to the target document at all -- create an empty placeholder target
                // document to match against
                usePlaceholderMessage = true;
            }

            if (usePlaceholderMessage) {
                cursor = new DomElementCursor(XmlUtil.createEmptyDocument());
            } else if( vmultipleElements != null || xpathContainsVariables ||
                (!compiledXpath.getXpathVersion().equals(XpathVersion.XPATH_1_0) && !compiledXpath.getXpathVersion().equals(XpathVersion.UNSPECIFIED))) {
                // Use a cursor backed by DOM so we can have Element results and/or XPath variables and/or XPath versions above 1.0
                logAndAudit( AssertionMessages.XPATH_NOT_ACCELERATED );
                cursor = new DomElementCursor(message.getXmlKnob().getDocumentReadOnly());
            } else {
                final XmlKnob xmlKnob = message.getXmlKnob();
                cursor = xmlKnob.getElementCursor();
            }

        } catch (SAXException e) {
            auditNotXml();
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "SAXException during XPath processing: " + ExceptionUtils.getMessage(e), e);
            //can't proceed cause the XML message probably isn't well formed ... FAILED
            logAndAudit( AssertionMessages.XPATH_PATTERN_IS, getXpath() );
            return AssertionStatus.FAILED;
        }

        cursor.moveToRoot();

        XpathResult xpathResult = null;
        try {
            XpathVariableFinder variableFinder = xpathContainsVariables ? new PolicyEnforcementContextXpathVariableFinder(context) : null;
            xpathResult = cursor.getXpathResult(compiledXpath, variableFinder, velement != null);
        } catch (XPathExpressionException e) {
            // Log it, but treat it as null
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "XPath failed: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        if (xpathResult == null) {
            logAndAudit( req ? AssertionMessages.XPATH_PATTERN_NOT_MATCHED_REQUEST_MI
                    : AssertionMessages.XPATH_PATTERN_NOT_MATCHED_RESPONSE_MI,
                    getXpath() );

            //the xpath ran, but nothing was matched ... FALSIFIED
            return AssertionStatus.FALSIFIED;
        }

        final short resultType = xpathResult.getType();
        switch (resultType) {
            case XpathResult.TYPE_BOOLEAN:
                if (areAnyOutVariablesDefined()) {
                    setVariables(xpathResult, context, XpathResult::getBoolean, MultiValuedXpathResultAdapter::getBooleanArray);
                }

                if (xpathResult.getBoolean()) {
                    logAndAudit( AssertionMessages.XPATH_RESULT_TRUE );
                    context.setVariable(vfound, SimpleXpathAssertion.TRUE);
                    return AssertionStatus.NONE;
                }

                logAndAudit( AssertionMessages.XPATH_RESULT_FALSE );
                logAndAudit( AssertionMessages.XPATH_PATTERN_IS, getXpath() );
                context.setVariable(vfound, SimpleXpathAssertion.FALSE);
                return AssertionStatus.FALSIFIED;

            case XpathResult.TYPE_NUMBER:
                if (areAnyOutVariablesDefined()) {
                    setVariables(xpathResult, context, XpathResult::getNumber, MultiValuedXpathResultAdapter::getNumberArray);
                }

                context.setVariable(vfound, SimpleXpathAssertion.TRUE);
                // TODO what to log for this?
                // logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                return AssertionStatus.NONE;

            case XpathResult.TYPE_STRING:
                if (areAnyOutVariablesDefined()) {
                    setVariables(xpathResult, context, XpathResult::getString, MultiValuedXpathResultAdapter::getStringArray);
                }

                context.setVariable(vfound, SimpleXpathAssertion.TRUE);
                // TODO what to log for this?
                // logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                return AssertionStatus.NONE;

            case XpathResult.TYPE_NODESET:
                /* FALLTHROUGH and handle nodeset */
                break;

            default:
                logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        " XPath evaluation produced unknown result type " + resultType );
                logAndAudit( AssertionMessages.XPATH_PATTERN_IS, getXpath() );
                return AssertionStatus.FAILED;
        }

        // Ok, it's a nodeset.
        XpathResultNodeSet ns = xpathResult.getNodeSet();

        final int size = ns.size();
        if (size > 1)
            logAndAudit( AssertionMessages.XPATH_MULTIPLE_RESULTS, Integer.toString( size ) );

        if (size < 1) {
            logAndAudit( req ? AssertionMessages.XPATH_PATTERN_NOT_MATCHED_REQUEST_MI
                    : AssertionMessages.XPATH_PATTERN_NOT_MATCHED_RESPONSE_MI,
                    getXpath() );
            return AssertionStatus.FALSIFIED;
        }

        context.setVariable(vfound, SimpleXpathAssertion.TRUE);
        if (vcount != null) context.setVariable(vcount, Integer.toString(size));

        int nodeType = ns.getType(0);
        switch (nodeType) {
            case Node.ELEMENT_NODE:
                logAndAudit( AssertionMessages.XPATH_ELEMENT_FOUND );
                if (vresult != null) context.setVariable(vresult, ns.getNodeValue(0));
                if (vmultipleResults != null) {
                    if(size > 0) {
                        String[] val = new String[size];
                        for(int i = 0;i < size;i++) {
                            val[i] = ns.getNodeValue(i);
                        }
                        context.setVariable(vmultipleResults, val);
                    }
                }
                if (velement != null) {
                    XpathResultIterator it = ns.getIterator();
                    if (it.hasNext()) context.setVariable(velement, it.nextElementAsCursor().asString());
                }
                if (vmultipleElements != null) {
                    if(size > 0) {
                        Element[] val = new Element[size];
                        int i = 0;
                        for(XpathResultIterator it = ns.getIterator();it.hasNext() && i < size;) {
                            val[i++] = it.nextElementAsCursor().asDomElement();
                        }
                        context.setVariable(vmultipleElements, val);
                    }
                }
                return AssertionStatus.NONE;

            case Node.TEXT_NODE:
                logAndAudit( AssertionMessages.XPATH_TEXT_NODE_FOUND );
                if (vresult != null || velement != null) {
                    String val = ns.getNodeValue(0);
                    context.setVariable(vresult, val);
                    context.setVariable(velement, val);
                }
                if (vmultipleElements != null || vmultipleResults != null) {
                    String[] val;
                    if(ns.size() > 0) {
                        val = new String[ns.size()];
                        for(int i = 0;i < val.length;i++) {
                            val[i] = ns.getNodeValue(i);
                        }
                    } else {
                        val = new String[0];
                    }
                    context.setVariable(vmultipleElements, val);
                    context.setVariable(vmultipleResults, val);
                }
                return AssertionStatus.NONE;

            default:
                /* FALLTHROUGH and handle other type of node */
                break;
        }

        logAndAudit( AssertionMessages.XPATH_OTHER_NODE_FOUND );
        if (vresult != null || velement != null) {
            String val = ns.getNodeValue(0);
            context.setVariable(vresult, val);
            context.setVariable(velement, val);
        }
        if (vmultipleElements != null || vmultipleResults != null) {
            String[] val;
            if(ns.size() > 0) {
                val = new String[ns.size()];
                for(int i = 0;i < val.length;i++) {
                    val[i] = ns.getNodeValue(i);
                }
            } else {
                val = new String[0];
            }
            context.setVariable(vmultipleElements, val);
            context.setVariable(vmultipleResults, val);
        }
        return AssertionStatus.NONE;
    }

    private <T> void setVariables(XpathResult xpathResult, PolicyEnforcementContext context, Function<XpathResult, T> getter, Function<XpathResult.MultiValuedXpathResultAdapter, T[]> arrayGetter) {
        if (xpathResult instanceof XpathResult.MultiValuedXpathResultAdapter) {
            XpathResult.MultiValuedXpathResultAdapter multiValuedXpathResultAdapter = (XpathResult.MultiValuedXpathResultAdapter) xpathResult;
            setOutVariables(context, getter.apply(multiValuedXpathResultAdapter), arrayGetter.apply(multiValuedXpathResultAdapter));
        } else {
            setOutVariables(context, getter.apply(xpathResult));
        }

    }

    private void setOutVariables(PolicyEnforcementContext context, Object val) {
        setOutVariables(context, val, null);
    }

    private void setOutVariables(PolicyEnforcementContext context, Object val, Object[] vals) {
        context.setVariable(vresult, val);
        context.setVariable(velement, val);

        if(vals!=null) {
            context.setVariable(vmultipleResults, vals);
            context.setVariable(vmultipleElements, vals);
            context.setVariable(vcount, Integer.toString(vals.length));
        } else {
            context.setVariable(vmultipleResults, val);
            context.setVariable(vmultipleElements, val);
            context.setVariable(vcount, "1");
        }
    }

    private void auditNotXml() {
        logAndAudit( req ? AssertionMessages.XPATH_REQUEST_NOT_XML : AssertionMessages.XPATH_RESPONSE_NOT_XML );
    }

    private boolean areAnyOutVariablesDefined() {
        return (vresult != null || velement != null || vmultipleResults != null || vmultipleElements != null);
    }

}