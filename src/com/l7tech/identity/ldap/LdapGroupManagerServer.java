package com.l7tech.identity.ldap;

import com.l7tech.identity.GroupManager;
import com.l7tech.identity.Group;
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
public class LdapGroupManagerServer implements GroupManager {

    public LdapGroupManagerServer(LdapIdentityProviderConfig config) {
        this.config = config;
    }

    public Group findByPrimaryKey(String oid) throws FindException {
        return null;
    }

    public void delete(Group group) throws DeleteException {
    }

    public long save(Group group) throws SaveException {
        return 0;
    }

    public void update(Group group) throws UpdateException {
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

    // ************************************************
    // PRIVATES
    // ************************************************

    private LdapIdentityProviderConfig config;
}
