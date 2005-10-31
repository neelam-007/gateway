package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.Assertion;

/**
 *  Assertion for WS-Federation using Passive Request Profile.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsFederationPassiveTokenExchange extends Assertion {

    //- PUBLIC

    public WsFederationPassiveTokenExchange() {
    }

    public WsFederationPassiveTokenExchange(String url, String contextUrl, boolean timestamp) {
        this();
        this.ipStsUrl = url;
        this.context = contextUrl;
        this.timestamp = timestamp;
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

    public boolean isTimestamp() {
        return timestamp;
    }

    public void setTimestamp(boolean timestamp) {
        this.timestamp = timestamp;
    }

    //- PRIVATE

    private String ipStsUrl;
    private String context;
    private boolean authenticate;
    private String replyUrl;
    private boolean timestamp;
}
