/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.message;

import com.l7tech.common.RequestId;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.ProcessingContext;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.service.PublishedService;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds message processing state needed by policy enforcement server (SSG) message processor and policy assertions.
 * TODO write some farking javadoc
 */
public class PolicyEnforcementContext extends ProcessingContext {
    private static final Logger logger = Logger.getLogger(PolicyEnforcementContext.class.getName());

    private final RequestId requestId;
    private final Map deferredAssertions = new LinkedHashMap();
    private boolean replyExpected;
    private RoutingStatus routingStatus = RoutingStatus.NONE;
    private User authenticatedUser;
    private boolean authenticated;
    private Level auditLevel;
    private boolean auditSaveRequest;
    private boolean auditSaveResponse;
    private List assertionResults = Collections.EMPTY_LIST;
    private SoapFaultDetail faultDetail = null;
    private boolean isAuthenticationMissing = false;
    private boolean isRequestPolicyViolated = false;
    private PublishedService service;
    private final Vector updatedCookies = new Vector();
    private AuditContext auditContext = null;
    private final Map vars = new HashMap();

    public PolicyEnforcementContext(Message request, Message response) {
        super(request, response);
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

    public AuditContext getAuditContext() {
        return auditContext;
    }

    public void setAuditContext(AuditContext auditContext) {
        this.auditContext = auditContext;
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

    /**
     * Check if some authentication credentials that were expected in the request were not found.
     * This implies {@link #setRequestPolicyViolated}, as well.
     */
    public boolean isAuthenticationMissing() {
        return isAuthenticationMissing;
    }

    /**
     * Report that some authentication credentials that were expected in the request were not found.
     * This implies requestPolicyViolated, as well.
     */
    public void setAuthenticationMissing() {
        isAuthenticationMissing = true;
        setRequestPolicyViolated();
    }

    /**
     * Check if a policy violation was detected while processing the request.
     * If the policy processing turns out to fail, a Policy-URL: should be sent back
     * to the requestor.
     */
    public boolean isRequestPolicyViolated() {
        return isRequestPolicyViolated;
    }

    /**
     * Note that a policy violation was detected while processing the request.
     * If the policy processing turns out to fail, a Policy-URL: should be sent back
     * to the requestor.
     */
    public void setRequestPolicyViolated() {
        isRequestPolicyViolated = true;
    }

    public void setService(PublishedService service) {
        this.service = service;
    }

    public PublishedService getService() {
        return service;
    }

    public Vector getUpdatedCookies() {
        return updatedCookies;
    }

    public void setVariable(String name, Object value) {
        vars.put(name, value);
    }

    public Object getVariable(String name) {
        return vars.get(name);
    }
}
