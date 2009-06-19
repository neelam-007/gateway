package com.l7tech.message;

/**
 * WS-Security version enum.
 */
public enum WsSecurityVersion {

    WSS10("1.0"),
    WSS11("1.1");

    private String displayName;

    private WsSecurityVersion(String name) {
        this.displayName = name;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
