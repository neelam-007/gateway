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
public class WsFederationPassiveTokenRequest extends Assertion {

    //- PUBLIC

    public WsFederationPassiveTokenRequest() {
    }

    public WsFederationPassiveTokenRequest(String url, String realm, boolean authenticate, String replyUrl, String contextUrl, boolean timestamp) {
        this();
        this.ipStsUrl = url;
        this.realm = realm;
        this.authenticate = authenticate;
        this.replyUrl = replyUrl;
        this.contextUrl = contextUrl;
        this.timestamp = timestamp;
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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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

    public String getContext() {
        return contextUrl;
    }

    public void setContextUrl(String contextUrl) {
        this.contextUrl = contextUrl;
    }

    public boolean isTimestamp() {
        return timestamp;
    }

    public void setTimestamp(boolean timestamp) {
        this.timestamp = timestamp;
    }

    public void copyFrom(WsFederationPassiveTokenRequest source) {
        this.setIpStsUrl(source.getIpStsUrl());
        this.setRealm(source.getRealm());
        this.setAuthenticate(source.isAuthenticate());
        this.setReplyUrl(source.getReplyUrl());
        this.setContextUrl(source.getContext());
        this.setTimestamp(source.isTimestamp());
    }

    public void copyFrom(WsFederationPassiveTokenExchange source) {
        this.setIpStsUrl(source.getIpStsUrl());
        this.setAuthenticate(source.isAuthenticate());
        this.setReplyUrl(source.getReplyUrl());
        this.setContextUrl(source.getContext());
    }

    //- PRIVATE

    private String ipStsUrl;
    private String realm;
    private boolean authenticate;
    private String replyUrl; // reply url, the URL on the service server to POST auth to (to get cookie)
    private String contextUrl; // service url, the thing we will be accessing when authorized
    private boolean timestamp;
}
