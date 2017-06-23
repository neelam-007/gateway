package com.l7tech.external.assertions.ldapwrite;

/**
 * This class represents the possible LDAP changetype.
 * Created by chaja24 on 3/10/2017.
 */
public enum LdapChangetypeEnum {

    ADD("Add"),
    DELETE("Delete"),
    MODIFY("Modify"),
    MODRDN("Modrdn");

    private final String text;

    LdapChangetypeEnum(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

}
