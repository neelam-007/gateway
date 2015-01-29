package com.l7tech.server.identity.external;

import com.l7tech.identity.GroupBean;
import com.l7tech.identity.User;
import com.l7tech.identity.external.VirtualPolicyGroup;
import com.l7tech.identity.external.VirtualPolicyUser;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 *
 */
public class PolicyBackedGroupManagerImpl implements PolicyBackedGroupManager {
    @Override
    public VirtualPolicyGroup findByPrimaryKey(String identifier) throws FindException {
        return null;
    }

    @Override
    public void delete(VirtualPolicyGroup group) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(Goid providerOid) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllVirtual(Goid providerOid) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String saveGroup(VirtualPolicyGroup group) throws SaveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(VirtualPolicyGroup group) throws UpdateException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String save(VirtualPolicyGroup group, Set<IdentityHeader> userHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String save(Goid id, VirtualPolicyGroup group, Set<IdentityHeader> userHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(VirtualPolicyGroup group, Set<IdentityHeader> userHeaders) throws UpdateException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<IdentityHeader> search(String searchString) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public Class getImpClass() {
        return VirtualPolicyGroup.class;
    }

    @Override
    public VirtualPolicyGroup reify(GroupBean bean) {
        VirtualPolicyGroup lg = new VirtualPolicyGroup(bean.getProviderId(), bean.getId(), bean.getName());
        lg.setDescription(bean.getDescription());
        return lg;
    }

    @Override
    public boolean isMember(User user, VirtualPolicyGroup group) throws FindException {
        return false;
    }

    @Override
    public void addUsers(VirtualPolicyGroup group, Set<VirtualPolicyUser> users) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeUsers(VirtualPolicyGroup group, Set<VirtualPolicyUser> users) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUser(VirtualPolicyUser user, Set<VirtualPolicyGroup> groups) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeUser(VirtualPolicyUser user, Set<VirtualPolicyGroup> groups) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUser(VirtualPolicyUser user, VirtualPolicyGroup group) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeUser(VirtualPolicyUser user, VirtualPolicyGroup group) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<IdentityHeader> getGroupHeaders(VirtualPolicyUser user) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public Set<IdentityHeader> getGroupHeaders(String userId) throws FindException {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public Set<IdentityHeader> getGroupHeadersForNestedGroup(@NotNull String groupId) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public void setGroupHeaders(VirtualPolicyUser user, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGroupHeaders(String userId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<IdentityHeader> getUserHeaders(VirtualPolicyGroup group) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public Set<IdentityHeader> getUserHeaders(String groupId) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public void setUserHeaders(VirtualPolicyGroup group, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserHeaders(String groupId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException {
        return new EntityHeaderSet<>();
    }

    @Override
    public Collection findAll() throws FindException {
        return Collections.emptyList();
    }

    @Override
    public VirtualPolicyGroup findByName(String groupName) throws FindException {
        return null;
    }
}
