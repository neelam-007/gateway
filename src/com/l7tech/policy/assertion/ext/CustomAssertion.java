package com.l7tech.policy.assertion.ext;

import java.io.Serializable;

/**
 * The custom assertions implementations implement this interface
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