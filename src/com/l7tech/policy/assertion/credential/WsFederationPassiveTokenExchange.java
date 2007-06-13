package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

/**
 *  Assertion for WS-Federation using Passive Request Profile.
 *
 * @author $Author$
 * @version $Revision$
 */
@ProcessesRequest
@RequiresSOAP(wss=true)
public class WsFederationPassiveTokenExchange extends Assertion {

    //- PUBLIC

    public WsFederationPassiveTokenExchange() {
    }

    public WsFederationPassiveTokenExchange(String url, String contextUrl, String replyUrl) {
        this();
        this.ipStsUrl = url;
        this.context = contextUrl;
        this.replyUrl = replyUrl;
    }

    public boolean isCredentialModifier() {
        return true;
    }

    public String getIpStsUrl() {
        return ipStsUrl;
    }

    public void setIpStsUrl(String ipStsUrl) {
        this.ipStsUrl = ipStsUrl;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public boolean isAuthenticate() {
        return authenticate;
    }

    public void setAuthenticate(boolean authenticate) {
        this.authenticate = authenticate;
    }

    public String getReplyUrl() {
        return replyUrl;
    }

    public void setReplyUrl(String replyUrl) {
        this.replyUrl = replyUrl;
    }

    public void copyFrom(WsFederationPassiveTokenRequest source) {
        this.setIpStsUrl(source.getIpStsUrl());
        this.setAuthenticate(source.isAuthenticate());
        this.setReplyUrl(source.getReplyUrl());
        this.setContext(source.getContext());
    }

    public void copyFrom(WsFederationPassiveTokenExchange source) {
        this.setIpStsUrl(source.getIpStsUrl());
        this.setAuthenticate(source.isAuthenticate());
        this.setReplyUrl(source.getReplyUrl());
        this.setContext(source.getContext());
    }

    //- PRIVATE

    private String ipStsUrl;
    private String context;
    private boolean authenticate;
    private String replyUrl;
}
