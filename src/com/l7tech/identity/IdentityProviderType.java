package com.l7tech.identity;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 23, 2003
 *
 */
public class IdentityProviderType {

    public static final IdentityProviderType INTERNAL = new IdentityProviderType(1);
    public static final IdentityProviderType LDAP = new IdentityProviderType(2);
    public static final IdentityProviderType UNDEFINED = new IdentityProviderType(-1);

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

    protected IdentityProviderType(int type) {
        this.type = type;
    }

    private int type;
}
