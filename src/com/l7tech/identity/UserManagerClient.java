package com.l7tech.identity;

import com.l7tech.objectmodel.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * SSM-side implementation of the UserManager.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
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
        } catch (RemoteException e) {
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

    public void delete(User user) throws DeleteException, ObjectNotFoundException {
        delete( user.getUniqueIdentifier() );
    }

    public void delete(String id) throws DeleteException, ObjectNotFoundException {
        try {
            getStub().deleteUser(config.getOid(), id );
        } catch (RemoteException e) {
            throw new DeleteException(e.getMessage(), e);
        }
    }

    public String save(User user) throws SaveException {
        return save(user, null);
    }

    public void update(User user) throws UpdateException, ObjectNotFoundException {
        update(user, null);
    }

    public String save(User user, Set groupHeaders) throws SaveException {
        try {
            return getStub().saveUser(config.getOid(), user, groupHeaders );
        } catch (ObjectModelException e) { // because the stub uses same method for save and update
            throw new SaveException(e.getMessage(), e);
        } catch (RemoteException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    public void update(User user, Set groupHeaders ) throws UpdateException, ObjectNotFoundException {
        try {
            getStub().saveUser(config.getOid(), user, groupHeaders );
        } catch (RemoteException e) {
            throw new UpdateException(e.getMessage(), e);
        } catch (SaveException e) { // because the stub uses same method for save and update
            throw new UpdateException(e.getMessage(), e);
        }
    }

    public Class getImpClass() {
        return User.class;
    }


    public Collection findAllHeaders() throws FindException {
        EntityHeader[] array = null;
        try {
            array = getStub().findAllUsers(config.getOid());
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
            array = getStub().findAllUsersByOffset(config.getOid(), offset, windowSize);
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
}
