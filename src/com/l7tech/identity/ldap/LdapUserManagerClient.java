package com.l7tech.identity.ldap;

import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import java.util.Collection;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 19, 2003
 *
 */
public class LdapUserManagerClient implements UserManager {

    public LdapUserManagerClient(LdapIdentityProviderConfig config) {
        this.config = config;
    }

    public User findByPrimaryKey(String oid) throws FindException {
        return null;
    }

    public void delete(User user) throws DeleteException {
    }

    public long save(User user) throws SaveException {
        return 0;
    }

    public void update(User user) throws UpdateException {
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

    // ************************************************
    // PRIVATES
    // ************************************************
    private LdapIdentityProviderConfig config;
}
