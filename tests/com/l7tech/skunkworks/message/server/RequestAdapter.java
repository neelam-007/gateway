/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message.server;

import com.l7tech.common.RequestId;
import com.l7tech.identity.User;
import com.l7tech.message.XmlRequest;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.MessageProcessor;

import java.util.logging.Level;

/**
 * @author mike
 */
public class RequestAdapter extends XmlMessageAdapterAdapter implements XmlRequest {
    public RequestAdapter(MessageContext context) {
        super(context, context.getRequest());
        MessageProcessor.setCurrentRequest(this);
    }

    public LoginCredentials getPrincipalCredentials() {
        return context.getCredentials();
    }

    public User getUser() {
        return context.getAuthenticatedUser();
    }

    public void setUser(User user) {
        context.setAuthenticatedUser(user);
    }

    public void setPrincipalCredentials(LoginCredentials pc) {
        context.setCredentials(pc);
    }

    public boolean isAuthenticated() {
        return context.isAuthenticated();
    }

    public boolean isReplyExpected() {
        return context.isReplyExpected();
    }

    public void setAuthenticated(boolean authenticated) {
        context.setAuthenticated(authenticated);
    }

    public RoutingStatus getRoutingStatus() {
        return context.getRoutingStatus();
    }

    public void setRoutingStatus(RoutingStatus status) {
        context.setRoutingStatus(status);
    }

    public RequestId getId() {
        return context.getRequestId();
    }

    public Level getAuditLevel() {
        return context.getAuditLevel();
    }

    public void setAuditLevel(Level auditLevel) {
        context.setAuditLevel(auditLevel);
    }

    public boolean isAuditSaveRequest() {
        return context.isAuditSaveRequest();
    }

    public void setAuditSaveRequest(boolean saveRequest) {
        context.setAuditSaveRequest(saveRequest);
    }

    public boolean isAuditSaveResponse() {
        return context.isAuditSaveResponse();
    }

    public void setAuditSaveResponse(boolean saveResponse) {
        context.setAuditSaveResponse(saveResponse);
    }

}
