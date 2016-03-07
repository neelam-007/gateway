package com.l7tech.security.xml;

public enum TimestampSignatureEnum {
    AUTO("Automatic"),
    ADD("Always Sign"),
    OMIT("Never Sign");

    private String displayName;

    private TimestampSignatureEnum(String name) {
        this.displayName = name;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
