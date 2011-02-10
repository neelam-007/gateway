package com.l7tech.external.assertions.api3scale;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The <code>SamlAuthorizationStatementAssertion</code> assertion describes
 * the SAML Authorization Statement constraints.
 */
public class Api3ScaleTransaction implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private String appId;
    private Map<String, String> metrics = new HashMap<String, String>();
    private String timestamp;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Map<String, String> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, String> metrics) {
        this.metrics = metrics;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Api3ScaleTransaction clone() {
        try {

            Api3ScaleTransaction stmt = (Api3ScaleTransaction) super.clone();
            return stmt;
        }
        catch(CloneNotSupportedException cnse) {
            throw new RuntimeException("Clone error");
        }
    }
}