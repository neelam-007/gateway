/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message.server;

import com.l7tech.common.RequestId;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.skunkworks.message.Message;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The thing that follows every request through the message processing system
 */
public class MessageContext {
    private static final Logger logger = Logger.getLogger(MessageContext.class.getName());

    private final Message request;
    private final Message response;

    private final RequestId requestId;

    private final List runOnClose = new ArrayList();
    private final Map deferredAssertions = new LinkedHashMap();
    private boolean replyExpected;
    private RoutingStatus routingStatus;
    private LoginCredentials credentials;
    private User authenticatedUser;
    private boolean authenticated;

    private Level auditLevel;
    private boolean auditSaveRequest;
    private boolean auditSaveResponse;
    private List assertionResults = Collections.EMPTY_LIST;
    private SoapFaultDetail faultDetail = null;
    private boolean isAuthenticationMissing = false;
    private boolean isPolicyViolated = false;

    public MessageContext(Message request, Message response) {
        this.requestId = RequestIdGenerator.next();
        if (request == null || response == null) throw new NullPointerException();
        this.request = request;
        this.response = response;
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

    public LoginCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(LoginCredentials credentials) {
        this.credentials = credentials;
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

    public synchronized void runOnClose( Runnable runMe ) {
        runOnClose.add( runMe );
    }

    public Message getRequest() {
        return request;
    }

    public Message getResponse() {
        return response;
    }

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
            try {
                response.close();
            } catch (Exception e) {
                logger.log(Level.INFO, "Caught exception closing response", e);
            } finally {
                try {
                    request.close();
                } catch (Exception e) {
                    logger.log(Level.INFO, "Caught exception closing request", e);
                }
            }
        }

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
    }

    public boolean isPolicyViolated() {
        return isPolicyViolated;
    }

    public void setPolicyViolated(boolean policyViolated) {
        isPolicyViolated = policyViolated;
    }
}
