package com.l7tech.adminws.identity;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 */
public class UserManagerClient extends IdentityManagerClient implements UserManager {

    public UserManagerClient(IdentityProviderConfig config) {
        super(config);
    }

    public User findByPrimaryKey(String usrId) throws FindException {
        try {
            return getStub().findUserByPrimaryKey(config.getOid(), usrId);
        } catch (java.rmi.RemoteException e) {
            throw new FindException("RemoteException in findUserByPrimaryKey", e);
        }
    }

    public User findByLogin(String login) throws FindException {
        Collection users = findAll();
        Iterator i = users.iterator();
        while (i.hasNext()) {
            User usr = (User)i.next();
            if (usr.getLogin().equals(login)) return usr;
        }
        return null;
    }

    public void delete(User user) throws DeleteException {
        try {
            if (userIsAdministrator(user)) throw new CannotDeleteAdminAccountException();
            // todo, user must be refactored so that it's id is always a string
            getStub().deleteUser(config.getOid(), Long.toString(user.getOid()));
        } catch (java.rmi.RemoteException e) {
            throw new DeleteException(e.getMessage(), e);
        } catch (FindException e) {
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public long save(User user) throws SaveException {
        try {
            long res = getStub().saveUser(config.getOid(), user);
            if (res > 0) user.setOid(res);
            return res;
        } catch (java.rmi.RemoteException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    public void update(User user) throws UpdateException {
        try {
            getStub().saveUser(config.getOid(), user);
        } catch (java.rmi.RemoteException e) {
            throw new UpdateException(e.getMessage(), e);
        }
    }

    public EntityHeader userToHeader(User user) {
        return null;
    }

    public User headerToUser(EntityHeader header) {
        return null;
    }

    public Collection findAllHeaders() throws FindException {
        com.l7tech.objectmodel.EntityHeader[] array = null;
        try {
            array = getStub().findAllUsers(config.getOid());
        } catch (java.rmi.RemoteException e) {
            throw new FindException("RemoteException in findAllHeaders", e);
        }
        Collection output = new java.util.ArrayList();
        for (int i = 0; i < array.length; i++) output.add(array[i]);
        return output;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        com.l7tech.objectmodel.EntityHeader[] array = null;
        try {
            array = getStub().findAllUsersByOffset(config.getOid(), offset, windowSize);
        } catch (java.rmi.RemoteException e) {
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
    private boolean userIsAdministrator(User user) throws FindException {
        if (user.getOid() < 1) return false;
        String compareCriterion = Long.toString(user.getOid());

        // get the group manager
        GroupManagerClient groupManager = new GroupManagerClient(config);
        // get the admingroup
        Collection groupHeaders = groupManager.findAllHeaders();
        Iterator i = groupHeaders.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            if (header.getName() != null && header.getName().equals(Group.ADMIN_GROUP_NAME)) {
                Group adminGroup = groupManager.findByPrimaryKey(header.getStrId());
                // get the member headers
                Collection adminUsers = adminGroup.getMembers();
                Iterator i2 = adminUsers.iterator();
                // see if user is there
                while (i2.hasNext()) {
                    User member = (User)i2.next();
                    String memberId = Long.toString(member.getOid());
                    if (memberId != null && memberId.equals(compareCriterion)) return true;
                }
            }
        }
        return false;
    }
}
