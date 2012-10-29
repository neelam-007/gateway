package com.l7tech.server.policy.assertion.credential;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;
import com.l7tech.xml.xpath.XpathVariableContext;
import com.l7tech.xml.xpath.XpathVariableFinderVariableContext;
import org.jaxen.FunctionContext;
import org.jaxen.JaxenException;
import org.jaxen.XPathFunctionContext;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author alex
 */
public class ServerXpathCredentialSource extends AbstractServerAssertion<XpathCredentialSource> {
    private static final FunctionContext XPATH_FUNCTIONS = new XPathFunctionContext(false);
    private final DOMXPath loginXpath, passwordXpath;
    private final boolean requiresTargetDocument;

    public ServerXpathCredentialSource(XpathCredentialSource assertion) {
        super(assertion);

        boolean loginUsesTargetDoc = true;
        XpathExpression loginExpr = null;
        try {
            loginExpr = assertion.getXpathExpression();
            if (loginExpr == null) {
                logger.warning("Login XPath is null; assertion is non-functional");
                loginXpath = null;
            } else {
                String expr = loginExpr.getExpression();
                if (!XpathUtil.usesTargetDocument(expr, loginExpr.getXpathVersion()))
                    loginUsesTargetDoc = false;
                loginXpath = new DOMXPath(expr);
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

        boolean passwordUsesTargetDoc = true;
        XpathExpression passwordExpr = null;
        try {
            passwordExpr = assertion.getPasswordExpression();
            if (passwordExpr == null) {
                logger.warning("Password XPath is null; assertion is non-functional");
                passwordXpath = null;
            } else {
                String expr = passwordExpr.getExpression();
                if (!XpathUtil.usesTargetDocument(expr, passwordExpr.getXpathVersion()))
                    passwordUsesTargetDoc = false;
                passwordXpath = new DOMXPath(expr);
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

        this.requiresTargetDocument = loginUsesTargetDoc || passwordUsesTargetDoc;
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
        } catch (AssertionStatusException e) {
            return e.getAssertionStatus();
        } catch (Exception e) {
            throw new PolicyAssertionException(assertion, e);
        }
    }

    private AssertionStatus doCheckRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Message targetMessage = context.getRequest();

        Document requestDoc = getTargetDocument(targetMessage);

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
            logAndAudit(AssertionMessages.XPATHCREDENTIAL_LOGIN_XPATH_FAILED, null, ExceptionUtils.getDebugException(e));
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
            logAndAudit(AssertionMessages.XPATHCREDENTIAL_PASS_XPATH_FAILED, null, e);
            return AssertionStatus.FAILED;
        }

        // Password is allowed to be zero-length, although no one should ever do so
        if (login == null || login.length() == 0 || pass == null) {
            return AssertionStatus.AUTH_REQUIRED;
        }

        LoginCredentials creds = LoginCredentials.makeLoginCredentials(
                new UsernamePasswordSecurityToken(SecurityTokenType.XPATH_CREDENTIALS,
                        login,
                        pass.toCharArray()),
                XpathCredentialSource.class);

        context.getAuthenticationContext(targetMessage).addCredentials( creds );

        return AssertionStatus.NONE;
    }

    private Document getTargetDocument(Message targetMessage) throws IOException {
        final boolean modifiesTargetDocument = assertion.isRemoveLoginElement() || assertion.isRemovePasswordElement();
        Document ret = null;
        Throwable parseException = null;

        if (targetMessage.isXml()) {
            XmlKnob xmlKnob = targetMessage.getKnob(XmlKnob.class);
            if (xmlKnob != null) {
                try {
                    if (modifiesTargetDocument) {
                        ret = xmlKnob.getDocumentWritable();
                    } else {
                        ret = xmlKnob.getDocumentReadOnly();
                    }
                } catch (SAXException e) {
                    parseException = e;
                    // FALLTHROUGH with null doc
                }
            }
        }

        if (ret == null) {
            if (requiresTargetDocument) {
                logAndAudit(AssertionMessages.XPATHCREDENTIAL_REQUEST_NOT_XML, null, ExceptionUtils.getDebugException(parseException));
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }

            // If neither expression actually cares about the target document, go ahead anyway.  (Bug #7224, Bug #9883)
            ret = XmlUtil.createEmptyDocument();
        }

        return ret;
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
            logAndAudit(notFoundMsg);
            return null;
        }

        List result = xpath.selectNodes(requestDoc);
        if (result == null || result.size() == 0) {
            logAndAudit(notFoundMsg);
            return null;
        }

        if (result.size() > 1) {
            logAndAudit(multiMsg);
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
                                logAndAudit(multiMsg);
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
                logAndAudit(notElementMsg);
            } else {
                Node parent = foundNode.getParentNode();
                if (parent.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element)parent;
                    el.getParentNode().removeChild(el);
                } else {
                    logAndAudit(notElementMsg);
                }
            }
        }

        return value;
    }
}
