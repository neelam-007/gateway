/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.logging.LogManager;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.service.PublishedService;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.security.auth.Subject;
import javax.security.auth.login.FailedLoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>ServerCustomAssertionHolder</code> class represents the server side of the
 * custom assertion. It implenets the <code>ServerAssertion</code> interface, and it
 * preapres the environment for executing the custom assertion <code>ServiceInvocation<code>.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ServerCustomAssertionHolder implements ServerAssertion {
    protected Logger logger = LogManager.getInstance().getSystemLogger();

    final protected CustomAssertion customAssertion;
    final protected CustomAssertionDescriptor descriptor;

    public ServerCustomAssertionHolder(CustomAssertionHolder ca) {
        if (ca == null || ca.getCustomAssertion() == null) {
            throw new IllegalArgumentException();
        }
        customAssertion = ca.getCustomAssertion(); // ignore hoder
        descriptor = CustomAssertions.getDescriptor(customAssertion.getClass());
        checkDescriptor();
    }

    public AssertionStatus checkRequest(final Request request, final Response response) throws IOException, PolicyAssertionException {
        logger.entering(ServerCustomAssertionHolder.class.getName(), "checkRequest");
        try {
            PublishedService service = (PublishedService)request.getParameter(Request.PARAM_SERVICE);
            if (!checkDescriptor()) {
                throw new PolicyAssertionException("Custom assertion is misconfigured, service '" + service.getName() + "'");
            }
            Subject subject = new Subject();
            String principalName = getPrincipalName(request);
            logger.fine("Service '" + service.getName() + "\n" +
              "custom assertion ' " + descriptor.getServerAssertion().getName() + "\n" +
              "principal '" + principalName + "'");
            if (principalName != null) {
                subject.getPrincipals().add(new CustomAssertionPrincipal(principalName));
            }
            subject.setReadOnly();
            Subject.doAs(subject, new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    Class sa = descriptor.getServerAssertion();
                    ServiceInvocation si = (ServiceInvocation)sa.newInstance();
                    if (isPostRouting(request)) {
                        si.onResponse(new CustomServiceResponse((XmlResponse)response));
                    } else {
                        si.onRequest(new CustomServiceRequest((XmlRequest)request));
                    }
                    return null;
                }
            });
            return AssertionStatus.NONE;
        } catch (PrivilegedActionException e) {
            if (ExceptionUtils.causedBy(e, FailedLoginException.class)) {
                logger.log(Level.WARNING, "Authentication (login)", e);
                return AssertionStatus.AUTH_FAILED;
            } else if (ExceptionUtils.causedBy(e, GeneralSecurityException.class)) {
                logger.log(Level.WARNING, "Authorization (access control) ", e);
                return AssertionStatus.UNAUTHORIZED;
            }
            logger.log(Level.SEVERE, "Error invking the custom assertion", e);
            return AssertionStatus.FAILED;
        } finally {
            logger.exiting(ServerCustomAssertionHolder.class.getName(), "checkRequest");
        }
    }

    private String getPrincipalName(Request request) {
        String principal = "";
        LoginCredentials principalCredentials = request.getPrincipalCredentials();
        if (principalCredentials != null) {
            principal = principalCredentials.getLogin();
        }
        return principal;
    }

    private boolean isPostRouting(Request request) {
        return RoutingStatus.ROUTED.equals(request.getRoutingStatus()) || RoutingStatus.ATTEMPTED.equals(request.getRoutingStatus());
    }

    /**
     * check if descriptor is valid, log if invalid
     */
    private boolean checkDescriptor() {
        if (descriptor == null || descriptor.getServerAssertion() == null) {
            logger.warning("Invalid custom assertion descriptor detected for '" + customAssertion.getClass() + "'\n" +
              " this policy element is misconfigured and will cause the policy to fail.");
            return false;
        }
        return true;
    }

    private class CustomServiceResponse implements ServiceResponse {
        private final XmlResponse response;
        private final Map context = new HashMap();
        private final TransportMetadata transportMetadata;
        private final Document document;

        public CustomServiceResponse(XmlResponse response) throws IOException, SAXException {
            this.response = response;
            this.transportMetadata = response.getTransportMetadata();
            this.document = (Document)response.getDocument().cloneNode(true);
            if (transportMetadata instanceof HttpTransportMetadata) {
                HttpServletRequest req = ((HttpTransportMetadata)transportMetadata).getRequest();
                HttpServletResponse res = ((HttpTransportMetadata)transportMetadata).getResponse();
                context.put("httpRequest", req);
                context.put("httpResponse", res);
            }
        }

        public Document getDocument() {
            return document;
        }

        public void stDocument(Document document) {
            response.setDocument(document);
            try {
                final String docstring = XmlUtil.documentToString(document);
                logger.fine("Set the response document to" + docstring);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error stringyfing document", e);
            }

        }

        public Map getContext() {
            return context;
        }
    }

    private class CustomServiceRequest implements ServiceRequest {
        private final XmlRequest request;
        private final Map context = new HashMap();
        private final TransportMetadata transportMetadata;
        private final Document document;

        public CustomServiceRequest(XmlRequest request)
          throws IOException, SAXException {
            this.request = request;
            this.transportMetadata = request.getTransportMetadata();
            this.document = (Document)request.getDocument().cloneNode(true);
            if (transportMetadata instanceof HttpTransportMetadata) {
                HttpServletRequest req = ((HttpTransportMetadata)transportMetadata).getRequest();
                HttpServletResponse res = ((HttpTransportMetadata)transportMetadata).getResponse();
                context.put("httpRequest", req);
                context.put("httpResponse", res);
            }
        }

        public Document getDocument() {
            return document;
        }

        public void stDocument(Document document) {
            request.setDocument(document);
            try {
                final String docstring = XmlUtil.documentToString(document);
                logger.fine("Set the request document to" + docstring);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error stringyfing document", e);
            }
        }

        public Map getContext() {
            return context;
        }
    }

    private static class CustomAssertionPrincipal implements Principal {
        private String name;

        public CustomAssertionPrincipal(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
