package com.l7tech.server.policy.assertion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.SAXException;

import com.l7tech.message.Message;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.NamespaceContextImpl;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.CursorXPathFactory;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * Base class for assertions that use XPath to perform validation on request
 * and/or response messages.
 *
 * <p>You need to implement getRulesResource() so that the validation rules can be loaded.</p>
 *
 * <p>Currently request and response validation use the same rules.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public abstract class ServerXpathValidationAssertion<AT extends Assertion> extends AbstractServerAssertion<AT> {    

    //- PUBLIC

    /**
     * Check the request. If configured to do so this will fail the request if it does not comply
     * to the profile.
     *
     * @param context the context for the request.
     * @return the status
     */
    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException {
        AssertionStatus status;
        if(context.getRoutingStatus().equals(RoutingStatus.ROUTED)) {
            Message responseMessage = context.getResponse();
            status = processResponseMessage(responseMessage);
        }
        else {

            if(isCheckRequestMessages()) {
                Message requestMessage = context.getRequest();
                status = processRequestMessage(requestMessage);
            }
            else {
                status = AssertionStatus.NONE;
            }

            if(AssertionStatus.NONE.equals(status)) {
                // Add ourselves to validate the response if requested
                if(isCheckResponseMessages()) {
                    context.addDeferredAssertion(this, this);
                }
            }
        }

        return status;
    }

    

    //- PROTECTED

    protected ServerXpathValidationAssertion(AT assertion, Logger validationLogger) {
        super(assertion);
        this.logger = validationLogger;
        namespaceContext = new NamespaceContextImpl(Collections.<String,String>emptyMap());
        rulesMap = Collections.emptyMap();
        initRules();
    }

    /**
     * Should request messages be subject to validation?
     *
     * @return true
     */
    protected boolean isCheckRequestMessages() {
        return true;
    }

    /**
     * Should response messages be subject to validation?
     *
     * @return true
     */
    protected boolean isCheckResponseMessages() {
        return true;
    }

    /**
     * Should request messages be failed for non-compliance?
     *
     * @return true
     */
    protected boolean isFailOnNonCompliantRequest() {
        return true;
    }

    /**
     * Should response messages be failed for non-compliance?
     *
     * @return false
     */
    protected boolean isFailOnNonCompliantResponse() {
        return false;
    }

    /**
     * Get an InputStream from which rules should be read.
     *
     * <p>The format for the rules file is: </p>
     *
     * <code>
     * Namespace.soap = http://schemas.xmlsoap.org/soap/envelope/
     *
     * E0001.rule = Invalid SOAP namespace
     * E0001.path = 0=count(//*[namespace-uri()='http://www.w3.org/2001/06/soap-envelope' or namespace-uri()='http://www.w3.org/2001/09/soap-envelope' or namespace-uri()='http://www.w3.org/2003/05/soap-envelope' or namespace-uri()='urn:schemas-xmlsoap-org:soap.v1'])
     * </code>
     *
     * So namespaces are declared with the 'Namespace.' prefix and each XPath
     * rule has a description (rule) and path (XPath that evaluates to true/false)
     */
    protected abstract InputStream getRulesResource();

    /**
     * Override to perform some action for a non SOAP request.
     *
     * <p>This will be called at most once per message</p>
     *
     * <p>This implementation does nothing.</p>
     */
    protected void onRequestNonSoap(){}

    /**
     * Override to perform some action for a non SOAP response.
     *
     * <p>This will be called at most once per message</p>
     *
     * <p>This implementation does nothing.</p>
     */
    protected void onResponseNonSoap(){}

    /**
     * Override to perform some action for a non-compliant request.
     *
     * <p>If isFailOnNonCompliantRequest returns true this method will be
     * called at most once per message. If isFailOnNonCompliantRequest returns
     * false this method will be called for each failed rule.</p>
     *
     * <p>This implementation does nothing.</p>
     *
     * @param ruleId the id for the failed rule
     * @param description the description for the failed rule
     * @see #isFailOnNonCompliantRequest()
     */
    protected void onRequestNonCompliance(String ruleId, String description){}

    /**
     * Override to perform some action for a non-compliant response.
     *
     * <p>If isFailOnNonCompliantResponse returns true this method will be
     * called at most once per message. If isFailOnNonCompliantResponse returns
     * false this method will be called for each failed rule.</p>
     *
     * <p>This implementation does nothing.</p>
     *
     * @param ruleId the id for the failed rule
     * @param description the description for the failed rule
     * @see #isFailOnNonCompliantResponse()
     */
    protected void onResponseNonCompliance(String ruleId, String description){}

    /**
     * Override to perform some action for a failed request.
     *
     * <p>This will only be called if isFailOnNonCompliantRequest is true.</p>
     *
     * <p>This implementation does nothing.</p>
     *
     * @see #isFailOnNonCompliantRequest()
     */
    protected void onRequestFailure(){}

    /**
     * Override to perform some action for a failed response.
     *
     * <p>This will only be called if isFailOnNonCompliantResponse is true.</p>
     *
     * <p>This implementation does nothing.</p>
     *
     * @see #isFailOnNonCompliantResponse()
     */
    protected void onResponseFailure(){}

    /**
     * Override to perform some action for a failed response.
     *
     * <p>This will only be called if isFailOnNonCompliantResponse is true.</p>
     *
     * <p>This implementation logs a warning message.</p>
     */
    protected void onXPathError(XPathException xpe){
        logger.log(Level.WARNING, "Error processing XPath", xpe);
    }

    //- PACKAGE

    /**
     * Used by test code only.
     */
    Map getRules() {
        return rulesMap;
    }

    //- PRIVATE

    //
    private static final String RULE_POSTFIX = ".rule";
    private static final String PATH_POSTFIX = ".path";
    private static final String NAMESPACE_PREFIX = "Namespace.";

    //
    private final Logger logger;
    private NamespaceContext namespaceContext;
    private Map<XPathExpression,String> rulesMap;      //Map of XPaths to descriptions

    /**
     *
     */
    private AssertionStatus processRequestMessage(Message requestMessage) throws IOException {
        AssertionStatus result;

        if(logger.isLoggable(Level.FINE)) {
            logger.fine("Processing request message.");
        }

        if(!ensureSoap(requestMessage)) {
            onRequestNonSoap();
            result = AssertionStatus.BAD_REQUEST;
        }
        else {
            Object xpathContext = getXPathContext(requestMessage);
            if(xpathContext==null) {
                result = AssertionStatus.BAD_REQUEST;
            }
            else {
                try {
                    boolean success = true;
                    for (Object o : rulesMap.keySet()) {
                        XPathExpression xpe = (XPathExpression) o;
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.finest("Evaluating XPath '" + xpe.toString() + "'.");
                        }
                        if (!"true".equals(xpe.evaluate(xpathContext))) {
                            if (logger.isLoggable(Level.FINEST)) {
                                logger.finest("XPath evaluated to false.");
                            }
                            String[] details = getDetails(xpe);
                            onRequestNonCompliance(details[0], details[1]);
                            success = false;
                            if (isFailOnNonCompliantRequest()) break; // fail fast
                        }
                    }
                    result = success || !isFailOnNonCompliantRequest()
                            ? AssertionStatus.NONE
                            : AssertionStatus.FALSIFIED;

                    if(AssertionStatus.FALSIFIED.equals(result)) {
                        onRequestFailure();
                    }
                }
                catch(XPathExpressionException xpee) {
                    onXPathError(xpee);
                    result = AssertionStatus.FAILED;
                }
            }
        }

        return result;
    }

    /**
     *
     */
    private AssertionStatus processResponseMessage(Message responseMessage) throws IOException {
        AssertionStatus result;

        if(logger.isLoggable(Level.FINE)) {
            logger.fine("Processing response message.");
        }

        if(!ensureSoap(responseMessage)) {
            onResponseNonSoap();
            result = AssertionStatus.BAD_REQUEST;
        }
        else {
            Object xpathContext = getXPathContext(responseMessage);
            if(xpathContext==null) {
                result = AssertionStatus.FALSIFIED;
            }
            else {
                try {
                    boolean success = true;
                    for (Object o : rulesMap.keySet()) {
                        XPathExpression xpe = (XPathExpression) o;
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.finest("Evaluating XPath '" + xpe.toString() + "'.");
                        }
                        if (!"true".equals(xpe.evaluate(xpathContext))) {
                            if (logger.isLoggable(Level.FINEST)) {
                                logger.finest("XPath evaluated to false.");
                            }
                            String[] details = getDetails(xpe);
                            onResponseNonCompliance(details[0], details[1]);
                            success = false;
                            if (isFailOnNonCompliantResponse()) break;
                        }
                    }
                    result = success || !isFailOnNonCompliantResponse()
                            ? AssertionStatus.NONE
                            : AssertionStatus.FALSIFIED;

                    if(AssertionStatus.FALSIFIED.equals(result)) {
                        onResponseFailure();
                    }
                }
                catch(XPathExpressionException xpee) {
                    onXPathError(xpee);
                    result = AssertionStatus.FAILED;
                }
            }
        }

        return result;
    }

    /**
     *
     */
    private String[] getDetails(XPathExpression xpe) {
        String description = rulesMap.get(xpe);
        if(description==null) description = "";
        return description.split(": ", 2);
    }

    /**
     *
     */
    private boolean ensureSoap(Message message) throws IOException {
        boolean isSoap = false;

        try {
            if(message.isSoap()) {
                isSoap = true;
            }
        }
        catch(SAXException se) {
            throw new CausedIOException(se);
        }

        return isSoap;
    }

    /**
     *
     */
    private Object getXPathContext(Message message) throws IOException {
        Object context;

        try {
            ElementCursor ec = message.getXmlKnob().getElementCursor();
            ec.moveToRoot();
            context = ec;
        }
        catch(SAXException se) {
            throw new CausedIOException(se);
        }

        return context;
    }

    /**
     *
     */
    private void initRules() {
        InputStream in = null;
        try {
            in = getRulesResource();
            if(in!=null) {
                Properties props = new Properties();
                props.load(in);
                Map<XPathExpression,String> newRules = new HashMap<XPathExpression,String>(props.size());

                XPathFactory xpf = new CursorXPathFactory();//XPathFactory.newInstance();

                namespaceContext = loadNamespaces(props);

                loadRules(xpf, props, newRules);
                rulesMap = Collections.unmodifiableMap(newRules);
            }
            else {
                logger.warning("Unable to load rules, NOT performing any runtime validation.");
            }
        }
        catch(IOException ioe) {
            logger.log(Level.WARNING, "Error loading compliance rules.", ioe);
        }
        finally {
            ResourceUtils.closeQuietly(in);
        }
    }

    private NamespaceContext loadNamespaces(Map props) {
        Map<String,String> namespaceMap = new HashMap<String,String>();

        for ( Object o : props.entrySet() ) {
            Map.Entry entry = (Map.Entry) o;
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            if (key.startsWith(NAMESPACE_PREFIX)) {
                // Load namespace
                String nsPrefix = key.substring(NAMESPACE_PREFIX.length());

                if (nsPrefix.length() == 0 || value.length() == 0) {
                    continue;
                }

                namespaceMap.put(nsPrefix, value);
            }
        }

        return new NamespaceContextImpl(namespaceMap);
    }

    private void loadRules(XPathFactory xpf, Properties props, Map<XPathExpression,String> newRules) {
        for (Object o : props.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            if (key != null && value != null) {
                if (key.endsWith(PATH_POSTFIX)) {
                    // Load rule
                    String ruleId = key.substring(0, key.length() - PATH_POSTFIX.length());
                    String description = props.getProperty(ruleId + RULE_POSTFIX);

                    if (description == null) description = "No description for rule.";
                    description = ruleId + ": " + description;

                    XPath xpath = xpf.newXPath();
                    xpath.setNamespaceContext(namespaceContext);
                    try {
                        XPathExpression xpe = xpath.compile(value);
                        newRules.put(xpe, description);
                    }
                    catch (XPathExpressionException xpee) {
                        logger.log(Level.WARNING, "Error parsing XPath for rule '" + ruleId
                                + "', xpath is '" + value + "',", xpee);
                    }
                }
            }
        }

    }

}
