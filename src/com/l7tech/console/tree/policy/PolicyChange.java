package com.l7tech.console.tree.policy;

import com.l7tech.console.event.PolicyEvent;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.service.PublishedService;


/**
 * The policy change. Hooks into <code>Advice</code> chain via
 * <code>PolicyChange.proceed()</code> method.
 * 
 * @author <a href="mailto:emarceta@layer7tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class PolicyChange {
    private Assertion policy = null;
    private PolicyEvent event = null;
    private PublishedService service = null;

    /**
     * Construct the policy change that will invoke advices for a given policy
     * change.
     * @param policy the policy that will be changed
     * @param event the policy event describing the change
     * @param service the service this policy belongs to
     */
    public PolicyChange(Assertion policy, PolicyEvent event, PublishedService service) {
        this.policy = policy;
        this.event = event;
        this.service = service;
    }

    /**
     * Gets the policy.
     * 
     * @return the current policy
     */
    public Assertion getPolicy() {
        return this.policy;
    }

    /**
     * Gets service.
     * 
     * @return the service that the policy is changed
     */
    public PublishedService getService() {
        return this.service;
    }

    /**
     * Gets the policy event describing the change.
     * 
     * @return the policy change that will be applied
     */
    public PolicyEvent getEvent() {
        return this.event;
    }

    /**
     * Invokes next advice in chain. If no advices are left,
     * applies the policy change.
     */
    public void proceed() throws PolicyException {
        throw new UnsupportedOperationException();
    }


    public String toString() {
        return this.getClass().getName() + "@" +
          Integer.toHexString(System.identityHashCode(this)) + "[" +
          "service: " + service.getName() + ", " +
          "change: " + event +
          "]";
    }

}
