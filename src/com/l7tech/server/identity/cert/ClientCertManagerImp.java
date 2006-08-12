package com.l7tech.server.identity.cert;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.Arrays;
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
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class ClientCertManagerImp extends HibernateDaoSupport implements ClientCertManager {
    private X509Certificate rootCertificate = null;
    protected byte[] rootCertSki = null;
    protected String rootCertSubjectName = null;

    @Transactional(propagation=Propagation.SUPPORTS)
    public void setRootCertificate(X509Certificate rootCertificate) {
        if (rootCertificate == null) throw new IllegalArgumentException("Root certificate must not be null");
        this.rootCertificate = rootCertificate;
        this.rootCertSubjectName = rootCertificate.getSubjectDN().getName();
        this.rootCertSki = CertUtils.getSKIBytesFromCert(rootCertificate);
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public boolean isCertPossiblyStale(X509Certificate userCert) {
        if (rootCertificate == null) throw new IllegalStateException("No root certificate set");
        if (rootCertSki == null || rootCertSubjectName == null) throw new IllegalStateException("Missing stuff");
        String requestCertIssuerName = userCert.getIssuerDN().getName();

        if (requestCertIssuerName.equals(rootCertSubjectName)) {
            // Check whether the request cert was signed with this version of the CA cert
            byte[] aki = CertUtils.getAKIBytesFromCert(userCert);
            if (aki == null) {
                // Bug #2094: mlyons: No aki -- can't tell if cert is stale.  Assume it isn't.
                return false;
            }
            return !Arrays.equals(aki, rootCertSki);
        }

        return false;
    }

    @Transactional(readOnly=true)
    public boolean userCanGenCert(User user, Certificate existingCert) {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("userCanGenCert for " + getName(user));
        CertEntryRow userData = getFromTable(user);
        // if this user has no data at all, then he is allowed to generate a cert
        if (userData == null) return true;
        // if user has a cert he is only allowed is his counter is below 10
        if (userData.getCertBase64() != null) {
            if (existingCert instanceof X509Certificate && isCertPossiblyStale((X509Certificate)existingCert))
                return true;

            return userData.getResetCounter() < 10;
        } else return true;
    }

    public void recordNewUserCert(User user, Certificate cert, boolean oldCertWasStale) throws UpdateException {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("recordNewUserCert for " + getName(user));

        /*
        We dont check that anymore. instead, we ensure that we use the subject of authenticated users
        instead of the subject of received csr's when creating certs
        // check that the cert's subject matches the user's login
        try {
            X500Name x500name = new X500Name(((X509Certificate)(cert)).getSubjectX500Principal().getName());
            String login = user.getLogin();
            if (login == null || login.length() == 0) {
                logger.log(Level.INFO, "User " + user.getName() + " has no login. Will save cert anyway.");
            } else if (!x500name.getCommonName().equals(login)) {
                String msg = "login value \'" + login + "\' does not match the cert subject \'" +
                             x500name.getCommonName() + "\'.";
                logger.log(Level.SEVERE, msg);
                throw new UpdateException(msg);

            } else {
                logger.finest("Cert's subject matches authenticated user's login (" + user.getLogin() + ")");
            }
        } catch (IOException e) {
            throw new UpdateException("could not verify the cert subject", e);
        }
        */

        // check if operation is permitted
        CertEntryRow userData = getFromTable(user);

        // if this user has no data at all, then he is allowed to generate a cert
        boolean canRegenCert =
                userData == null ||
                userData.getCertBase64() == null ||
                oldCertWasStale ||
                userData.getResetCounter() < 10;

        if (!canRegenCert) {
            String msg = "this user is currently not allowed to generate a new cert: " + getName(user);
            logger.info(msg);
            throw new UpdateException(msg);
        }

        // this could be new entry
        boolean newentry = false;
        if (userData == null) {
            userData = new CertEntryRow();
            userData.setProvider(user.getProviderId());
            userData.setUserId(user.getId());
            userData.setLogin(user.getLogin());
            userData.setResetCounter(0);
            newentry = true;
        }
        userData.setResetCounter(userData.getResetCounter()+1);

        try {
            String encodedcert = HexUtils.encodeBase64(cert.getEncoded());
            userData.setCertBase64(encodedcert);
        } catch (CertificateEncodingException e) {
            String msg = "Certificate encoding exception recording cert";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }

        // record new data
        try {
            if (newentry) {
                Object res = getHibernateTemplate().save(userData);
                logger.finest("saving cert entry " + res);
            } else {
                logger.finest("updating cert entry");
                getHibernateTemplate().update(userData);
            }
        } catch (HibernateException e) {
            String msg = "Hibernate exception recording cert";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }
    }

    @Transactional(readOnly=true)
    public Certificate getUserCert(User user) throws FindException {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("getUserCert for " + getName(user));
        CertEntryRow userData = getFromTable(user);
        if (userData != null) {
            String dbcert = userData.getCertBase64();
            if (dbcert == null) {
                logger.finest("no cert recorded for user " + getName(user));
                return null;
            }

            try {
                return CertUtils.decodeCert(HexUtils.decodeBase64(dbcert));
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
            logger.finest("no entry for " + getName(user) + " in provider " + user.getProviderId());
        }
        return null;
    }

    private String getName(User user) {
        String name = user.getLogin();
        if (name == null || name.length() == 0) name = user.getName();
        if (name == null || name.length() == 0) name = user.getId();
        return name;
    }

    public void revokeUserCert(User user) throws UpdateException {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("revokeUserCert for " + getName(user));
        CertEntryRow currentdata = getFromTable(user);
        if (currentdata != null) {
            currentdata.setCertBase64(null);
            currentdata.setResetCounter(0);
            try {
                getHibernateTemplate().delete(currentdata);
            } catch (HibernateException e) {
                String msg = "Hibernate exception revoking cert";
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            }
        } else {
            logger.fine("there was no existing cert for " + getName(user));
        }
    }

    public void forbidCertReset(User user) throws UpdateException {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("forbidCertReset for " + getName(user));
        CertEntryRow currentdata = getFromTable(user);
        if (currentdata != null) {
            currentdata.setResetCounter(10);
            try {
                // update existing data
                getHibernateTemplate().update(currentdata);
            } catch (HibernateException e) {
                String msg = "Hibernate exception updating cert info";
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            }
        } else {
            // this should not happen
            String msg = "there was no existing cert for " + getName(user);
            logger.warning(msg);
            throw new UpdateException(msg);
        }
    }

    @Transactional(readOnly=true)
    public List findByThumbprint(String thumbprint) throws FindException {
        return simpleQuery("thumbprintSha1", thumbprint);
    }

    @Transactional(readOnly=true)
    public List findBySki(String ski) throws FindException {
        return simpleQuery("ski", ski);
    }

    private List simpleQuery(String fieldname, final String value) throws FindException {
        final StringBuffer hql = new StringBuffer("FROM ");
        hql.append("cc").append(" IN CLASS ").append(CertEntryRow.class.getName());
        hql.append(" WHERE ").append("cc").append(".").append(fieldname).append(" ");

        try {
            return getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    if (value == null) {
                        hql.append("is null");
                        return session.createQuery(hql.toString()).list();
                    }

                    hql.append(" = ?");
                    return session.createQuery(hql.toString()).setString(0, value.trim()).list();
                }
            });
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new FindException("Couldn't retrieve cert", e);
        }
    }


    /**
     * retrieves the table data for a specific user
     * @return the data in the table or null if no data exist for this user
     */
    private CertEntryRow getFromTable(final User user) {
        try {
            return (CertEntryRow)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(FIND_BY_USER_ID);
                    q.setLong(0, user.getProviderId());
                    q.setString(1, user.getId());
                    CertEntryRow row = (CertEntryRow)q.uniqueResult();
                    if (row != null) return row;
                    if (user.getLogin() != null && user.getLogin().length() > 0) {
                        // Try searching by login if userId fails
                        q = session.createQuery(FIND_BY_LOGIN);
                        q.setLong(0, user.getProviderId());
                        q.setString(1, user.getLogin());
                        return q.uniqueResult();
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            logger.log(Level.WARNING, "hibernate error finding cert entry for " + getName(user), e);
            return null;
        }
    }

    protected final Logger logger = Logger.getLogger(getClass().getName());

    private static final String TABLE_NAME = "client_cert";
    private static final String PROVIDER_COLUMN = "provider";
    private static final String USER_ID = "userId";
    private static final String USER_LOGIN = "login";

    private static final String FIND_BY_USER_ID = "from " + TABLE_NAME + " in class " + CertEntryRow.class.getName() +
                           " where " + TABLE_NAME + "." + PROVIDER_COLUMN + " = ?" +
                           " and " + TABLE_NAME + "." + USER_ID + " = ?";

    private static final String FIND_BY_LOGIN = "from " + TABLE_NAME + " in class " + CertEntryRow.class.getName() +
                           " where " + TABLE_NAME + "." + PROVIDER_COLUMN + " = ?" +
                           " and " + TABLE_NAME + "." + USER_LOGIN + " = ?";

}
