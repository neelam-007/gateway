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
 * Date: Jun 13, 2003
 *
 */
public class LdapUserManagerServer implements UserManager {

    public LdapUserManagerServer(LdapIdentityProviderConfig config) {
        this.config = config;
    }

    /**
     * This is actually not supported in this UserManager since ldap entries are not reffered to by a long id
     * but rather by a DN.
     */
    public User findByPrimaryKey(String oid) throws FindException {
        return null;
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public void delete(User user) throws DeleteException {
        throw new DeleteException("Not supported in LdapUserManagerServer");
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public long save(User user) throws SaveException {
        throw new SaveException("Not supported in LdapUserManagerServer");
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public void update(User user) throws UpdateException {
        throw new UpdateException("Not supported in LdapUserManagerServer");
    }

    public void setIdentityProviderOid(long oid) {
        // what is that for?
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
