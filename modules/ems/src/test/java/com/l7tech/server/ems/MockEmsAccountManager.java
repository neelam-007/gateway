package com.l7tech.server.ems;

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
public class MockEmsAccountManager implements EmsAccountManager {
    public int getUserCount() throws FindException {
        return 0;
    }

    public Collection<InternalUser> getUserPage(int start, int count, String property, boolean ascending) throws FindException {
        return Collections.emptyList();
    }

    public InternalUser findByLogin(String login) throws FindException {
        return null;
    }

    public String save(InternalUser user) throws SaveException {
        throw new SaveException("Mock does not support save");
    }

    public void update(InternalUser user) throws UpdateException {
        throw new UpdateException("Mock does not support update");
    }

    public void delete(String user) throws DeleteException {
        throw new DeleteException("Mock does not support delete");
    }
}
