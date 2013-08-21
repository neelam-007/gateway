package com.l7tech.server;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.AssertionAccessManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entity manager for AssertionAccess entities.
 */
public class AssertionAccessManagerImpl extends HibernateEntityManager<AssertionAccess, EntityHeader> implements AssertionAccessManager {
    private static final Logger logger = Logger.getLogger(AssertionAccessManagerImpl.class.getName());
    private static final long CACHE_TIME = 3000L;

    @Inject
    private AssertionRegistry assertionRegistry;

    private final AtomicReference<Pair<Long, Map<String,AssertionAccess>>> assertionAccessCache = new AtomicReference<>();

    @Override
    public Class<? extends Entity> getImpClass() {
        return AssertionAccess.class;
    }

    @Override
    @Transactional(readOnly=true)
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

    @Override
    @Transactional(readOnly=true)
    public AssertionAccess getAssertionAccessCached(@NotNull Assertion assertion) {
        Pair<Long, Map<String, AssertionAccess>> cache = assertionAccessCache.get();
        final long now = System.currentTimeMillis();
        if (cache == null || cache.left == null || (now - cache.left) > CACHE_TIME) {
            cache = null;
        }

        if (cache == null) {
            // Rebuild cache
            cache = new Pair<Long, Map<String, AssertionAccess>>(now, new HashMap<String,AssertionAccess>());
            try {
                Collection<AssertionAccess> aas = findAllRegistered();
                for (AssertionAccess aa : aas) {
                    cache.right.put(aa.getName(), aa);
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to look up assertion access: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
            assertionAccessCache.set(cache);
        }

        AssertionAccess access = cache.right.get(assertion.getClass().getName());
        return access != null ? access : AssertionAccess.forAssertion(assertion);
    }

    private Map<String, AssertionAccess> getPersistedMapByAssertionClassName() throws FindException {
        Map<String,AssertionAccess> persistedMap = new HashMap<String,AssertionAccess>();
        for (AssertionAccess access : findAll()) {
            persistedMap.put(access.getName(), access);
        }
        return persistedMap;
    }
}
