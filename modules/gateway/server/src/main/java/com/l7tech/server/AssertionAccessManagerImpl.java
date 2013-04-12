package com.l7tech.server;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.AssertionAccessManager;

import javax.inject.Inject;
import java.util.*;

/**
 * Entity manager for AssertionAccess entities.
 */
public class AssertionAccessManagerImpl extends HibernateEntityManager<AssertionAccess, EntityHeader> implements AssertionAccessManager {

    @Inject
    private AssertionRegistry assertionRegistry;

    @Override
    public Class<? extends Entity> getImpClass() {
        return AssertionAccess.class;
    }

    @Override
    public Collection<AssertionAccess> findAllRegistered() throws FindException {
        Collection<AssertionAccess> ret = new ArrayList<AssertionAccess>();

        Map<String, AssertionAccess> persistedMap = getPersistedMapByAssertionClassName();

        Set<Assertion> registeredAssertions = assertionRegistry.getAssertions();
        for (Assertion ass : registeredAssertions) {
            final String assname = ass.getClass().getName();
            AssertionAccess aa = persistedMap.get(assname);
            if (aa != null) {
                // Use persisted AA
                ret.add(aa);
            } else {
                // Use virtual AA
                ret.add(AssertionAccess.forAssertion(ass));
            }
        }

        return ret;
    }

    private Map<String, AssertionAccess> getPersistedMapByAssertionClassName() throws FindException {
        Map<String,AssertionAccess> persistedMap = new HashMap<String,AssertionAccess>();
        for (AssertionAccess access : findAll()) {
            persistedMap.put(access.getName(), access);
        }
        return persistedMap;
    }
}
