package com.l7tech.server.ems.user;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.identity.internal.InternalUser;

import java.util.Collection;
import java.util.Collections;

/**
 * Mock implementation for EmsAccountManager
 */
public class MockEsmAccountManager implements EsmAccountManager {
    @Override
    public int getUserCount() throws FindException {
        return 0;
    }

    @Override
    public Collection<InternalUser> getUserPage(int start, int count, String property, boolean ascending) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public InternalUser findByLogin(String login) throws FindException {
        return null;
    }

    @Override
    public InternalUser findByPrimaryKey(String identifier) throws FindException {
        return null;
    }

    @Override
    public String save(InternalUser user) throws SaveException {
        throw new SaveException("Mock does not support save");
    }

    @Override
    public void update(InternalUser user) throws UpdateException {
        throw new UpdateException("Mock does not support update");
    }

    @Override
    public void delete(String user) throws DeleteException {
        throw new DeleteException("Mock does not support delete");
    }
}
