package com.l7tech.identity.internal;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;
import com.l7tech.identity.IdProvConfManagerServer;
import com.l7tech.logging.LogManager;
import com.l7tech.common.util.HexUtils;

import java.sql.*;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.SignatureException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.FileInputStream;

import cirrus.hibernate.HibernateException;

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
    }

    public User findByPrimaryKey(String oid) throws FindException {
        try {
            User out = (User)_manager.findByPrimaryKey( getContext(), getImpClass(), Long.parseLong(oid));
            out.setProviderId(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
            return out;
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        } catch ( NumberFormatException nfe ) {
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
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, err);
                throw new FindException( err );
            }
        } catch ( SQLException se ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
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
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "search for " + searchString + " returns " + users.size() + " users.");
            for (Iterator i = users.iterator(); i.hasNext();) {
                output.add(userToHeader((User)i.next()));
            }
            return output;
        } catch (SQLException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "error searching users with pattern " + searchString, e);
            throw new FindException(e.toString(), e);
        }
    }

    /**
     * determines whether a user is allowed to get a new cert.
     * the rules are: if a cert is already present and had been used -> not allowed
     * if a cert is present and has never been user -> user can reset maximum of ten times
     */
    public boolean userCanResetCert(String oid) throws FindException{
        try {
            // fla note, this will move to a seperate manager once we redesign persistence layer
            String colname = "cert_reset_counter";
            Connection connection = ((HibernatePersistenceContext)PersistenceContext.getCurrent()).getSession().connection();
            Statement statement = connection.createStatement();
            String sqlStr = "SELECT " + colname + " FROM " + getTableName() + " WHERE oid=" + oid;
            ResultSet rs = statement.executeQuery(sqlStr);
            if (!rs.next()) {
                LogManager.getInstance().getSystemLogger().log(Level.WARNING, "cannot retrieve " + colname + " value for user " + oid + ". user deleted?");
                return false; // this should not happen unless user no longer exist
            }
            int res = rs.getInt(colname);
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "cert_reset_counter value for user " + oid + " = " + res);
            if (res < 10) return true;
            else {
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "user " + oid + " not authorized to regen cert.");
                return false;
            }
        } catch (SQLException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "error getting cert_reset_counter value for " + oid, e);
            throw new FindException(e.toString(), e);
        } catch (HibernateException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "error getting cert_reset_counter value for " + oid, e);
            throw new FindException(e.toString(), e);
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
                throw new FindException(e.toString(), e);
            }
        }
    }

    /**
     * records new cert and updates the reset counter accordingly
     */
    public void recordNewCert(String oid, Certificate cert) throws UpdateException {
        try {
            // fla note, this will move to a seperate manager once we redesign persistence layer
            String colname = "cert";
            Connection connection = ((HibernatePersistenceContext)PersistenceContext.getCurrent()).getSession().connection();
            PreparedStatement statement = connection.prepareStatement("UPDATE " + getTableName() + " SET " + colname + "=? WHERE oid=?");
            sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            String encodedcert = encoder.encode(cert.getEncoded());
            //statement.setBinaryStream(1, new ByteArrayInputStream(certbytes), certbytes.length);
            statement.setString(1, encodedcert);
            statement.setString(2, oid);
            statement.executeUpdate();

            // get existing counter value from table
            colname = "cert_reset_counter";
            Statement statement2 = connection.createStatement();
            String sqlStr = "SELECT " + colname + " FROM " + getTableName() + " WHERE oid=" + oid;
            ResultSet rs = statement2.executeQuery(sqlStr);
            if (!rs.next()) throw new UpdateException("cannot get value for " + colname);
            int currentValue = rs.getInt(colname);

            // update it
            statement = connection.prepareStatement("UPDATE " + getTableName() + " SET " + colname + "=? WHERE oid=?");
            statement.setInt(1, currentValue+1);
            statement.setString(2, oid);
            statement.executeUpdate();
        } catch (SQLException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch (HibernateException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch (CertificateEncodingException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
                throw new UpdateException(e.toString(), e);
            }
        }
    }

    /**
     * returns null if no certificate is registered for this user
     */
    public Certificate retrieveUserCert(String oid) throws FindException {
        try {
            // fla note, this will move to a seperate manager once we redesign persistence layer
            String colname = "cert";
            Connection connection = ((HibernatePersistenceContext)PersistenceContext.getCurrent()).getSession().connection();

            Statement statement = connection.createStatement();
            String sqlStr = "SELECT " + colname + " FROM " + getTableName() + " WHERE oid=" + oid;
            ResultSet rs = statement.executeQuery(sqlStr);
            if (!rs.next()) {
                return null;
            }
            String dbcert = rs.getString(colname);
            if (dbcert == null) return null;
            sun.misc.BASE64Decoder base64decoder = new sun.misc.BASE64Decoder();
            byte[] certbytes = base64decoder.decodeBuffer(dbcert);
            Certificate dledcert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certbytes));
            /*
            // Verify that this cert was signed by our current root (this is now done in authentication code)
            javax.naming.Context cntx = new javax.naming.InitialContext();
            String rootCertLoc = (String)(cntx.lookup("java:comp/env/RootCertLocation"));
            InputStream certStream = new FileInputStream(rootCertLoc);
            byte[] rootcacertbytes = HexUtils.slurpStream(certStream, 16384);
            certStream.close();
            ByteArrayInputStream bais = new ByteArrayInputStream(rootcacertbytes);
            java.security.cert.Certificate rootcacert = CertificateFactory.getInstance("X.509").generateCertificate(bais);
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "Verifying db cert against current root cert...");
            try {
                dledcert.verify(rootcacert.getPublicKey());
            } catch (SignatureException e) {
                LogManager.getInstance().getSystemLogger().log(Level.WARNING, "key found in database does not verify against current root ca cert. maybe our root cert changed since this cert was created.", e);
                throw new FindException("key found in database does not verify against current root ca cert " + e.getMessage(), e);
            }
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "Verification OK - db cert is valid.");
            // End of verification
            */
            return dledcert;
        } catch (Exception e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new FindException(e.toString(), e);
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
                throw new FindException(e.toString(), e);
            }
        }
    }

    /**
     * removed cert from db and resets counter
     */
    public void revokeCert(String oid) throws UpdateException{
        try {
            // fla note, this will move to a seperate manager once we redesign persistence layer
            Connection connection = ((HibernatePersistenceContext)PersistenceContext.getCurrent()).getSession().connection();
            Statement statement = connection.createStatement();
            statement.executeUpdate("UPDATE " + getTableName() + " SET cert=NULL, cert_reset_counter=0 WHERE oid=" + oid);
        } catch (SQLException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch (HibernateException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } finally {
            try {
                PersistenceContext.getCurrent().close();
            } catch (SQLException e) {
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
                throw new UpdateException(e.toString(), e);
            }
        }
    }

    public void delete(User user) throws DeleteException {
        try {
            _manager.delete( getContext(), user );
        } catch ( SQLException se ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
            throw new DeleteException( se.toString(), se );
        }
    }

    public long save(User user) throws SaveException {
        try {
            return _manager.save( getContext(), user );
        } catch ( SQLException se ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( User user ) throws UpdateException {
        try {
            _manager.update( getContext(), user );
        } catch ( SQLException se ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
            throw new UpdateException( se.toString(), se );
        }
    }

    public EntityHeader userToHeader(User user) {
        return new EntityHeader(user.getOid(), EntityType.USER, user.getName(), null);
    }

    public User headerToUser(EntityHeader header) {
        try {
            return findByPrimaryKey(header.getStrId());
        } catch (FindException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
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
}
