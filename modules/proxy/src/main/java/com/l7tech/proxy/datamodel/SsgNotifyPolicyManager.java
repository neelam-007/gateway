package com.l7tech.proxy.datamodel;

import java.util.Set;

import com.l7tech.proxy.datamodel.exceptions.PolicyLockedException;

/**
 * PolicyManager that delegates to an underlying manager and notifies the Ssg
 * instance of any policy changes.
 *
 * @author Steve Jones
 */
public class SsgNotifyPolicyManager implements PolicyManager {

    //- PUBLIC

    public SsgNotifyPolicyManager(final Ssg ssg, final PolicyManager policyManager) {
        this.ssg = ssg;

        // ensure no chaining of SsgNotifyPolicyManager
        PolicyManager unwrappedPolicyManager = policyManager;
        while (unwrappedPolicyManager instanceof SsgNotifyPolicyManager) {
            unwrappedPolicyManager = ((SsgNotifyPolicyManager) unwrappedPolicyManager).getPolicyManager();
        }

        this.delegate = unwrappedPolicyManager;
    }

    public PolicyManager getPolicyManager() {
        return delegate;
    }

    public void clearPolicies() {
        delegate.clearPolicies();
    }

    public Policy findMatchingPolicy(PolicyAttachmentKey policyAttachmentKey) {
        return delegate.findMatchingPolicy(policyAttachmentKey);
    }

    public void flushPolicy(PolicyAttachmentKey policyAttachmentKey) {
        delegate.flushPolicy(policyAttachmentKey);
    }

    public Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) {
        return delegate.getPolicy(policyAttachmentKey);
    }

    public Set getPolicyAttachmentKeys() {
        return delegate.getPolicyAttachmentKeys();
    }

    public void setPolicy(PolicyAttachmentKey key, Policy policy) throws PolicyLockedException {
        delegate.setPolicy(key, policy);
        ssg.notifyPolicyUpdate(policy);
    }

    //- PRIVATE

    private final Ssg ssg;
    private final PolicyManager delegate;
}
