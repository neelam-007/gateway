/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.service.PublishedService;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.security.auth.Subject;
import javax.security.auth.login.FailedLoginException;
import java.io.IOException;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>ServerCustomAssertionHolder</code> class represents the server side of the
 * custom assertion. It implemets the <code>ServerAssertion</code> interface, and it
 * prepares the environment for executing the custom assertion <code>ServiceInvocation<code>.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ServerCustomAssertionHolder implements ServerAssertion {
    protected final Logger logger = Logger.getLogger(getClass().getName());

    final protected CustomAssertion customAssertion;
    final private boolean isAuthAssertion;
    private CustomAssertionDescriptor descriptor;
    private ServiceInvocation serviceInvocation;

    public ServerCustomAssertionHolder(CustomAssertionHolder ca) {
        if (ca == null || ca.getCustomAssertion() == null) {
            throw new IllegalArgumentException();
        }
        customAssertion = ca.getCustomAssertion(); // ignore hoder
        isAuthAssertion = Category.ACCESS_CONTROL.equals(ca.getCategory());
    }

    private void initialize() throws PolicyAssertionException {
        descriptor = CustomAssertions.getDescriptor(customAssertion.getClass());

        if (!checkDescriptor(descriptor)) {
            throw new PolicyAssertionException("Custom assertion is misconfigured");
        }
        Class sa = descriptor.getServerAssertion();
        try {
            serviceInvocation = (ServiceInvocation)sa.newInstance();
            serviceInvocation.setCustomAssertion(customAssertion);
        } catch (InstantiationException e) {
            throw new PolicyAssertionException("Custom assertion is misconfigured", e);
        } catch (IllegalAccessException e) {
            throw new PolicyAssertionException("Custom assertion is misconfigured", e);
        }
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // Bugzilla #707 - removed the logger.entering()/exiting() as they are just for debugging purpose
        //logger.entering(ServerCustomAssertionHolder.class.getName(), "checkRequest");

        if(serviceInvocation == null) initialize();

        try {
            PublishedService service = context.getService();
            final CustomAssertionDescriptor descriptor = CustomAssertions.getDescriptor(customAssertion.getClass());

            if (!checkDescriptor(descriptor)) {
                throw new PolicyAssertionException("Custom assertion is misconfigured, service '" + service.getName() + "'");
            }
            Subject subject = new Subject();
            LoginCredentials principalCredentials = context.getCredentials();
            if (principalCredentials != null) {
                String principalName = principalCredentials.getLogin();
                logger.fine("Service '" + service.getName() + "\n" +
                  "custom assertion ' " + descriptor.getServerAssertion().getName() + "\n" +
                  "principal '" + principalName + "'");

                if (principalName != null) {
                    subject.getPrincipals().add(new CustomAssertionPrincipal(principalName));
                }
                final char[] credentials = principalCredentials.getCredentials();
                if (credentials != null) {
                    subject.getPrivateCredentials().add(new String(credentials));
                }
            }
            subject.setReadOnly();
            Subject.doAs(subject, new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    if (isPostRouting(context)) {
                        serviceInvocation.onResponse(new CustomServiceResponse(context));
                    } else {
                        serviceInvocation.onRequest(new CustomServiceRequest(context));
                    }
                    return null;
                }
            });
            if (isAuthAssertion)
                context.setAuthenticated(true);
            return AssertionStatus.NONE;
        } catch (PrivilegedActionException e) {
            if (ExceptionUtils.causedBy(e.getException(), FailedLoginException.class)) {
                logger.log(Level.WARNING, "Authentication (login)", e);
                return AssertionStatus.AUTH_FAILED;
            } else if (ExceptionUtils.causedBy(e.getException(), GeneralSecurityException.class)) {
                logger.log(Level.WARNING, "Authorization (access control) failed", e);
                return AssertionStatus.UNAUTHORIZED;
            }
            logger.log(Level.SEVERE, "Failed to invoke the custom assertion", e);
            return AssertionStatus.FAILED;
        } catch (AccessControlException e) {
            logger.log(Level.WARNING, "Authorization (access control) failed", e);
            return AssertionStatus.UNAUTHORIZED;
        } finally {
            //logger.exiting(ServerCustomAssertionHolder.class.getName(), "checkRequest");
        }
    }

    private boolean isPostRouting(PolicyEnforcementContext context) {
        return RoutingStatus.ROUTED.equals(context.getRoutingStatus()) || RoutingStatus.ATTEMPTED.equals(context.getRoutingStatus());
    }

    /**
     * check if descriptor is valid, log if invalid
     */
    private boolean checkDescriptor(CustomAssertionDescriptor descriptor) {
        if (descriptor == null || descriptor.getServerAssertion() == null) {
            logger.warning("Invalid custom assertion descriptor detected for '" + customAssertion.getClass() + "'\n" +
              " this policy element is misconfigured and will cause the policy to fail.");
            return false;
        }
        return true;
    }

    private class CustomServiceResponse implements ServiceResponse {
        private final PolicyEnforcementContext pec;
        private final Map context = new HashMap();
        private final Document document;
        private final SecurityContext securityContext;

        public CustomServiceResponse(PolicyEnforcementContext pec) throws IOException, SAXException {
            this.pec = pec;
            this.document = (Document)pec.getResponse().getXmlKnob().getDocumentReadOnly().cloneNode(true);

            context.put("httpRequest", pec.getHttpServletRequest());
            context.put("httpResponse", pec.getHttpServletResponse());
            securityContext = new SecurityContext() {
                public Subject getSubject() {
                    return Subject.getSubject(AccessController.getContext());
                }

                public boolean isAuthenticated() {
                    return CustomServiceResponse.this.pec.isAuthenticated();
                }

                public void setAuthenticated() throws GeneralSecurityException {
                    throw new GeneralSecurityException("Cannot authenticate in the response");
                }
            };
        }

        public Document getDocument() {
            return document;
        }

        public void setDocument(Document document) {
            XmlKnob respXml = (XmlKnob)pec.getResponse().getKnob(XmlKnob.class);
            if (respXml == null)
                pec.getResponse().initialize(document);
            else
                respXml.setDocument(document);
            try {
                final String docstring = XmlUtil.nodeToString(document);
                logger.fine("Set the response document to" + docstring);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error stringyfing document", e);
            }
        }

        public SecurityContext getSecurityContext() {
            return securityContext;
        }

        public Map getContext() {
            return context;
        }
    }

    private class CustomServiceRequest implements ServiceRequest {
        private final PolicyEnforcementContext pec;
        private final Map context = new HashMap();
        private final Document document;
        private final SecurityContext securityContext;

        public CustomServiceRequest(PolicyEnforcementContext pec)
          throws IOException, SAXException {
            this.pec = pec;
            this.document = (Document)pec.getRequest().getXmlKnob().getDocumentReadOnly().cloneNode(true);
            Vector newCookies = pec.getUpdatedCookies();
            context.put("httpRequest", pec.getHttpServletRequest());
            context.put("httpResponse", pec.getHttpServletResponse());
            context.put("updatedCookies", newCookies);
            securityContext = new SecurityContext() {
                public Subject getSubject() {
                    return Subject.getSubject(AccessController.getContext());
                }

                public boolean isAuthenticated() {
                    return CustomServiceRequest.this.pec.isAuthenticated();
                }

                public void setAuthenticated() throws GeneralSecurityException {
                    if (CustomServiceRequest.this.pec.isAuthenticated()) {
                        throw new GeneralSecurityException("already authenticated");
                    }
                    CustomServiceRequest.this.pec.setAuthenticated(true);
                }
            };
        }

        public Document getDocument() {
            return document;
        }

        public void setDocument(Document document) {
            XmlKnob reqXml = (XmlKnob)pec.getRequest().getKnob(XmlKnob.class);
            if (reqXml == null)
                pec.getRequest().initialize(document);
            else
                reqXml.setDocument(document);
            try {
                final String docstring = XmlUtil.nodeToString(document);
                logger.fine("Set the request document to" + docstring);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error stringyfing document", e);
            }
        }

        public SecurityContext getSecurityContext() {
            return securityContext;
        }

        public Map getContext() {
            return context;
        }
    }
}
