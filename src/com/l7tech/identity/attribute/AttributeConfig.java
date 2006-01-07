package com.l7tech.identity.attribute;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.Set;

/**
 * A description of a generic attribute that users and/or credentials might have.
 * (e.g. email address, membership status, account balance...)
 * <p>
 * Mappings of this attribute to fields that might be found in users or groups
 * can be found in the {@link #identityMappings} linked from this record.
 * <p>
 * Mappings of this attribute to values derived from security tokens can be found in the
 * {@link #securityTokenMappings} linked from this record.
 */
public class AttributeConfig extends NamedEntityImp {
    private String description;
    private Set securityTokenMappings;
    private Set identityMappings;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set getSecurityTokenMappings() {
        return securityTokenMappings;
    }

    public void setSecurityTokenMappings(Set securityTokenMappings) {
        this.securityTokenMappings = securityTokenMappings;
    }

    public Set getIdentityMappings() {
        return identityMappings;
    }

    public void setIdentityMappings(Set identityMappings) {
        this.identityMappings = identityMappings;
    }
}
