package com.l7tech.identity;

import java.io.Serializable;
import java.io.ObjectStreamException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 23, 2003
 *
 */
public class IdentityProviderType implements Serializable {
    private static int id = 1;
    public static final IdentityProviderType INTERNAL = new IdentityProviderType(id++, "internal");
    public static final IdentityProviderType LDAP = new IdentityProviderType(id++, "LDAP");
    public static final IdentityProviderType MSAD = new IdentityProviderType(id++, "MS Active Directory");

    public static IdentityProviderType fromVal(int val) {
        switch (val) {
            case 1:
                return INTERNAL;
            case 2:
                return LDAP;
            case 3:
                return MSAD;
        }
        throw new IllegalArgumentException("Unknown type id " + val);
    }

    public int toVal() {
        return type;
    }

    public String description() {
        return description;
    }

    public boolean isLdapLike() {
        if ( this == LDAP || this == MSAD ) return true;
        return false;
    }

    protected IdentityProviderType(int type, String desc) {
        this.type = type;
        this.description = desc;
    }

    /**
     * Resolves instances being deserialized to the predefined constants
     *
     * @return the object reference of the newly created object after it is
     *         deserialized.
     * @exception ObjectStreamException
     */
    private Object readResolve() throws ObjectStreamException {
        return VALUES[type - 1];
    }

    private static final
    IdentityProviderType[] VALUES = {
        INTERNAL,
        LDAP,
        MSAD
    };
    private final int type;
    private final String description;


}
