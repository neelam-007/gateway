package com.l7tech.external.assertions.ldapwrite;

/**
 * This class represents the possible LDAP operations (changetype directive) .
 * Created by chaja24 on 3/10/2017.
 */
public enum LdapOperationsEnum {

    ADD("Add"),
    DELETE("Delete"),
    MODIFY("Modify"),
    MODRDN("Modrdn");

    private final String text;

    LdapOperationsEnum(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

}
