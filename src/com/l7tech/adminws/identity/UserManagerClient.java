package com.l7tech.adminws.identity;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.RemoteException;

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

    public void delete(User user) throws DeleteException {
        try {
            if (userIsCurrentlyAdministrator(Long.toString(user.getOid()))) {
                throw new CannotDeleteAdminAccountException();
            }
            // todo, user must be refactored so that it's id is always a string
            getStub().deleteUser(config.getOid(), Long.toString(user.getOid()));
        } catch (RemoteException e) {
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
        } catch (RemoteException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    public void update(User user) throws UpdateException {
        try {
            getStub().saveUser(config.getOid(), user);
        } catch (RemoteException e) {
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

    // fla note, this should be moved to some sort of cert manager
    public Certificate retrieveUserCert(String oid) throws FindException {
        try {
            String encodedCert = getStub().getUserCert(config.getOid(), oid);
            // this means no cert was created
            if (encodedCert == null) return null;
            sun.misc.BASE64Decoder base64decoder = new sun.misc.BASE64Decoder();
            byte[] certbytes = base64decoder.decodeBuffer(encodedCert);
            return CertificateFactory.getInstance("X.509").
                    generateCertificate(new ByteArrayInputStream(certbytes));
        } catch (RemoteException e) {
            throw new FindException("RemoteException in retrieveUserCert", e);
        } catch (IOException e) {
            throw new FindException("RemoteException in retrieveUserCert", e);
        } catch (CertificateException e) {
            throw new FindException("RemoteException in retrieveUserCert", e);
        }
    }

    // fla note, this should be moved to some sort of cert manager
    public void revokeCert(String oid) throws UpdateException {
        try {
            getStub().revokeCert(config.getOid(), oid);
        } catch (RemoteException e) {
            throw new UpdateException("RemoteException in revokeCert", e);
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private boolean userIsCurrentlyAdministrator(String userId) throws FindException {
        // i actually dont get the user, the console only construct a new user and sets the oid
        User user = findByPrimaryKey(userId);
        Set groupMembershipHeaders = user.getGroupHeaders();
        for (Iterator i = groupMembershipHeaders.iterator(); i.hasNext();) {
            EntityHeader header = (EntityHeader)i.next();
            if (header.getName() != null && header.getName().equals(Group.ADMIN_GROUP_NAME)) {
                return true;
            }
        }
        return false;
    }
}
