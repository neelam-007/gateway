package com.l7tech.identity.internal.imp;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.SaveException;

import java.util.Collection;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 14, 2003
 *
 */
public class InternalUserManagerClient implements com.l7tech.identity.UserManager {
    public User findByPrimaryKey(long oid) throws FindException {
        return null;
    }

    public void delete(User user) throws DeleteException {
    }

    public long save(User user) throws SaveException {
        return 0;
    }

    public void setIdentityProviderOid(long oid) {
    }

    public Collection findAllHeaders() throws FindException {
        return null;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        return null;
    }

    public Collection findAll() throws FindException {
        return null;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        return null;
    }
}
