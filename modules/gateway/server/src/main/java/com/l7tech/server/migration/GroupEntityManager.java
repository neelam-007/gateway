package com.l7tech.server.migration;

import com.l7tech.identity.Group;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.ReadOnlyEntityManager;
import com.l7tech.objectmodel.SearchableEntityManager;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;

/**
 * 
 */
public class GroupEntityManager implements ReadOnlyEntityManager<Group, IdentityHeader>, SearchableEntityManager<Group, IdentityHeader> {

    @Override
    public Group findByPrimaryKey(final long oid) throws FindException {
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
    public Collection<Group> findAll() throws FindException {
        return null;
    }

    @Override
    public Collection<IdentityHeader> findHeaders(final int offset, final int windowSize, final String filter) throws FindException {
        return null;
    }
}
