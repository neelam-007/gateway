package com.l7tech.identity.internal;

import cirrus.hibernate.HibernateException;
import cirrus.hibernate.Session;
import com.l7tech.identity.IdProvConfManagerServer;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;

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
        HibernatePersistenceContext hpc = null;
        try {
            hpc = hibernateContext();
            Session s = hpc.getSession();
            List results = s.find( getFieldQuery( oid, F_CERTRESETCOUNTER ) );
            Integer i = (Integer)results.get(0);
            int res = i.intValue();

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
            if ( hpc != null ) hpc.close();
        }
    }

    /**
     * records new cert and updates the reset counter accordingly
     */
    public void recordNewCert(String oid, Certificate cert) throws UpdateException {
        HibernatePersistenceContext hpc = null;
        try {
            sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            String encodedcert = encoder.encode(cert.getEncoded());

            hpc = hibernateContext();
            hpc.beginTransaction();
            User u = findByPrimaryKey( oid );
            u.setCert( encodedcert );
            u.setCertResetCounter( u.getCertResetCounter() + 1 );
            hpc.commitTransaction();

            /*
            // fla note, this will move to a seperate manager once we redesign persistence layer
            String colname = "cert";
            Connection connection = ((HibernatePersistenceContext)PersistenceContext.getCurrent()).getSession().connection();
            PreparedStatement statement = connection.prepareStatement("UPDATE " + getTableName() + " SET " + colname + "=? WHERE oid=?");
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
            */
        } catch (SQLException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch (CertificateEncodingException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch ( TransactionException e ) {
            LogManager.getInstance().getSystemLogger().log(Level.WARNING, e.getMessage(), e);
            throw new UpdateException( e.toString(), e );
        } catch ( FindException e ) {
            LogManager.getInstance().getSystemLogger().log(Level.WARNING, e.getMessage(), e);
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
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new FindException(e.toString(), e);
        } finally {
            if ( hpc != null ) hpc.close();
        }
    }

    /**
     * removed cert from db and resets counter
     */
    public void revokeCert(String oid) throws UpdateException{
        PersistenceContext pc = null;
        try {
            pc = getContext();

            pc.beginTransaction();
            User u = findByPrimaryKey( oid );
            u.setCert( null );
            u.setCertResetCounter( 0 );
            pc.commitTransaction();

            /*
            // fla note, this will move to a seperate manager once we redesign persistence layer
            Connection connection = ((HibernatePersistenceContext)PersistenceContext.getCurrent()).getSession().connection();
            Statement statement = connection.createStatement();
            statement.executeUpdate("UPDATE " + getTableName() + " SET cert=NULL, cert_reset_counter=0 WHERE oid=" + oid);
            */
        } catch (SQLException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch ( FindException e ) {
            LogManager.getInstance().getSystemLogger().log(Level.WARNING, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } catch ( TransactionException e ) {
            LogManager.getInstance().getSystemLogger().log(Level.WARNING, e.getMessage(), e);
            throw new UpdateException(e.toString(), e);
        } finally {
            if ( pc != null ) pc.close();
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

    private HibernatePersistenceContext hibernateContext() throws SQLException {
        return (HibernatePersistenceContext)PersistenceContext.getCurrent();
    }

    /**
     * Generates a Hibernate query string for retrieving a single field from a User.
     * @param oid The objectId of the User to query
     * @param getfield the (aliased) name of the field to return
     * @return
     */
    private String getFieldQuery( String oid, String getfield ) {
        String alias = getTableName();
        StringBuffer sqlBuffer = new StringBuffer( "SELECT " );
        sqlBuffer.append( alias );
        sqlBuffer.append( "." );
        sqlBuffer.append( getfield );
        sqlBuffer.append( " FROM " );
        sqlBuffer.append( alias );
        sqlBuffer.append( " in class " );
        sqlBuffer.append( getImpClass().getName() );
        sqlBuffer.append( " WHERE " );
        sqlBuffer.append( alias );
        sqlBuffer.append( "." );
        sqlBuffer.append( F_OID );
        sqlBuffer.append( " = " );
        sqlBuffer.append( oid );
        return sqlBuffer.toString();
    }

    private static final String F_CERTRESETCOUNTER = "certResetCounter";
    private static final String F_OID = "oid";
    public static final String F_CERT = "cert";
}
