package com.l7tech.gateway.common.audit;

/**
*/
public enum AuditType {
    ALL("All"), ADMIN("Admin"), MESSAGE("Message"), SYSTEM("System");
    private final String name;

    AuditType(String name){
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
