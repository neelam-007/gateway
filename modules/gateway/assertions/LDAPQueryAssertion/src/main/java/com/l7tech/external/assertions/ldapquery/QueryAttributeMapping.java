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
public class QueryAttributeMapping implements Cloneable, Serializable {
    private String attributeName;
    private String matchingContextVariableName;
    private boolean joinMultivalued = true;
    private boolean multivalued;
    private boolean failMultivalued;

    public QueryAttributeMapping() {
    }

    public QueryAttributeMapping( final String attribute,
                                  final String variable) {
        this.attributeName = attribute;
        this.matchingContextVariableName = variable;
    }

    public QueryAttributeMapping( final String attributeName,
                                  final String matchingContextVariableName,
                                  final boolean joinMultivalued,
                                  final boolean failMultivalued,
                                  final boolean multivalued ) {
        this.attributeName = attributeName;
        this.matchingContextVariableName = matchingContextVariableName;
        this.joinMultivalued = joinMultivalued;
        this.failMultivalued = failMultivalued;
        this.multivalued = multivalued;
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

    /**
     * Indicates whether multiple attribute values are expected.
     *
     * @return false if we should always use the first value and ignore the rest.
     *         true if we should handle multiple values according to {@link #isJoinMultivalued()}.
     */
    public boolean isMultivalued() {
        return multivalued;
    }

    /**
     * Indicates whether multiple attribute values are expected.
     *
     * @param multivalued false if we should always use the first value and ignore the rest.
     *                    true if we should handle multiple values according to {@link #isJoinMultivalued()}.
     */
    public void setMultivalued(boolean multivalued) {
        this.multivalued = multivalued;
    }

    /**
     * Controls behavior when multivalued is enabled.  Ignored unless multivalued is true.
     *
     * @return true if we should join multiple attribute values with a comma.
     *          false if we should set a multivalued context variable.
     */
    public boolean isJoinMultivalued() {
        return joinMultivalued;
    }

    /**
     * Controls behavior when multivalued is enabled.  Ignored unless multivalued is true.
     *
     * @param joinMultivalued true if we should join multiple attribute values with a comma.
     *          false if we should set a multivalued context variable.
     */
    public void setJoinMultivalued(boolean joinMultivalued) {
        this.joinMultivalued = joinMultivalued;
    }

    /**
     * Indicates whether the presence of a  multivalued attribute is an error.
     *
     * <p>This setting is ignored unless multivalued is false.</p>
     *
     * @return True to fail the assertion if the attribute has multiple values.
     */
    public boolean isFailMultivalued() {
        return failMultivalued;
    }

    public void setFailMultivalued( final boolean failMultivalued ) {
        this.failMultivalued = failMultivalued;
    }

    @Override
    public QueryAttributeMapping clone() {
        try {
            return (QueryAttributeMapping) super.clone();
        } catch ( CloneNotSupportedException e ) {
            throw new RuntimeException(e);
        }
    }
}
