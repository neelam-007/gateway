package com.l7tech.identity.attribute;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.Set;

/**
 * A description of a generic attribute that users and/or credentials might have.
 * (e.g. email address, membership status, account balance...)
 * <p>
 * Mappings of this attribute to fields that might be found in users or groups
 * can be found in the {@link #identityMappings} belonging to this object.
 * <p>
 * Mappings of this attribute to values derived from security tokens can be found in the
 * {@link #securityTokenMappings} belonging to this object.
 */
public class AttributeConfig extends NamedEntityImp {
    private String variableName;
    private String description;
    private Set securityTokenMappings;
    private Set identityMappings;

    /**
     * A short, unique and unambiguous name for this attribute, for use in
     * {@link com.l7tech.policy.variable.ExpandVariables} expressions.  Required.
     * @return the variable name.
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * A short, unique and unambiguous name for this attribute, for use in
     * {@link com.l7tech.policy.variable.ExpandVariables} expressions.  Required.
     * @param variableName the variable name
     */
    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    /**
     * @return optional, human-readable text describing this attribute and its uses.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description optional, human-readable text describing this attribute and its uses.
     */
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
