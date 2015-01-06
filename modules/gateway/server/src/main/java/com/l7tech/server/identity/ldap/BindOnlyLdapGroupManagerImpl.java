package com.l7tech.server.identity.ldap;

import com.l7tech.identity.GroupBean;
import com.l7tech.identity.User;
import com.l7tech.identity.ldap.BindOnlyLdapUser;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * A stub group manager that does not provide any groups, for use with the simple Bind-Only LDAP provider.
 */
@LdapClassLoaderRequired
public class BindOnlyLdapGroupManagerImpl implements BindOnlyLdapGroupManager {
    @Override
    public LdapGroup findByPrimaryKey(String identifier) throws FindException {
        return null;
    }

    @Override
    public void delete(LdapGroup group) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(Goid ipoid) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllVirtual(Goid ipoid) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String saveGroup(LdapGroup group) throws SaveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(LdapGroup group) throws UpdateException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String save(LdapGroup group, Set<IdentityHeader> userHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(LdapGroup group, Set<IdentityHeader> userHeaders) throws UpdateException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<IdentityHeader> search(String searchString) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public Class getImpClass() {
        return LdapGroup.class;
    }

    @Override
    public LdapGroup reify(GroupBean bean) {
        LdapGroup lg = new LdapGroup(bean.getProviderId(), bean.getId(), bean.getName());
        lg.setDescription(bean.getDescription());
        return lg;
    }

    @Override
    public boolean isMember(User user, LdapGroup group) throws FindException {
        return false;
    }

    @Override
    public void addUsers(LdapGroup group, Set<BindOnlyLdapUser> users) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeUsers(LdapGroup group, Set<BindOnlyLdapUser> users) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUser(BindOnlyLdapUser user, Set<LdapGroup> groups) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeUser(BindOnlyLdapUser user, Set<LdapGroup> groups) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUser(BindOnlyLdapUser user, LdapGroup group) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeUser(BindOnlyLdapUser user, LdapGroup group) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<IdentityHeader> getGroupHeaders(BindOnlyLdapUser user) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public Set<IdentityHeader> getGroupHeaders(String userId) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public Set<IdentityHeader> getGroupHeadersForNestedGroup(String groupId) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public void setGroupHeaders(BindOnlyLdapUser user, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGroupHeaders(String userId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<IdentityHeader> getUserHeaders(LdapGroup group) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public Set<IdentityHeader> getUserHeaders(String groupId) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public void setUserHeaders(LdapGroup group, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserHeaders(String groupId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException {
        return new EntityHeaderSet<IdentityHeader>();
    }

    @Override
    public Collection findAll() throws FindException {
        return Collections.emptyList();
    }

    @Override
    public LdapGroup findByName(String groupName) throws FindException {
        return null;
    }
}
