package com.l7tech.identity.internal;

import cirrus.hibernate.HibernateException;
import cirrus.hibernate.Session;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdProvConfManagerServer;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;
import com.l7tech.server.SessionManager;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 * @version $Revision$
 *
 */
public class InternalUserManagerServer extends HibernateEntityManager implements UserManager {
    public InternalUserManagerServer() {
        super();
        logger = LogManager.getInstance().getSystemLogger();
    }

    public User findByPrimaryKey(String oid) throws FindException {
        try {
            User out = (User)_manager.findByPrimaryKey( getContext(), getImpClass(), Long.parseLong(oid));
            out.setProviderId(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
            return out;
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
            throw new FindException( se.toString(), se );
        } catch ( NumberFormatException nfe ) {
            logger.log(Level.SEVERE, null, nfe);
            throw new FindException( nfe.toString(), nfe );
        }
    }

    public User findByLogin( String login ) throws FindException {
        try {
            List users = _manager.find( getContext(), "from " + getTableName() + " in class " + getImpClass().getName() + " where " + getTableName() + ".login = ?", login, String.class );
            switch ( users.size() ) {
            case 0:
                return null;
            case 1:
                User u = (User)users.get(0);
                u.setProviderId( IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID );
                return u;
            default:
                String err = "Found more than one user with the login " + login;
                logger.log(Level.SEVERE, err);
                throw new FindException( err );
            }
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
            throw new FindException( se.toString(), se );
        }
    }

    public Collection search(String searchString) throws FindException {
        // replace wildcards to match stuff understood by mysql
        // replace * with % and ? with _
        // note. is this portable?
        searchString = searchString.replace('*', '%');
        searchString = searchString.replace('?', '_');
        try {
            List users = _manager.find(getContext(), "from " + getTableName() + " in class " + getImpClass().getName() + " where " + getTableName() + ".login like ?", searchString, String.class);
            Collection output = new ArrayList();
            logger.info("search for " + searchString + " returns " + users.size() + " users.");
            for (Iterator i = users.iterator(); i.hasNext();) {
                output.add(userToHeader((User)i.next()));
            }
            return output;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error searching users with pattern " + searchString, e);
            throw new FindException(e.toString(), e);
        }
    }

    /**
     * determines whether a user is allowed to get a new cert.
     * the rules are: if a cert is already present and had been used -> not allowed
     * if a cert is present and has never been user -> user can reset maximum of ten times
     */
    public boolean userCanResetCert(String oid) throws FindException{
        HibernatePersistenceContext hpc = null;
        try {
            hpc = hibernateContext();
            Session s = hpc.getSession();
            List results = s.find( getFieldQuery( oid, F_CERTRESETCOUNTER ) );
            Integer i = (Integer)results.get(0);
            int res = i.intValue();

            logger.info("cert_reset_counter value for user " + oid + " = " + res);
            if (res < CERTRESETCOUNTER_MAX) return true;
            else {
                logger.log(Level.SEVERE, "user " + oid + " not authorized to regen cert.");
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error getting cert_reset_counter value for " + oid, e);
            throw new FindException(e.toString(), e);
        } catch (HibernateException e) {
            logger.log(Level.SEVERE, "error getting cert_reset_counter value for " + oid, e);
            throw new FindException(e.toString(), e);
        } finally {
            if ( hpc != null ) hpc.close();
        }
    }

    /**
     * records new cert and updates the reset counter accordingly
     */
    public void recordNewCert(String oid, Certificate cert) throws UpdateException {
        HibernatePersistenceContext hpc = null;
        try {
            // todo, we should not have to get entire user here, just update the necessary column
            sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            String encodedcert = encoder.encode(cert.getEncoded());
            hpc = hibernateContext();
            hpc.beginTransaction();
            User u = findByPrimaryKey( oid );
            u.setCert( encodedcert );
            u.setCertResetCounter( u.getCertResetCounter() + 1 );
            hpc.commitTransaction();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch (CertificateEncodingException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch ( TransactionException e ) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new UpdateException( e.toString(), e );
        } catch ( FindException e ) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new UpdateException( e.toString(), e );
        } finally {
            if ( hpc != null ) hpc.close();
        }
    }

    /**
     * returns null if no certificate is registered for this user
     */
    public Certificate retrieveUserCert(String oid) throws FindException {
        HibernatePersistenceContext hpc = null;
        try {
            hpc = hibernateContext();
            String hql = getFieldQuery( oid, F_CERT );
            Session s = hpc.getSession();
            List results = s.find( hql );
            String dbcert = (String)results.get(0);
            if (dbcert == null) return null;
            sun.misc.BASE64Decoder base64decoder = new sun.misc.BASE64Decoder();
            byte[] certbytes = base64decoder.decodeBuffer(dbcert);
            Certificate dledcert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certbytes));
            return dledcert;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new FindException(e.toString(), e);
        } finally {
            if ( hpc != null ) hpc.close();
        }
    }

    /**
     * records the fact that a user's cert was used successfully. once this is set, a user can no longer
     * regenerate a cert automatically unless the administrator revokes the existing user's cert
     */
    public void setCertWasUsed(String oid) throws UpdateException {
        PersistenceContext pc = null;
        try {
            // todo, we should not have to get entire user here, just update the necessary columns
            pc = getContext();
            pc.beginTransaction();
            User u = findByPrimaryKey( oid );
            u.setCertResetCounter(CERTRESETCOUNTER_MAX);
            pc.commitTransaction();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch ( FindException e ) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch ( TransactionException e ) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } finally {
            if ( pc != null ) pc.close();
        }
    }

    /**
     * removed cert from db and resets counter
     */
    public void revokeCert(String oid) throws UpdateException {
        PersistenceContext pc = null;
        try {
            // todo, we should not have to get entire user here, just update the necessary columns
            pc = getContext();
            pc.beginTransaction();
            User u = findByPrimaryKey( oid );
            // if the user has a cert already, revoke it and revoke the password
            if (u.getCert() != null && u.getCert().length() > 0) {
                logger.info("revoking cert and password for user " + u.getLogin());
                byte[] randomPasswd = new byte[32];
                SessionManager.getInstance().getRand().nextBytes(randomPasswd);
                u.setPassword(new String(randomPasswd));
                u.setCert( null );
                u.setCertResetCounter( 0 );
            }
            pc.commitTransaction();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch ( FindException e ) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch ( TransactionException e ) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } finally {
            if ( pc != null ) pc.close();
        }
    }

    public void delete(User user) throws DeleteException {
        try {
            User originalUser = findByPrimaryKey(Long.toString(user.getOid()));
            if (isLastAdmin(originalUser)) {
                String msg = "An attempt was made to nuke the last standing adminstrator";
                logger.severe(msg);
                throw new DeleteException(msg);
            }
            _manager.delete( getContext(), user );
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
            throw new DeleteException( se.toString(), se );
        } catch ( FindException e ) {
            logger.log(Level.SEVERE, null, e);
            throw new DeleteException( e.toString(), e );
        }
    }

    public long save(User user) throws SaveException {
        try {
            return _manager.save( getContext(), user );
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
            throw new SaveException( se.toString(), se );
        }
    }

    /**
     * checks that passwd was changed. if so, also revokes the existing cert
     * checks if the user is the last standing admin account, throws if so
     * @param user existing user
     */
    public void update( User user ) throws UpdateException {
        try {
            User originalUser = findByPrimaryKey(Long.toString(user.getOid()));
            // here, we should make sure that IF the user is an administrator, he should not
            // delete his membership to the admin group unless there is another exsiting member
            if (isLastAdmin(originalUser)) {
                // was he stupid enough to nuke his admin membership?
                boolean stillAdmin = false;
                Iterator newgrpsiterator = user.getGroups().iterator();
                while (newgrpsiterator.hasNext()) {
                    Group grp = (Group)newgrpsiterator.next();
                    if (Group.ADMIN_GROUP_NAME.equals(grp.getName())) {
                        stillAdmin = true;
                        break;
                    }
                }
                if (!stillAdmin) {
                    logger.severe("An attempt was made to update the last standing administrator with no membership to the admin group!");
                    throw new UpdateException("This update would revoke admin membership on the last standing administrator");
                }
            }
            // checks whether the user changed his password
            String originalPasswd = originalUser.getPassword();
            String newPasswd = user.getPassword();

            if (!originalPasswd.equals(newPasswd)) {
                logger.info("Revoking cert for user " + originalUser.getLogin() + " because he is changing his passwd.");
                // must revoke the cert
                originalUser.setCert( null );
                originalUser.setCertResetCounter( 0 );
                user.setCert( null );
                user.setCertResetCounter( 0 );
            }
            // update user
            originalUser.copyFrom(user);
            // update from existing user
            _manager.update( getContext(), originalUser );
        } catch ( SQLException se ) {
            logger.log(Level.SEVERE, null, se);
            throw new UpdateException( se.toString(), se );
        } catch ( FindException e ) {
            logger.log(Level.SEVERE, null, e);
            throw new UpdateException( e.toString(), e );
        }
    }

    public EntityHeader userToHeader(User user) {
        return new EntityHeader(user.getOid(), EntityType.USER, user.getName(), null);
    }

    public User headerToUser(EntityHeader header) {
        try {
            return findByPrimaryKey(header.getStrId());
        } catch (FindException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public String getTableName() {
        return "internal_user";
    }

    public Class getImpClass() {
        return User.class;
    }

    public Class getInterfaceClass() {
        return User.class;
    }

    private HibernatePersistenceContext hibernateContext() throws SQLException {
        return (HibernatePersistenceContext)PersistenceContext.getCurrent();
    }

    /**
     * check whether this user is the last standing administrator
     * this is used at update time to prevent the last administrator to nuke his admin accout membership
     * @param currentUser
     * @return true is this user is an administrator and no other users are
     */
    private boolean isLastAdmin(User currentUser) {
        Iterator i = currentUser.getGroups().iterator();
        while (i.hasNext()) {
            Group grp = (Group)i.next();
            // is he an administrator?
            if (Group.ADMIN_GROUP_NAME.equals(grp.getName())) {
                // is he the last one ?
                if (grp.getMembers().size() <= 1) return true;
                return false;
            }
        }
        return false;
    }

    private static final String F_CERTRESETCOUNTER = "certResetCounter";
    // this value for the F_CERTRESETCOUNTER column represent the maximum number of retries that
    // a user can regen his cert until the admin must revoke. once a cert is used in the authentication
    // code, the F_CERTRESETCOUNTER value is automatically set to that value so that it cannot be regen once
    // it is used.
    private static final int CERTRESETCOUNTER_MAX = 10;
    public static final String F_CERT = "cert";
    private Logger logger = null;
}
