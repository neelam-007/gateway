package com.l7tech.security.xml;

public enum TimestampEnum {
    AUTO("Automatic"),
    ADD("Always Include"),
    OMIT("Never Include");

    private String displayName;

    private TimestampEnum(String name) {
        this.displayName = name;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
