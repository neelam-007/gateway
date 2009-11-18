/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

public abstract class WsFederationPassiveTokenAssertion extends Assertion {
    public WsFederationPassiveTokenAssertion() {
    }

    public WsFederationPassiveTokenAssertion(String url, String context, String replyUrl, boolean authenticate) {
        this.ipStsUrl = url;
        this.context = context;
        this.replyUrl = replyUrl;
        this.authenticate = authenticate;
    }

    public WsFederationPassiveTokenAssertion(String url, String context, String replyUrl) {
        this.ipStsUrl = url;
        this.context = context;
        this.replyUrl = replyUrl;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getIpStsUrl() {
        return ipStsUrl;
    }

    public void setIpStsUrl(String ipStsUrl) {
        this.ipStsUrl = ipStsUrl;
    }

    public String getReplyUrl() {
        return replyUrl;
    }

    public void setReplyUrl(String replyUrl) {
        this.replyUrl = replyUrl;
    }

    public boolean isAuthenticate() {
        return authenticate;
    }

    public void setAuthenticate(boolean authenticate) {
        this.authenticate = authenticate;
    }

    @Override
    public boolean isCredentialModifier() {
        return true;
    }

    public void copyFrom(WsFederationPassiveTokenAssertion source) {
        this.setIpStsUrl(source.getIpStsUrl());
        this.setAuthenticate(source.isAuthenticate());
        this.setReplyUrl(source.getReplyUrl());
        this.setContext(source.getContext());
        this.setEnabled(source.isEnabled());
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Use WS-Federation Credential");
        meta.put(AssertionMetadata.DESCRIPTION, "Gateway replaces request's credentials with those obtained using WS-Federation (Passive Request Profile).");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlWithCert16.gif");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "WS-Federation Request Properties");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Edit16.gif");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.EditWsFederationPassiveTokenRequestAction");

        return meta;
    }
    
    //- PRIVATE
    private boolean authenticate;
    private String ipStsUrl;
    private String replyUrl; // reply url, the URL on the service server to POST auth to (to get cookie)
    private String context; // service url, the thing we will be accessing when authorized
}
