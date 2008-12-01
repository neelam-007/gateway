package com.l7tech.server.migration;

import com.l7tech.objectmodel.ReadOnlyEntityManager;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SearchableEntityManager;
import com.l7tech.identity.User;

import java.util.Collection;

/**
 *
 */
public class UserEntityManager implements ReadOnlyEntityManager<User, IdentityHeader>, SearchableEntityManager<User, IdentityHeader> {

    @Override
    public Collection<IdentityHeader> findHeaders( final int offset, final int windowSize, final String filter) throws FindException {
        return null;
    }

    @Override
    public User findByPrimaryKey(final long oid) throws FindException {
        return null;
    }

    @Override
    public Collection<IdentityHeader> findAllHeaders() throws FindException {
        return null;
    }

    @Override
    public Collection<IdentityHeader> findAllHeaders(final int offset, final int windowSize) throws FindException {
        return null;
    }

    @Override
    public Collection<User> findAll() throws FindException {
        return null;
    }
}
