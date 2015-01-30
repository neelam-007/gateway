package com.l7tech.server.bundling;

import com.l7tech.policy.AssertionAccess;
import org.jetbrains.annotations.NotNull;

/**
 * An entity container for assertion security zones.
 */
public class AssertionAccessContainer extends EntityContainer<AssertionAccess> {
    /**
     * Create a new entity container for an assertion security zones.
     *
     * @param assertionAccess The assertion security zones to create the container for
     */
    public AssertionAccessContainer(@NotNull final AssertionAccess assertionAccess) {
        super(assertionAccess.getName(), assertionAccess);
    }
}
