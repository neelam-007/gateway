package com.l7tech.identity;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Enum that specifies which type of id provider.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 23, 2003
 *
 */
public class IdentityProviderType implements Serializable {
    private static int id = 1;
    public static final IdentityProviderType INTERNAL =
            new IdentityProviderType( id++, "internal",
                                      "com.l7tech.server.identity.internal.InternalIdentityProviderImpl");

    public static final IdentityProviderType LDAP =
            new IdentityProviderType( id++, "LDAP",
                                      "com.l7tech.server.identity.ldap.LdapIdentityProviderImpl");

    public static final IdentityProviderType FEDERATED =
            new IdentityProviderType( id++, "Federated",
                                      "com.l7tech.server.identity.fed.FederatedIdentityProviderImpl");

    public static final IdentityProviderType BIND_ONLY_LDAP =
            new IdentityProviderType( id++, "Simple LDAP",
                                      "com.l7tech.server.identity.ldap.BindOnlyLdapIdentityProviderImpl");

    public static final IdentityProviderType POLICY_BACKED =
            new IdentityProviderType( id++, "Policy-Backed",
                                      "com.l7tech.server.identity.external.PolicyBackedIdentityProviderImpl");

    public static IdentityProviderType fromVal(int val) {
        switch (val) {
            case 1:
                return INTERNAL;
            case 2:
                return LDAP;
            case 3:
                return FEDERATED;
            case 4:
                return BIND_ONLY_LDAP;
            case 5:
                return POLICY_BACKED;
        }
        throw new IllegalArgumentException("Unknown type id " + val);
    }

    /**
     * Determine if the given provider is of a ype
     * @param prov the provider
     * @param type the provider type
     * @return true if provider is of given type, false otherwise
     */
    public static boolean is(IdentityProvider prov, IdentityProviderType type) {
        if (prov == null || type == null) {
            throw new IllegalArgumentException();
        }

        final IdentityProviderConfig config = prov.getConfig();
        if (config == null) {
            throw new IllegalArgumentException("IdentityProviderConfig cannot be null");
        }
        return type.equals(config.type());
    }

    public int toVal() {
        return type;
    }

    public String description() {
        return description;
    }

    protected IdentityProviderType(int type, String desc, String classname) {
        this.type = type;
        this.description = desc;
        this.classname = classname;
    }

    public String getClassname() {
        return classname;
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
        FEDERATED,
        BIND_ONLY_LDAP,
        POLICY_BACKED
    };

    private static final long serialVersionUID = 5766402770013082083L;

    private final int type;
    private final String description;
    private final String classname;
}
