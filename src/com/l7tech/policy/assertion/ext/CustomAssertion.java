package com.l7tech.policy.assertion.ext;

import java.io.Serializable;

/**
 * The custom assertions implementations implement this interface and
 * provide the bean style properties (get/set) tyhat configure the
 * custom assertion.
 * The implementation must offer Java Bean style get/set operations.
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