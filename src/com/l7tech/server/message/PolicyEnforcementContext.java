/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.message;

import com.l7tech.common.RequestId;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.ProcessingContext;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.service.PublishedService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds message processing state needed by policy enforcement server (SSG) message processor and policy assertions.
 * TODO write some farking javadoc
 */
public class PolicyEnforcementContext extends ProcessingContext {
    private static final Logger logger = Logger.getLogger(PolicyEnforcementContext.class.getName());

    private final HttpServletRequest hrequest;
    private final HttpServletResponse hresponse;
    private final RequestId requestId;
    private final Map deferredAssertions = new LinkedHashMap();
    private boolean replyExpected;
    private RoutingStatus routingStatus;
    private User authenticatedUser;
    private boolean authenticated;
    private Level auditLevel;
    private boolean auditSaveRequest;
    private boolean auditSaveResponse;
    private List assertionResults = Collections.EMPTY_LIST;
    private SoapFaultDetail faultDetail = null;
    private boolean isAuthenticationMissing = false;
    private boolean isPolicyViolated = false;
    private final List runOnClose = new ArrayList();
    private PublishedService service;
    private final Vector updatedCookies = new Vector();

    public PolicyEnforcementContext(Message request, Message response) {
        this(request, response, null, null);
    }

    public PolicyEnforcementContext(Message request, Message response,
                                    HttpServletRequest hrequest, HttpServletResponse hresponse) {
        super(request, response);
        this.hrequest = hrequest;
        this.hresponse = hresponse;
        this.requestId = RequestIdGenerator.next();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(User authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public RoutingStatus getRoutingStatus() {
        return routingStatus;
    }

    public void setRoutingStatus(RoutingStatus routingStatus) {
        this.routingStatus = routingStatus;
    }

    public boolean isReplyExpected() {
        return replyExpected;
    }

    public void setReplyExpected(boolean replyExpected) {
        this.replyExpected = replyExpected;
    }

    public Level getAuditLevel() {
        return auditLevel;
    }

    public void setAuditLevel(Level auditLevel) {
        this.auditLevel = auditLevel;
    }

    public RequestId getRequestId() {
        return requestId;
    }

    public boolean isAuditSaveRequest() {
        return auditSaveRequest;
    }

    public void setAuditSaveRequest(boolean auditSaveRequest) {
        this.auditSaveRequest = auditSaveRequest;
    }

    public boolean isAuditSaveResponse() {
        return auditSaveResponse;
    }

    public void setAuditSaveResponse(boolean auditSaveResponse) {
        this.auditSaveResponse = auditSaveResponse;
    }

    public Collection getDeferredAssertions() {
        return deferredAssertions.values();
    }

    public void addDeferredAssertion(ServerAssertion owner, ServerAssertion decoration) {
        deferredAssertions.put(owner, decoration);
    }

    public void removeDeferredAssertion(ServerAssertion owner) {
        deferredAssertions.remove(owner);
    }

    public void addResult(AssertionResult result) {
        if (assertionResults == Collections.EMPTY_LIST)
            assertionResults = new ArrayList();
        assertionResults.add(result);
    }

    public Iterator results() {
        return assertionResults.iterator();
    }

    public SoapFaultDetail getFaultDetail() {
        return faultDetail;
    }

    public void setFaultDetail(SoapFaultDetail faultDetail) {
        this.faultDetail = faultDetail;
    }

    public List getAssertionResults() {
        return assertionResults;
    }

    public void setAssertionResults(List assertionResults) {
        this.assertionResults = assertionResults;
    }

    public boolean isAuthenticationMissing() {
        return isAuthenticationMissing;
    }

    public void setAuthenticationMissing(boolean authenticationMissing) {
        isAuthenticationMissing = authenticationMissing;
        if ( isAuthenticationMissing ) isPolicyViolated = true;
    }

    public boolean isPolicyViolated() {
        return isPolicyViolated;
    }

    public void setPolicyViolated(boolean policyViolated) {
        isPolicyViolated = policyViolated;
    }

    public synchronized void runOnClose( Runnable runMe ) {
        runOnClose.add( runMe );
    }

    /** Runs all {@link runOnClose} {@link Runnable}s. */
    public void close() {
        Runnable runMe;
        Iterator i = runOnClose.iterator();
        try {
            while ( i.hasNext() ) {
                runMe = (Runnable)i.next();
                try {
                    runMe.run();
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Cleanup runnable threw exception", t);
                }
                i.remove();
            }
        } finally {
            super.close();
        }
    }

    public void setService(PublishedService service) {
        this.service = service;
    }

    public PublishedService getService() {
        return service;
    }

    /**
     * If the corresponding request arrived via an HTTP servlet, this stores the servlet request.
     * This is here instead of in the HttpRequestKnob because it is only useful to the SSG.
     *
     * @return the HttpServletRequest that led to this policy enforcement context, or null if it was not a servlet.
     */
    public HttpServletRequest getHttpServletRequest() {
        return hrequest;
    }

    /**
     * If the corresponding request arrived via an HTTP servlet, this stores the servlet response.
     * This is here instead of in the HttpRequestKnob because it is only useful to the SSG.
     *
     * @return the HttpServletResponse waiting for the response to this request, or null if it didn't come in via a servlet.
     */
    public HttpServletResponse getHttpServletResponse() {
        return hresponse;
    }

    public Vector getUpdatedCookies() {
        return updatedCookies;
    }
}
