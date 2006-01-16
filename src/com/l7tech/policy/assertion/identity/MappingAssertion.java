package com.l7tech.policy.assertion.identity;

import com.l7tech.common.security.token.SecurityTokenType;

public class MappingAssertion extends IdentityAssertion {
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
}
