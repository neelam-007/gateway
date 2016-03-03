package com.l7tech.external.assertions.apiportalintegration.server.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author chean22, 3/2/2016
 */
public class ApplicationMag {

    private String scope;
    private String redirectUri;
    // Keys: "master-key", "environment"
    private List<Map<String, String>> masterKeys = new ArrayList<>();

    public ApplicationMag() {
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public List<Map<String, String>> getMasterKeys() {
        return masterKeys;
    }

    public void setMasterKeys(List<Map<String, String>> masterKeys) {
        this.masterKeys = masterKeys;
    }
}
