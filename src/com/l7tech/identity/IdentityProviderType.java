package com.l7tech.identity;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 23, 2003
 *
 */
public class IdentityProviderType {

    public static final IdentityProviderType INTERNAL = new IdentityProviderType(1, "internal");
    public static final IdentityProviderType LDAP = new IdentityProviderType(2, "LDAP");
    public static final IdentityProviderType UNDEFINED = new IdentityProviderType(-1, "undefined");

    public static IdentityProviderType fromVal(int val) {
        switch (val) {
            case 1:
                return INTERNAL;
            case 2:
                return LDAP;
            default:
                return UNDEFINED;
        }
    }

    public int toVal() {
        return type;
    }

    public String description() {
        return description;
    }


    protected IdentityProviderType(int type, String desc) {
        this.type = type;
        this.description = desc;
    }

    private int type;
    private String description;
}
