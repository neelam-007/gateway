package com.l7tech.server.policy;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.assertion.Assertion;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entity manager interface for AssertionAccess instances.
 * <p/>
 * AssertionAccess is a virtual entity that is created on demand for all registered assertion types in the
 * {@link com.l7tech.policy.AssertionRegistry}.  It is possible to add additional metadata (such as
 * security zones) that is persisted to the database.
 */
public interface AssertionAccessManager extends GoidEntityManager<AssertionAccess, EntityHeader> {
    /**
     * Get all AssertionAccess instances, including one per registered assertion.
     * <p/>
     * This will include exactly one AssertionAccess instance per assertion currently registered in the assertion registry.
     * Normally, most of these will be "virtual" AssertionAccess instances, with an OID of -1, that are created on the fly
     * and do not exist in the database.
     * <p/>
     * If an AssertionAccess is customized (to assign a security zone, say) and saved then it will be persisted to the DB
     * and assigned a real OID.
     * <p/>
     * This method will not include AssertionAccess instances in its return value whose corresponding assertion class
     * is not currently registered with the assertion registry, even if there is a row in the assertion_access table for them in the DB.
     *
     * @return all AssertionAccess instances, including exactly one (possibly-virtual) AssertionAccess instnace per currently-registered assertion class.
     * @throws com.l7tech.objectmodel.FindException if there is a problem accessing the database
     */
    Collection<AssertionAccess> findAllRegistered() throws FindException;

    /**
     * Get the AssertionAccess instance for the specified assertion classname.
     * <p/>
     * This method is cached and may return slightly out-of-date info.
     *
     * @param assertion assertion whose AssertionAccess to look up.
     * @return assertion access instance.  Never null.
     */
    AssertionAccess getAssertionAccessCached(@NotNull Assertion assertion);
}
