package com.l7tech.server.config.systemconfig;

import java.util.ArrayList;
import java.util.List;

/**
 * User: megery
 */
public class RadiusAuthTypeSettings extends AuthTypeSettings {
    String radiusServer;
    String radiusSecret;
    String radiusTimeout;

    public String getRadiusServer() {
        return radiusServer;
    }

    public void setRadiusServer(String radiusServer) {
        this.radiusServer = radiusServer;
    }

    public String getRadiusSecret() {
        return radiusSecret;
    }

    public void setRadiusSecret(String radiusSecret) {
        this.radiusSecret = radiusSecret;
    }

    public String getRadiusTimeout() {
        return radiusTimeout;
    }

    public void setRadiusTimeout(String radiusTimeout) {
        this.radiusTimeout = radiusTimeout;
    }

    @Override
    public List<String> asConfigLines() {
        return asConfigLines(false);
    }

    private List<String> asConfigLines(boolean shouldHidePasswords) {
        List<String> configLines = new ArrayList<String>();
        configLines.add(makeNameValuePair("RADIUS_SRV_HOST",getRadiusServer()));
        configLines.add(makeNameValuePair("RADIUS_SECRET", (shouldHidePasswords?"<HIDDEN>":getRadiusSecret())));
        configLines.add(makeNameValuePair("RADIUS_TIMEOUT", getRadiusTimeout()));
        return configLines;
    }

    public List<String> describe() {
        List<String> descs = new ArrayList<String>();
        descs.add("The following RADIUS configuration will be applied:");
        descs.addAll(asConfigLines(true));
        return descs;
    }
}
