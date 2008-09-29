package com.l7tech.policy.wsp;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 10-Jun-2008
 * Time: 7:47:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class PolicyConflictException extends InvalidPolicyStreamException {
    private String importedPolicyName;
    private String existingPolicyName;
    private String policyGuid;

    PolicyConflictException() {
    }

    public PolicyConflictException(String s, String importedPolicyName, String existingPolicyName, String policyGuid) {
        super(s);
        this.importedPolicyName = importedPolicyName;
        this.existingPolicyName = existingPolicyName;
        this.policyGuid = policyGuid;
    }

    public PolicyConflictException(Throwable cause, String importedPolicyName, String policyGuid) {
        super();
        initCause(cause);
        this.importedPolicyName = importedPolicyName;
        this.policyGuid = policyGuid;
    }

    PolicyConflictException(String s, Throwable cause, String importedPolicyName, String policyGuid) {
        super(s);
        initCause(cause);
        this.importedPolicyName = importedPolicyName;
        this.policyGuid = policyGuid;
    }

    public String getImportedPolicyName() {
        return importedPolicyName;
    }

    public String getPolicyGuid() {
        return policyGuid;
    }

    public String getExistingPolicyName() {
        return existingPolicyName;
    }
}
