package com.l7tech.server.identity.cert;

import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.*;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import sun.security.x509.X500Name;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.*;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Hibernate implementation of the ClientCertManager.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Oct 23, 2003<br/>
 * $Id$
 */
public class ClientCertManagerImp implements ClientCertManager {

    public ClientCertManagerImp() {
        manager = PersistenceManager.getInstance();
        if (!(manager instanceof HibernatePersistenceManager)) {
            throw new IllegalStateException("Can't instantiate a " + getClass().getName() +
                        "without first initializing a HibernatePersistenceManager!");
        }
    }

    public boolean userCanGenCert(User user) {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("userCanGenCert for " + user.getLogin());
        CertEntryRow userData = getFromTable(user);
        // if this user has no data at all, then he is allowed to generate a cert
        if (userData == null) return true;
        // if user has a cert he is only allowed is his counter is below 10
        if (userData.getCert() != null) {
            if (userData.getResetCounter() >= 10) return false;
            else return true;
        } else return true;
    }

    public void recordNewUserCert(User user, Certificate cert) throws UpdateException {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("recordNewUserCert for " + user.getLogin());

        // check that the cert's subject matches the user's login
        try {
            X500Name x500name = new X500Name(((X509Certificate)(cert)).getSubjectX500Principal().getName());
            String login = user.getLogin();
            if (login == null || login.length() == 0) {
                logger.log(Level.INFO, "User " + user.getName() + " has no login. Will save cert anyway.");
            } else if (!x500name.getCommonName().equals(login)) {
                logger.log(Level.SEVERE, "User " + user.getLogin() +
                                         " tried to csr for subject other than self (" +
                                         x500name.getCommonName() + ")");
                throw new UpdateException("Attempt to create cert for identity " + x500name.getCommonName() +
                                          " with login" + user.getLogin());
            } else {
                logger.finest("Cert's subject matches authenticated user's login (" + user.getLogin() + ")");
            }
        } catch (IOException e) {
            throw new UpdateException("could not verify the cert subject", e);
        }

        // check if operation is permitted
        if (!userCanGenCert(user)) {
            String msg = "this user is currently not allowed to generate a new cert: " + user.getLogin();
            logger.info(msg);
            throw new UpdateException(msg);
        }

        // prepare data
        CertEntryRow userData = getFromTable(user);
        // this could be new entry
        boolean newentry = false;
        if (userData == null) {
            userData = new CertEntryRow();
            userData.setProvider(user.getProviderId());
            userData.setUserId(user.getUniqueIdentifier());
            userData.setLogin(user.getLogin());
            userData.setResetCounter(0);
            newentry = true;
        }
        userData.setResetCounter(userData.getResetCounter()+1);

        try {
            String encodedcert = HexUtils.encodeBase64(cert.getEncoded());
            userData.setCert(encodedcert);
        } catch (CertificateEncodingException e) {
            String msg = "Certificate encoding exception recording cert";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }

        // record new data
        try {
            HibernatePersistenceContext pc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            Session session = pc.getSession();
            if (newentry) {
                Object res = session.save(userData);
                logger.finest("saving cert entry " + res);
            } else {
                logger.finest("updating cert entry");
                session.update(userData);
            }
        } catch (HibernateException e) {
            String msg = "Hibernate exception recording cert";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        } catch (SQLException e) {
            String msg = "SQL exception recording cert";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }
    }

    public Certificate getUserCert(User user) throws FindException {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("getUserCert for " + user.getLogin());
        CertEntryRow userData = getFromTable(user);
        if (userData != null) {
            String dbcert = userData.getCert();
            if (dbcert == null) {
                logger.finest("no cert recorded for user " + user.getLogin());
                return null;
            }

            try {
                byte[] certbytes = HexUtils.decodeBase64(dbcert);
                Certificate cert = CertificateFactory.getInstance("X.509").
                                    generateCertificate(new ByteArrayInputStream(certbytes));
                return cert;
            } catch (CertificateException e) {
                String msg = "Error in CertificateFactory.getInstance";
                logger.log(Level.WARNING, msg, e);
                throw new FindException(msg, e);
            } catch (IOException e) {
                String msg = "Error in base64decoder.decodeBuffer";
                logger.log(Level.WARNING, msg, e);
                throw new FindException(msg, e);
            }
        } else {
            logger.finest("no entry for " + user.getLogin() + " in provider " + user.getProviderId());
        }
        return null;
    }

    public void revokeUserCert(User user) throws UpdateException {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("revokeUserCert for " + user.getLogin());
        CertEntryRow currentdata = getFromTable(user);
        if (currentdata != null) {
            currentdata.setCert(null);
            currentdata.setResetCounter(0);
            try {
                HibernatePersistenceContext pc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
                Session session = pc.getSession();
                // update existing data
                session.update(currentdata);
            } catch (HibernateException e) {
                String msg = "Hibernate exception revoking cert";
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            } catch (SQLException e) {
                String msg = "SQL exception revoking cert";
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            }
        } else {
            logger.fine("there was no existing cert for " + user.getLogin());
        }
    }

    public void forbidCertReset(User user) throws UpdateException {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("forbidCertReset for " + user.getLogin());
        CertEntryRow currentdata = getFromTable(user);
        if (currentdata != null) {
            currentdata.setResetCounter(10);
            try {
                HibernatePersistenceContext pc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
                Session session = pc.getSession();
                // update existing data
                session.update(currentdata);
            } catch (HibernateException e) {
                String msg = "Hibernate exception updating cert info";
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            } catch (SQLException e) {
                String msg = "SQL exception updating cert info";
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            }
        } else {
            // this should not happen
            String msg = "there was no existing cert for " + user.getLogin();
            logger.warning(msg);
            throw new UpdateException(msg);
        }
    }

    /**
     * retrieves the table data for a specific user
     * @return the data in the table or null if no data exist for this user
     */
    private CertEntryRow getFromTable(User user) {
        String query = "from " + TABLE_NAME + " in class " + CertEntryRow.class.getName() +
                       " where " + TABLE_NAME + "." + PROVIDER_COLUMN + " = ?" +
                       " and " + TABLE_NAME + "." + USER_ID + " = ?";
        List hibResults = null;
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            Query q = context.getSession().createQuery(query);
            q.setLong(0, user.getProviderId());
            q.setString(1, user.getUniqueIdentifier());
            hibResults = q.list();
        } catch (SQLException e) {
            hibResults = Collections.EMPTY_LIST;
            logger.log(Level.WARNING, "hibernate error finding cert entry for " + user.getLogin(), e);
        }  catch (HibernateException e) {
            hibResults = Collections.EMPTY_LIST;
            logger.log(Level.WARNING, "hibernate error finding cert entry for " + user.getLogin(), e);
        } /*finally {
            context.close();
        }*/

        switch (hibResults.size()) {
            case 0:
                return null;
            case 1:
                return (CertEntryRow)hibResults.get(0);
            default:
                logger.warning("this should not happen. more than one entry found" +
                                          "for login: " + user.getLogin());
                return null;
        }
    }

    protected PersistenceManager manager = null;
    protected final Logger logger = Logger.getLogger(getClass().getName());

    private static final String TABLE_NAME = "client_cert";
    private static final String PROVIDER_COLUMN = "provider";
    private static final String USER_ID = "userId";
}
