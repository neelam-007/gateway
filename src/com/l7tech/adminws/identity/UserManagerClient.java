package com.l7tech.adminws.identity;

import com.l7tech.identity.UserManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
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
        //todo
        return null;
    }

    public void delete(User user) throws DeleteException {
        throw new DeleteException("Not supported in UserManagerClient");
    }

    public long save(User user) throws SaveException {
        throw new SaveException("Not supported in UserManagerClient");
    }

    public void update(User user) throws UpdateException {
        throw new UpdateException("Not supported in UserManagerClient");
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
}
