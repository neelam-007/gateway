package com.l7tech.identity;

import com.l7tech.objectmodel.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 */
public class GroupManagerClient extends GroupManagerAdapter {

    public GroupManagerClient(IdentityProviderConfig config) {
        manager = new IdentityManagerClient( config );
        this.config = config;
    }

    public Group findByPrimaryKey(String oid) throws FindException {
        try {
            return manager.getStub().findGroupByPrimaryKey(config.getOid(), oid);
        } catch (RemoteException e) {
            throw new FindException("RemoteException in findByPrimaryKey", e);
        }
    }

    public Group findByName(String name) throws FindException {
        throw new FindException("not implemented in this version of the manager");
    }

    public void delete(Group group) throws DeleteException {
        try {
            if (groupIsAdminGroup(group)) throw new CannotDeleteAdminAccountException();
            // todo, group must be refactored so that it's id is always a string
            manager.getStub().deleteGroup(config.getOid(), Long.toString(group.getOid()));
        } catch (RemoteException e) {
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public long save(Group group) throws SaveException {
        try {
            long res = manager.getStub().saveGroup(config.getOid(), group);
            if (res > 0) group.setOid(res);
            return res;
        } catch (UpdateException e) {
            throw new SaveException(e.getMessage(), e);
        } catch (RemoteException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    public void update(Group group) throws UpdateException {
        try {
            manager.getStub().saveGroup(config.getOid(), group);
        } catch (SaveException e) {
            throw new UpdateException(e.getMessage(), e);
        } catch (RemoteException e) {
            throw new UpdateException(e.getMessage(), e);
        }
    }

    public EntityHeader groupToHeader(Group group) {
        return null;
    }

    public Group headerToGroup(EntityHeader header) throws FindException {
        return null;
    }

    public Collection findAllHeaders() throws FindException {
        EntityHeader[] array = null;
        try {
            array = manager.getStub().findAllGroups(config.getOid());
        } catch (RemoteException e) {
            throw new FindException("RemoteException in findAllHeaders", e);
        }
        Collection output = new java.util.ArrayList();
        for (int i = 0; i < array.length; i++) output.add(array[i]);
        return output;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        EntityHeader[] array = null;
        try {
            array = manager.getStub().findAllGroupsByOffset(config.getOid(), offset, windowSize);
        } catch (RemoteException e) {
            throw new FindException("RemoteException in findAllHeaders", e);
        }
        Collection output = new java.util.ArrayList();
        for (int i = 0; i < array.length; i++) output.add(array[i]);
        return output;
    }

    public Collection findAll() throws FindException {
        Collection headers = findAllHeaders();
        Collection output = new ArrayList();
        Iterator i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        Collection headers = findAllHeaders(offset, windowSize);
        Collection output = new ArrayList();
        Iterator i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private boolean groupIsAdminGroup(Group group) {
        // i actually dont get the group, the console only constructs a new group and sets the oid
        try {
            Group actualGroup = findByPrimaryKey(Long.toString(group.getOid()));
            if ( Group.ADMIN_GROUP_NAME.equals( actualGroup.getName() ) ) return true;
        } catch (FindException e) {
            // it's valid that the group does not exist here
            // todo, use client's error reporting mechanism
            e.printStackTrace(System.err);
            return false;
        }
        return false;
    }

    private IdentityManagerClient manager;
    private IdentityProviderConfig config;
}
