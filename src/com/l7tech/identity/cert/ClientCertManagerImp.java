package com.l7tech.identity.cert;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.logging.LogManager;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.Collections;
import java.sql.SQLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Oct 23, 2003
 * Time: 10:53:57 AM
 * $Id$
 *
 * Hibernate implementation of the ClientCertManager
 */
public class ClientCertManagerImp implements ClientCertManager {

    public ClientCertManagerImp() {
        manager = PersistenceManager.getInstance();
        if (!(manager instanceof HibernatePersistenceManager)) {
            throw new IllegalStateException("Can't instantiate a " + getClass().getName() +
                        "without first initializing a HibernatePersistenceManager!");
        }
        logger = LogManager.getInstance().getSystemLogger();
    }

    public boolean userCanGenCert(User user) {
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
        logger.finest("recordNewUserCert for " + user.getLogin());
        PersistenceContext pc = null;
        try {
            pc = PersistenceContext.getCurrent();
        } catch (SQLException e) {
            String msg = "SQL exception getting context";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }
        try {
            pc.beginTransaction();
            if (!userCanGenCert(user)) {
                String msg = "this user is currently not allowed to generate a new cert: " + user.getLogin();
                pc.rollbackTransaction();
                throw new UpdateException(msg);
            }
            CertEntryRow userData = getFromTable(user);
            userData.setResetCounter(userData.getResetCounter()+1);

            sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            try {
                String encodedcert = encoder.encode(cert.getEncoded());
                userData.setCert(encodedcert);
            } catch (CertificateEncodingException e) {
                String msg = "Certificate encoding exception recording cert";
                logger.log(Level.WARNING, msg, e);
                pc.rollbackTransaction();
                throw new UpdateException(msg, e);
            }
            pc.commitTransaction();
        } catch (TransactionException e) {
            String msg = "Transaction exception recording cert";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }
    }

    public Certificate getUserCert(User user) throws FindException {
        logger.finest("getUserCert for " + user.getLogin());
        CertEntryRow userData = getFromTable(user);
        if (userData != null) {
            String dbcert = userData.getCert();
            if (dbcert == null) {
                logger.finest("no cert recorded for user " + user.getLogin());
                return null;
            }
            sun.misc.BASE64Decoder base64decoder = new sun.misc.BASE64Decoder();
            try {
                byte[] certbytes = base64decoder.decodeBuffer(dbcert);
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
        }
        return null;
    }

    public void revokeUserCert(User user) throws UpdateException {
        logger.finest("revokeUserCert for " + user.getLogin());
        PersistenceContext pc = null;
        try {
            pc = PersistenceContext.getCurrent();
        } catch (SQLException e) {
            String msg = "SQL exception getting context";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }
        try {
            pc.beginTransaction();
            CertEntryRow currentdata = getFromTable(user);
            if (currentdata != null) {
                currentdata.setCert(null);
                currentdata.setResetCounter(0);
            }
            pc.commitTransaction();
        } catch (TransactionException e) {
            String msg = "Transaction exception revoking cert";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }
    }

    /**
     * retrieves the table data for a specific user
     * @return the data in the table or null if no data exist for this user
     */
    private CertEntryRow getFromTable(User user) {
        String query = "from " + TABLE_NAME + " in class " + CertEntryRow.class.getName() +
                       " where " + TABLE_NAME + "." + PROVIDER_COLUMN + " = " + Long.toString(user.getProviderId()) +
                       " and " + TABLE_NAME + "." + PROVIDER_LOGIN + " = " + user.getLogin();
        List hibResults = null;
        try {
            hibResults = manager.find(PersistenceContext.getCurrent(), query);
        } catch (FindException e) {
            hibResults = Collections.EMPTY_LIST;
            logger.log(Level.WARNING, "hibernate error finding cert entry for " + user.getLogin(), e);
        } catch (SQLException e) {
            hibResults = Collections.EMPTY_LIST;
            logger.log(Level.WARNING, "hibernate error finding cert entry for " + user.getLogin(), e);
        }

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
    protected Logger logger = null;

    private static final String TABLE_NAME = "client_cert";
    private static final String PROVIDER_COLUMN = "providerId";
    private static final String PROVIDER_LOGIN = "userLogin";
}
