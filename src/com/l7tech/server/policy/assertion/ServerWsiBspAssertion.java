package com.l7tech.server.policy.assertion;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Properties;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.namespace.NamespaceContext;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.WsiBspAssertion;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.message.Message;

/**
 * Server assertion for WSI-BSP compliance.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ServerWsiBspAssertion implements ServerAssertion {

    //- PUBLIC

    /**
     * Server assertion for WSI-BSP compliance.
     *
     * @param wsiBspAssertion assertion data object
     * @param springContext the application context to use
     */
    public ServerWsiBspAssertion(WsiBspAssertion wsiBspAssertion, ApplicationContext springContext) {
        this.wsiBspAssertion = wsiBspAssertion;
        this.auditor = new Auditor(this, springContext, logger);
        nsPreToUriMap = Collections.EMPTY_MAP;
        nsUriToPreMap = Collections.EMPTY_MAP;
        rulesMap = Collections.EMPTY_MAP;
        initRules();
    }

    /**
     * Check the request. If configured to do so this will fail the request if it does not comply
     * to the profile.
     *
     * @param context the context for the request.
     * @return the status
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException {
        AssertionStatus status = AssertionStatus.SERVER_ERROR;
        if(context.getRoutingStatus().equals(RoutingStatus.ROUTED)) {
            Message responseMessage = context.getResponse();
            status = processResponseMessage(responseMessage, context);
        }
        else {

            if(wsiBspAssertion.isCheckRequestMessages()) {
                Message requestMessage = context.getRequest();
                status = processRequestMessage(requestMessage, context);
            }
            else {
                status = AssertionStatus.NONE;
            }

            if(AssertionStatus.NONE.equals(status)) {
                // Add ourselves to validate the response if requested
                if(wsiBspAssertion.isCheckResponseMessages()) {
                    context.addDeferredAssertion(this, this);
                }
            }
        }

        return status;
    }

    //- PRIVATE

    //
    private static final Logger logger = Logger.getLogger(ServerWsiBspAssertion.class.getName());
    private static final String RESOURCE_RULES = "com/l7tech/server/policy/assertion/ServerWsiBspAssertion.rules.properties";
    private static final String RULE_POSTFIX = ".rule";
    private static final String PATH_POSTFIX = ".path";
    private static final String NAMESPACE_PREFIX = "Namespace.";

    //
    private final WsiBspAssertion wsiBspAssertion;
    private final Auditor auditor;
    private Map nsPreToUriMap; //Map of prefixes (String) to namespace uris (String)
    private Map nsUriToPreMap; //Map of namespace uri (String) to prefixes (List)
    private Map rulesMap;      //Map of XPaths to descriptions

    /**
     *
     */
    private AssertionStatus processRequestMessage(Message requestMessage,
                                                  PolicyEnforcementContext context) throws IOException {
        AssertionStatus result = AssertionStatus.SERVER_ERROR;

        if(!ensureSoap(requestMessage)) {
            auditor.logAndAudit(AssertionMessages.WSI_BSP_REQUEST_NON_SOAP);
            result = AssertionStatus.BAD_REQUEST;
        }
        else {
            Document requestDocument = getDocument(requestMessage);
            if(requestDocument==null) {
                result = AssertionStatus.BAD_REQUEST;
            }
            else {
                try {
                    boolean success = true;
                    for(Iterator ruleIter=rulesMap.keySet().iterator(); ruleIter.hasNext();) {
                        XPathExpression xpe = (XPathExpression) ruleIter.next();
                        if(!"true".equals(xpe.evaluate(requestDocument))) {
                            if(wsiBspAssertion.isAuditResponseNonCompliance())
                                auditor.logAndAudit(AssertionMessages.WSI_BSP_REQUEST_NON_COMPLIANT, getDetails(xpe));
                            success = false;
                            if(wsiBspAssertion.isFailOnNonCompliantRequest()) break; // fail fast
                        }
                    }
                    result = success || !wsiBspAssertion.isFailOnNonCompliantRequest()
                            ? AssertionStatus.NONE
                            : AssertionStatus.FALSIFIED;

                    if(AssertionStatus.FALSIFIED.equals(result)) {
                        auditor.logAndAudit(AssertionMessages.WSI_BSP_REQUEST_FAIL);
                    }                    
                }
                catch(XPathExpressionException xpee) {
                    auditor.logAndAudit(AssertionMessages.WSI_BSP_XPATH_ERROR, null, xpee);
                    result = AssertionStatus.FAILED;
                }
            }
        }

        return result;
    }

    /**
     *
     */
    private AssertionStatus processResponseMessage(Message responseMessage,
                                                   PolicyEnforcementContext context) throws IOException {
        AssertionStatus result = AssertionStatus.SERVER_ERROR;

        if(!ensureSoap(responseMessage)) {
            auditor.logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_NON_SOAP);
            result = AssertionStatus.BAD_REQUEST;
        }
        else {
            Document responseDocument = getDocument(responseMessage);
            if(responseDocument==null) {
                result = AssertionStatus.FALSIFIED;
            }
            else {
                try {
                    boolean success = true;
                    for(Iterator ruleIter=rulesMap.keySet().iterator(); ruleIter.hasNext();) {
                        XPathExpression xpe = (XPathExpression) ruleIter.next();
                        if(!"true".equals(xpe.evaluate(responseDocument))) {
                            if(wsiBspAssertion.isAuditResponseNonCompliance())
                                auditor.logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_NON_COMPLIANT, getDetails(xpe));
                            success = false;
                            if(wsiBspAssertion.isFailOnNonCompliantResponse()) break;
                        }
                    }
                    result = success | !wsiBspAssertion.isFailOnNonCompliantResponse()
                            ? AssertionStatus.NONE
                            : AssertionStatus.FALSIFIED;

                    if(AssertionStatus.FALSIFIED.equals(result)) {
                        auditor.logAndAudit(AssertionMessages.WSI_BSP_RESPONSE_FAIL);
                    }
                }
                catch(XPathExpressionException xpee) {
                    auditor.logAndAudit(AssertionMessages.WSI_BSP_XPATH_ERROR, null, xpee);
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
        String description = (String) rulesMap.get(xpe);
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
    private Document getDocument(Message message) throws IOException {
        Document document = null;

        try {
            document = message.getXmlKnob().getDocumentReadOnly();
        }
        catch(SAXException se) {
            throw new CausedIOException(se);
        }

        return document;
    }

    /**
     *
     */
    private void initRules() {
        ClassLoader loader = ServerWsiBspAssertion.class.getClassLoader();
        InputStream in = null;
        try {
            in = loader.getResourceAsStream(RESOURCE_RULES);
            if(in!=null) {
                Properties props = new Properties();
                props.load(in);
                Map newRules = new HashMap(props.size());
                Map newNsPreToUri = new HashMap();
                Map newNsUriToPre = new HashMap();

                XPathFactory xpf = XPathFactory.newInstance();

                loadNamespaces(props, newNsPreToUri, newNsUriToPre);
                nsPreToUriMap = Collections.unmodifiableMap(newNsPreToUri);
                nsUriToPreMap = Collections.unmodifiableMap(newNsUriToPre);  //TODO do we care about the mutable List?

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

    private void loadNamespaces(Map props, Map newNsPreToUri, Map newNsUriToPre) {
            for(Iterator iterator = props.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                if(key.startsWith(NAMESPACE_PREFIX)) {
                    // Load namespace
                    String nsPrefix = key.substring(NAMESPACE_PREFIX.length());
                    String nsUri = value;

                    if(nsPrefix.length()==0 || nsUri.length()==0) {
                        continue;
                    }

                    newNsPreToUri.put(nsPrefix, nsUri);
                    List uriList = (List) newNsUriToPre.get(nsUri);
                    if(uriList==null) {
                        uriList = new ArrayList(4);
                        newNsUriToPre.put(nsUri, uriList);
                    }
                    uriList.add(nsPrefix);
                }
            }
    }

    private void loadRules(XPathFactory xpf, Properties props, Map newRules) {
        for(Iterator iterator = props.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            if(key!=null && value!=null) {
                if(key.endsWith(PATH_POSTFIX)) {
                    // Load rule
                    String ruleId = key.substring(0, key.length()-PATH_POSTFIX.length());
                    String description = props.getProperty(ruleId + RULE_POSTFIX);

                    if(description==null) description = "No description for rule.";
                    description = ruleId + ": " + description;

                    XPath xpath = xpf.newXPath();
                    xpath.setNamespaceContext(getNamespaceContext());
                    try {
                        XPathExpression xpe = xpath.compile(value);
                        newRules.put(xpe, description);
                    }
                    catch(XPathExpressionException xpee) {
                        logger.log(Level.WARNING, "Error parsing XPath for rule '"+ruleId
                                +"', xpath is '"+value+"',", xpee);
                    }
                }
            }
        }

    }

    private NamespaceContext getNamespaceContext() {
        return new NamespaceContext(){
            public String getNamespaceURI(String prefix) {
                return (String) nsPreToUriMap.get(prefix);
            }

            public String getPrefix(String namespaceURI) {
                String prefix = null;
                Iterator iter = getPrefixes(namespaceURI);
                if(iter.hasNext()) {
                    prefix = (String) iter.next();
                }
                return prefix;
            }

            public Iterator getPrefixes(String namespaceURI) {
                List prefixes = (List) nsUriToPreMap.get(namespaceURI);
                Iterator prefixIter = null;
                if(prefixes!=null) {
                    prefixIter = prefixes.iterator();
                }
                else {
                    prefixIter = Collections.EMPTY_LIST.iterator();
                }
                return prefixIter;
            }
        };
    }
}
