package com.l7tech.objectmodel;

import javax.validation.constraints.NotNull;

/**
 * This is a cusom entity header for a secure password. It allows the entity header to store the password type.
 * Either "PASSWORD" or "PEM_PRIVATE_KEY"
 *
 * @author Victor Kazakov
 */
public class SecurePasswordEntityHeader extends EntityHeader {
    private String passwordType;

    public SecurePasswordEntityHeader(@NotNull Goid goid, EntityType type, String name, String description, String passwordType) {
        super(goid.toString(), type, name, description, null);
        this.passwordType = passwordType;
    }

    public String getPasswordType() {
        return passwordType;
    }

    public void setPasswordType(String passwordType) {
        this.passwordType = passwordType;
    }
}
