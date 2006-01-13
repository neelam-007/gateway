package com.l7tech.policy.assertion.ext;

import java.io.Serializable;

/**
 * Holds the configuration for a particular custom assertion instance inside a policy tree.
 * <p/>
 * Every custom assertion has two major parts: the CustomAssertion bean used to hold its policy configuration,
 * and the {@link ServiceInvocation} instance that performs the actual runtime work inside the SecureSpan Gateway.
 * <p/>
 * All custom assertion implementations must implement this interface, and
 * provide bean style properties (get/set) that configure the
 * custom assertion instance.
 * <p/>
 * If no custom editor GUI is provided, the SecureSpan Manager will generate a simple property editor GUI
 * based on the get/set fields present in this CustomAssertion bean.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public interface CustomAssertion extends Serializable {
    /**
     * @return the assertion name
     */
    String getName();
}