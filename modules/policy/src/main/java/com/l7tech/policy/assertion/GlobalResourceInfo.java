package com.l7tech.policy.assertion;

import com.l7tech.policy.AssertionResourceInfo;

/**
 * {@link AssertionResourceInfo} implementation where the document is read from a global resource
 * shared across policies. For example, in the case of Schema Validation, this refers to an entry
 * in the global schemas table.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 30, 2007<br/>
 */
public class GlobalResourceInfo extends AssertionResourceInfo {
    private String id;

    @Override
    public AssertionResourceType getType() {
        return AssertionResourceType.GLOBAL_RESOURCE;
    }

    @Override
    public String[] getUrlRegexes() {
        // The administrator provided this schema, so we'll trust any URLs he included in it, regardless
        // of what they appear to be pointed at.
        return new String[] { ".*" };
    }

    /**
     * In the case of a global resource, this refers to the URI property.
     *
     * @return the id (URI) used to retrieve the resource from global resources.
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id (URI) used to retrieve the resource from global resources.
     *
     * @param id the URI for the global resource.
     */
    public void setId(String id) {
        this.id = id;
    }
}
