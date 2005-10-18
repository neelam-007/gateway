package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.Assertion;

/**
 *  Assertion for WS-Federation using Passive Request Profile.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsFederationPassiveTokenRequest extends Assertion {

    //- PUBLIC

    public WsFederationPassiveTokenRequest() {
    }

    public WsFederationPassiveTokenRequest(String url, String realm, boolean timestamp) {
        this();
        this.ipStsUrl = url;
        this.realm = realm;
        this.timestamp = timestamp;
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

    public boolean isTimestamp() {
        return timestamp;
    }

    public void setTimestamp(boolean timestamp) {
        this.timestamp = timestamp;
    }

    //- PRIVATE

    private String ipStsUrl;
    private String realm;
    private boolean timestamp;
}
