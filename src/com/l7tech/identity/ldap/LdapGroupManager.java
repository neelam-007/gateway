package com.l7tech.identity.ldap;

import com.l7tech.identity.GroupManager;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.logging.LogManager;

import java.util.Set;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * GroupManager for ldap identity provider.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 * $Id$<br/>
 *
 */
public class LdapGroupManager implements GroupManager {
    public LdapGroupManager(LdapIdentityProviderConfig cfg, LdapIdentityProvider daddy) {
        this.cfg = cfg;
        this.parent = daddy;
    }

    public Group findByPrimaryKey(String identifier) throws FindException {
        return null;
    }

    public Group findByName(String name) throws FindException {
        return null;
    }

    public void delete(Group group) throws DeleteException, ObjectNotFoundException {
    }

    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
    }

    public String save(Group group) throws SaveException {
        return null;
    }

    public void update(Group group) throws UpdateException, ObjectNotFoundException {
    }

    public String save(Group group, Set userHeaders) throws SaveException {
        return null;
    }

    public void update(Group group, Set userHeaders) throws UpdateException, ObjectNotFoundException {
    }

    public boolean isMember(User user, Group group) throws FindException {
        return false;
    }

    public void addUsers(Group group, Set users) throws FindException, UpdateException {
    }

    public void removeUsers(Group group, Set users) throws FindException, UpdateException {
    }

    public void addUser(User user, Set groups) throws FindException, UpdateException {
    }

    public void removeUser(User user, Set groups) throws FindException, UpdateException {
    }

    public void addUser(User user, Group group) throws FindException, UpdateException {
    }

    public void removeUser(User user, Group group) throws FindException, UpdateException {
    }

    public Set getGroupHeaders(User user) throws FindException {
        return null;
    }

    public Set getGroupHeaders(String userId) throws FindException {
        return null;
    }

    public void setGroupHeaders(User user, Set groupHeaders) throws FindException, UpdateException {
    }

    public void setGroupHeaders(String userId, Set groupHeaders) throws FindException, UpdateException {
    }

    public Set getUserHeaders(Group group) throws FindException {
        return null;
    }

    public Set getUserHeaders(String groupId) throws FindException {
        return null;
    }

    public void setUserHeaders(Group group, Set groupHeaders) throws FindException, UpdateException {
    }

    public void setUserHeaders(String groupId, Set groupHeaders) throws FindException, UpdateException {
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

    private LdapIdentityProviderConfig cfg;
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    private LdapIdentityProvider parent;
}
