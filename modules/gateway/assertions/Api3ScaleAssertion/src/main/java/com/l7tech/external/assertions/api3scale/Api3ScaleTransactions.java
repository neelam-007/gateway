package com.l7tech.external.assertions.api3scale;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * User: wlui
 */

public class Api3ScaleTransactions implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private String appId = DEFAULT_APP_ID;
    private Map<String, String> metrics = new  HashMap<String, String>();
    private String timestamp;
    private static final String DEFAULT_APP_ID = "${request.http.header.app_id}";


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

    public Api3ScaleTransactions clone() {
        try {

            Api3ScaleTransactions stmt = (Api3ScaleTransactions) super.clone();
            return stmt;
        }
        catch(CloneNotSupportedException cnse) {
            throw new RuntimeException("Clone error");
        }
    }
}
