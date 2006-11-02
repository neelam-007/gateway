package com.l7tech.policy.assertion.identity;

import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.policy.variable.VariableMetadata;

public class MappingAssertion extends IdentityAssertion implements SetsVariables, UsesEntities {
    private long attributeConfigOid;

    private String variableName;
    private String retrieveAttributeName;
    private SecurityTokenType tokenType;
    private String searchAttributeName;
    private boolean validForUsers;
    private boolean validForGroups;

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getRetrieveAttributeName() {
        return retrieveAttributeName;
    }

    public void setRetrieveAttributeName(String retrieveAttributeName) {
        this.retrieveAttributeName = retrieveAttributeName;
    }

    public SecurityTokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(SecurityTokenType tokenType) {
        this.tokenType = tokenType;
    }

    public String getSearchAttributeName() {
        return searchAttributeName;
    }

    public void setSearchAttributeName(String searchAttributeName) {
        this.searchAttributeName = searchAttributeName;
    }

    public long getAttributeConfigOid() {
        return attributeConfigOid;
    }

    public void setAttributeConfigOid(long attributeConfigOid) {
        this.attributeConfigOid = attributeConfigOid;
    }

    public boolean isValidForUsers() {
        return validForUsers;
    }

    public void setValidForUsers(boolean validForUsers) {
        this.validForUsers = validForUsers;
    }

    public boolean isValidForGroups() {
        return validForGroups;
    }

    public void setValidForGroups(boolean validForGroups) {
        this.validForGroups = validForGroups;
    }

    public EntityHeader[] getEntitiesUsed() {
        EntityHeader[] headers = super.getEntitiesUsed();
        EntityHeader[] headers2 = new EntityHeader[headers.length + 1];
        System.arraycopy(headers, 0, headers2, 0, headers.length);
        headers2[headers.length] = new EntityHeader(Long.toString(attributeConfigOid), EntityType.ATTRIBUTE_CONFIG, null, null);
        return headers2;
    }

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata(variableName, false, false, null, false)
        };
    }
}
