package com.l7tech.server;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.AssertionAccessManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 */
public class AssertionAccessManagerStub  extends EntityManagerStub<AssertionAccess, EntityHeader> implements AssertionAccessManager {

    public AssertionAccessManagerStub(AssertionAccess... assertionAccessIn) {
        super(assertionAccessIn);
    }

    @Override
    public Collection<AssertionAccess> findAllRegistered() throws FindException {
        return findAll();
    }

    @Override
    public AssertionAccess getAssertionAccessCached(@NotNull Assertion assertion) {
        return AssertionAccess.forAssertion(assertion);
    }
}
