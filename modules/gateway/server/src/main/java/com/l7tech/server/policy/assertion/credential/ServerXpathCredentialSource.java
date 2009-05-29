package com.l7tech.server.policy.assertion.credential;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathVariableContext;
import com.l7tech.xml.xpath.XpathVariableFinderVariableContext;
import org.jaxen.FunctionContext;
import org.jaxen.JaxenException;
import org.jaxen.XPathFunctionContext;
import org.jaxen.dom.DOMXPath;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerXpathCredentialSource extends AbstractServerAssertion<XpathCredentialSource> {
    private static final Logger logger = Logger.getLogger(ServerXpathCredentialSource.class.getName());
    private static final FunctionContext XPATH_FUNCTIONS = new XPathFunctionContext(false);
    private final Auditor auditor;
    private final DOMXPath loginXpath, passwordXpath;
    private final XpathCredentialSource assertion;

    public ServerXpathCredentialSource(XpathCredentialSource assertion, ApplicationContext springContext) {
        super(assertion);
        this.assertion = assertion;
        this.auditor = new Auditor(this, springContext, logger);

        XpathExpression loginExpr = null;
        try {
            loginExpr = assertion.getXpathExpression();
            if (loginExpr == null) {
                logger.warning("Login XPath is null; assertion is non-functional");
                loginXpath = null;
            } else {
                loginXpath = new DOMXPath(loginExpr.getExpression());
                loginXpath.setFunctionContext(XPATH_FUNCTIONS);
                loginXpath.setVariableContext(new XpathVariableFinderVariableContext(null)); // uses thread-local rendezvous
                for (String prefix : loginExpr.getNamespaces().keySet()) {
                    String uri = loginExpr.getNamespaces().get(prefix);
                    loginXpath.addNamespace(prefix, uri);
                }
            }
        } catch (JaxenException e) {
            throw new RuntimeException("Invalid login xpath expression '" + loginExpr + "'", e);
        }

        XpathExpression passwordExpr = null;
        try {
            passwordExpr = assertion.getPasswordExpression();
            if (passwordExpr == null) {
                logger.warning("Password XPath is null; assertion is non-functional");
                passwordXpath = null;
            } else {
                passwordXpath = new DOMXPath(passwordExpr.getExpression());
                passwordXpath.setFunctionContext(XPATH_FUNCTIONS);
                passwordXpath.setVariableContext(new XpathVariableFinderVariableContext(null)); // uses thread-local rendezvous
                for (String prefix : passwordExpr.getNamespaces().keySet()) {
                    String uri = passwordExpr.getNamespaces().get(prefix);
                    passwordXpath.addNamespace(prefix, uri);
                }
            }
        } catch (JaxenException e) {
            throw new RuntimeException("Invalid password xpath expression '" + passwordExpr + "'", e);
        }
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            return XpathVariableContext.doWithVariableFinder(new PolicyEnforcementContextXpathVariableFinder(context),
                    new Callable<AssertionStatus>() {
                        @Override
                        public AssertionStatus call() throws Exception {
                            return doCheckRequest(context);
                        }
                    });
        } catch (IOException e) {
            throw e;
        } catch (PolicyAssertionException e) {
            throw e;
        } catch (Exception e) {
            throw new PolicyAssertionException(assertion, e);
        }
    }

    private AssertionStatus doCheckRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        XmlKnob requestXml = null;
        if (context.getRequest().isXml())
            requestXml = context.getRequest().getKnob(XmlKnob.class);

        if (requestXml == null) {
            auditor.logAndAudit(AssertionMessages.XPATHCREDENTIAL_REQUEST_NOT_XML);
            return AssertionStatus.NOT_APPLICABLE;
        }

        Document requestDoc;
        try {
            if (assertion.isRemoveLoginElement() || assertion.isRemovePasswordElement()) {
                requestDoc = requestXml.getDocumentWritable();
            } else {
                requestDoc = requestXml.getDocumentReadOnly();
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.XPATHCREDENTIAL_REQUEST_NOT_XML, null, e);
            return AssertionStatus.FAILED;
        }

        String login;
        try {
            login = find(requestDoc,
                    loginXpath,
                    AssertionMessages.XPATHCREDENTIAL_LOGIN_XPATH_NOT_FOUND,
                    AssertionMessages.XPATHCREDENTIAL_LOGIN_FOUND_MULTI,
                    AssertionMessages.XPATHCREDENTIAL_LOGIN_PARENT_NOT_ELEMENT,
                    assertion.isRemoveLoginElement(),
                    false); // Login can't be empty
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.XPATHCREDENTIAL_LOGIN_XPATH_FAILED, null, e);
            return AssertionStatus.FAILED;
        }

        String pass;
        try {
            pass = find(requestDoc,
                    passwordXpath,
                    AssertionMessages.XPATHCREDENTIAL_PASS_XPATH_NOT_FOUND,
                    AssertionMessages.XPATHCREDENTIAL_PASS_FOUND_MULTI,
                    AssertionMessages.XPATHCREDENTIAL_PASS_PARENT_NOT_ELEMENT,
                    assertion.isRemovePasswordElement(),
                    true); // Return zero-length Password if element present but empty
        } catch (Exception e) {
            auditor.logAndAudit(AssertionMessages.XPATHCREDENTIAL_PASS_XPATH_FAILED, null, e);
            return AssertionStatus.FAILED;
        }

        // Password is allowed to be zero-length, although no one should ever do so
        if (login == null || login.length() == 0 || pass == null) {
            return AssertionStatus.AUTH_REQUIRED;
        }

        context.getAuthenticationContext(context.getRequest()).addCredentials(LoginCredentials.makePasswordCredentials(login, pass.toCharArray(), XpathCredentialSource.class));

        return AssertionStatus.NONE;
    }

    private String find(Document requestDoc, DOMXPath xpath,
                        AuditDetailMessage notFoundMsg,
                        AuditDetailMessage multiMsg,
                        AuditDetailMessage notElementMsg,
                        boolean removeElement, boolean emptyOk)
            throws JaxenException
    {
        String value = null;
        Node foundNode = null;

        if (xpath == null) {
            auditor.logAndAudit(notFoundMsg);
            return null;
        }

        List result = xpath.selectNodes(requestDoc);
        if (result == null || result.size() == 0) {
            auditor.logAndAudit(notFoundMsg);
            return null;
        }

        if (result.size() > 1) {
            auditor.logAndAudit(multiMsg);
            return null;
        }

        Object o = result.get(0);
        if (o instanceof Node) {
            Node n = (Node) o;
            int type = n.getNodeType();
            switch (type) {
                case Node.TEXT_NODE:
                    // Got it
                    value = n.getNodeValue();
                    foundNode = n;
                    break;
                case Node.ELEMENT_NODE:
                    // Text must be an immediate child of the found element
                    Node child = n.getFirstChild();
                    while (child != null) {
                        if (child.getNodeType() == Node.TEXT_NODE) {
                            if (value != null) {
                                auditor.logAndAudit(multiMsg);
                                return null;
                            }
                            value = child.getNodeValue();
                            foundNode = child;
                        }
                        child = child.getNextSibling();
                    }

                    // We found the element but it was empty--this is equivalent to no password but
                    // treated differently from when the password wasn't found
                    if (emptyOk && value == null) value = "";

                    break;
            }
        } else if (o instanceof String) {
            value = (String)o;
        }

        if (value != null && removeElement) {
            if (foundNode == null) {
                auditor.logAndAudit(notElementMsg);
            } else {
                Node parent = foundNode.getParentNode();
                if (parent.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element)parent;
                    el.getParentNode().removeChild(el);
                } else {
                    auditor.logAndAudit(notElementMsg);
                }
            }
        }

        return value;
    }
}
