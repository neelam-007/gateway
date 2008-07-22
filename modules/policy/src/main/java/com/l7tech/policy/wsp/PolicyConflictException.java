package com.l7tech.policy.wsp;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 10-Jun-2008
 * Time: 7:47:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class PolicyConflictException extends InvalidPolicyStreamException {
    private String policyName;
    private String policyGuid;

    PolicyConflictException() {
    }

    public PolicyConflictException(String s, String policyName, String policyGuid) {
        super(s);
        this.policyName = policyName;
        this.policyGuid = policyGuid;
    }

    public PolicyConflictException(Throwable cause, String policyName, String policyGuid) {
        super();
        initCause(cause);
        this.policyName = policyName;
        this.policyGuid = policyGuid;
    }

    PolicyConflictException(String s, Throwable cause, String policyName, String policyGuid) {
        super(s);
        initCause(cause);
        this.policyName = policyName;
        this.policyGuid = policyGuid;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getPolicyGuid() {
        return policyGuid;
    }
}
