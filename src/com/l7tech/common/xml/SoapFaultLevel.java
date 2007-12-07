package com.l7tech.common.xml;

import com.l7tech.policy.variable.Syntax;

import java.io.Serializable;

/**
 * Attached to a PolicyEnforcementContext and overridable through the FaultLevel assertion,
 * such an object tells the SSG what the soap fault returned to a requestor should look like
 * when a policy evaluation fails.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 4, 2006<br/>
 *
 * @see com.l7tech.policy.assertion.FaultLevel
 * @see com.l7tech.server.message.PolicyEnforcementContext
 */
public class SoapFaultLevel implements Serializable {
    public static final int DROP_CONNECTION = 0;
    public static final int TEMPLATE_FAULT = 1;
    public static final int GENERIC_FAULT = 2;
    public static final int MEDIUM_DETAIL_FAULT = 3;
    public static final int FULL_TRACE_FAULT = 4;
    private int level = GENERIC_FAULT;
    private String faultTemplate;
    private boolean includePolicyDownloadURL = true;
    private String[] variablesUsed = new String[0];


    /**
     * @return the level of the fault that should be returned to requestor in case of a policy evaluation failure
     */
    public int getLevel() {
        return level;
    }

    /**
     * set the level of the fault that should be returned to requestor in case of a policy evaluation failure
     * @param level DROP_CONNECTION or TEMPLATE_FAULT or GENERIC_FAULT or MEDIUM_DETAIL_FAULT or FULL_TRACE_FAULT
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * the template that serves as a basis for the fault that should be returned to requestor in case of a
     * policy evaluation failure. it may contain context variables that need to be resolved before the fault
     * is actually produced
     */
    public String getFaultTemplate() {
        return faultTemplate;
    }

    /**
     * the template that serves as a basis for the fault that should be returned to requestor in case of a
     * policy evaluation failure. it may contain context variables that need to be resolved before the fault
     * is actually produced
     */
    public void setFaultTemplate(String faultTemplate) {
        this.faultTemplate = faultTemplate;
        if (faultTemplate != null) {
            variablesUsed = Syntax.getReferencedNames(faultTemplate);
        } else {
            variablesUsed = new String[0];
        }
    }

    /**
     * whether or not the returned soap fault should include a special http header whose value is the url to download
     * the policy which was violated.
     */
    public boolean isIncludePolicyDownloadURL() {
        return includePolicyDownloadURL;
    }

    /**
     * whether or not the returned soap fault should include a special http header whose value is the url to download
     * the policy which was violated.
     */
    public void setIncludePolicyDownloadURL(boolean includePolicyDownloadURL) {
        this.includePolicyDownloadURL = includePolicyDownloadURL;
    }

    public String toString() {
        return getClass().getName() + ". Level: " + level +
                                      ", Include URL: " + includePolicyDownloadURL +
                                      ", Template: " + faultTemplate;
    }

    public String[] getVariablesUsed() {
        return variablesUsed;
    }
}
