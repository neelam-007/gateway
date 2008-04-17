package com.l7tech.external.assertions.ldapquery;

import java.io.Serializable;

/**
 * Defines pairing between context variable and an ldap attribute name.
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 6, 2007<br/>
 */
public class QueryAttributeMapping implements Serializable {
    private String attributeName;
    private String matchingContextVariableName;
    private boolean multivalued;

    public QueryAttributeMapping(String attribute, String variable) {
        this.attributeName = attribute;
        this.matchingContextVariableName = variable;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getMatchingContextVariableName() {
        return matchingContextVariableName;
    }

    public void setMatchingContextVariableName(String matchingContextVariableName) {
        this.matchingContextVariableName = matchingContextVariableName;
    }

    public boolean isMultivalued() {
        return multivalued;
    }

    public void setMultivalued(boolean multivalued) {
        this.multivalued = multivalued;
    }
}
