package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.AssertionAccessManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 */
public class AssertionAccessManagerStub  extends EntityManagerStub<AssertionAccess, EntityHeader> implements AssertionAccessManager {

    public AssertionAccessManagerStub(AssertionAccess... assertionAccessIn) {
        super(assertionAccessIn);
    }

    List<String> registeredAssertions = new ArrayList<String>();

    public void setRegisteredAssertions(String... registeredAssertions){
        this.registeredAssertions = Arrays.asList(registeredAssertions);
    }

    @Override
    public Collection<AssertionAccess> findAllRegistered() throws FindException {
        Collection<AssertionAccess> ret = new ArrayList<AssertionAccess>();

        for (String assName : registeredAssertions) {
            AssertionAccess aa = findByUniqueName(assName);
            if (aa != null) {
                // Use persisted AA
                ret.add(aa);
            } else {
                // Use virtual AA
                ret.add(new AssertionAccess(assName));
            }
        }

        return ret;
    }

    @Override
    public AssertionAccess getAssertionAccessCached(@NotNull Assertion assertion) {
        return AssertionAccess.forAssertion(assertion);
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return AssertionAccess.class;
    }
}
