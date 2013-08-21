/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.CertEntryRow;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Not a {@link com.l7tech.server.HibernateEntityManager} for historical reasons.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class ClientCertManagerImp extends HibernateDaoSupport implements ClientCertManager {
    private final DefaultKey defaultKey;

    public ClientCertManagerImp(DefaultKey defaultKey) {
        this.defaultKey = defaultKey;
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public boolean isCertPossiblyStale(X509Certificate userCert) {
        SsgKeyEntry caInfo = defaultKey.getCaInfo();
        if (caInfo == null) {
            // No CA key currently configured -- assume cert is not stale
            return false;
        }

        X509Certificate caCert = caInfo.getCertificate();
        byte[] rootCertSki = CertUtils.getSKIBytesFromCert(caCert);

        if ( userCert.getIssuerX500Principal().equals(caCert.getSubjectX500Principal()) ) {
            // Check whether the request cert was signed with this version of the CA cert
            byte[] aki = CertUtils.getAKIBytesFromCert(userCert);
            // Bug #2094: mlyons: If no aki, can't tell if cert is stale.  Assume it isn't.
            return aki != null && !Arrays.equals(aki, rootCertSki);
        }

        return false;
    }

    @Override
    @Transactional(readOnly=true)
    public boolean userCanGenCert(User user, Certificate existingCert) throws FindException {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        logger.finest("userCanGenCert for " + getName(user));
        CertEntryRow userData = getFromTable(user);
        // if this user has no data at all, then he is allowed to generate a cert
        if (userData == null) return true;
        // if user has a cert he is only allowed is his counter is below 10
        return userData.getCertBase64() == null
            || existingCert instanceof X509Certificate && isCertPossiblyStale((X509Certificate)existingCert)
            || userData.getResetCounter() < 10;
    }

    @Override
    public void recordNewUserCert(User user, Certificate cert, boolean oldCertWasStale) throws UpdateException {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("recordNewUserCert for " + getName(user));

        // check if operation is permitted
        CertEntryRow userData;
        try {
            userData = getFromTable(user);
        } catch (FindException e) {
            throw new UpdateException("Couldn't find user to update cert", e);
        }

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

    @Override
    @Transactional(readOnly=true)
    public Certificate getUserCert(User user) throws FindException {
        if (user == null) throw new IllegalArgumentException("user cannot be null");
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

    @Override
    public void revokeUserCert(User user) throws UpdateException {
        revokeUserCertIfIssuerMatches(user, null);
    }

    @Override
    public boolean revokeUserCertIfIssuerMatches(User user, X500Principal issuer) throws UpdateException {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("revokeUserCert for " + getName(user) + (issuer==null ? "" : " issuer "+issuer.toString()));
        boolean revokedUserCertificate = false;
        CertEntryRow currentdata;
        try {
            currentdata = getFromTable(user);
        } catch (FindException e) {
            throw new UpdateException("Couldn't find user to update cert", e);
        }
        if (currentdata != null) {
            boolean revoke = false;
            if (issuer != null) {
                X509Certificate userCertificate = currentdata.getCertificate();
                if (userCertificate != null && userCertificate.getIssuerX500Principal().equals(issuer)) {
                    revoke = true;
                }
            } else {
                revoke = true;
            }

            if (revoke) {
                currentdata.setCertBase64(null);
                currentdata.setResetCounter(0);
                try {
                    getHibernateTemplate().delete(currentdata);
                    revokedUserCertificate = true;
                } catch (HibernateException e) {
                    String msg = "Hibernate exception revoking cert";
                    logger.log(Level.WARNING, msg, e);
                    throw new UpdateException(msg, e);
                }
            }
        } else {
            logger.fine("there was no existing cert for " + getName(user));
        }

        return revokedUserCertificate;
    }

    @Override
    public void forbidCertReset(User user) throws UpdateException {
        if (user == null) throw new IllegalArgumentException("can't call this with null");
        logger.finest("forbidCertReset for " + getName(user));
        boolean wasSystem = AuditContextUtils.isSystem();
        AuditContextUtils.setSystem(true);
        try {
            CertEntryRow currentdata;
            try {
                currentdata = getFromTable(user);
            } catch (FindException e) {
                throw new UpdateException("Couldn't find user to update cert", e);
            }
            if (currentdata != null) {
                if ( currentdata.getResetCounter() != 10 ) {
                    currentdata.setResetCounter(10);
                    try {
                        // update existing data
                        getHibernateTemplate().update(currentdata);
                        getHibernateTemplate().flush();
                    } catch (HibernateException e) {
                        String msg = "Hibernate exception updating cert info";
                        logger.log(Level.WARNING, msg, e);
                        throw new UpdateException(msg, e);
                    }
                }
            } else {
                // this should not happen
                String msg = "there was no existing cert for " + getName(user);
                logger.warning(msg);
                throw new UpdateException(msg);
            }
        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }
    }

    @Override
    @Transactional(readOnly=true)
    public List<CertEntryRow> findByThumbprint(String thumbprint) throws FindException {
        return simpleQuery("thumbprintSha1", thumbprint);
    }

    @Override
    @Transactional(readOnly=true)
    public List<CertEntryRow> findBySki(String ski) throws FindException {
        return simpleQuery("ski", ski);
    }

    @Override
    @Transactional(readOnly=true)
    public List<CertEntryRow> findByIssuerAndSerial(final X500Principal issuer, final BigInteger serial) throws FindException {
        if (issuer == null || serial == null) throw new NullPointerException();
        return simpleQuery(new Pair<String, Object>("issuerDn", CertUtils.getDN( issuer )),
                           new Pair<String, Object>("serial", serial));
    }

    @Override
    @Transactional(readOnly=true)
    public List<CertEntryRow> findBySubjectDn(X500Principal subjectDn) throws FindException {
        return simpleQuery("subjectDn", CertUtils.getDN(subjectDn)); // TODO use only CANONICAL format for this lookup and in the DB
    }

    @Override
    public List<CertInfo> findAll() throws FindException {
        List<CertInfo> allCerts = new ArrayList<CertInfo>();

        try {
            @SuppressWarnings({"unchecked"})
            List<CertEntryRow> userCerts = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    return session.createCriteria(CertEntryRow.class).list();
                }
            });

            for (CertEntryRow cer : userCerts) {
                allCerts.add(new CertInfoImpl(cer));
            }
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new FindException("Couldn't retrieve cert", e);
        }

        return Collections.unmodifiableList(allCerts);
    }

    private List<CertEntryRow> simpleQuery(final String fieldname, final Object value) throws FindException {
        return simpleQuery(new Pair<String, Object>(fieldname, value));
    }

    private List<CertEntryRow> simpleQuery(final Pair<String, Object>... nvps) throws FindException {
        if (nvps == null || nvps.length == 0) throw new IllegalArgumentException("fieldnames and values most be non-null and not empty");
        try {
            //noinspection unchecked
            return getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria crit = session.createCriteria(CertEntryRow.class);
                    for (Pair<String, Object> nvp : nvps) {
                        final String fieldname = nvp.left;
                        final Object value = nvp.right;
                        crit.add(value == null ? Restrictions.isNull(fieldname) : Restrictions.eq(fieldname, value));
                    }
                    return crit.list();
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Couldn't retrieve cert", e);
        }
    }

    /*
     * retrieves the table data for a specific user
     * @return the data in the table or null if no data exist for this user
     */
    private CertEntryRow getFromTable(final User user) throws FindException {
        // Try to find by providerId & userId first
        List<CertEntryRow> rows = simpleQuery(new Pair<String, Object>(PROVIDER_COLUMN, user.getProviderId()), 
                                              new Pair<String, Object>(USER_ID, user.getId()));
        if (rows.size() == 1) return rows.get(0);

        // Then try to find by providerId & login
        final String login = user.getLogin();
        if (login == null || login.length() == 0) return null; // Bug 7003, FIP users frequently have no login

        rows = simpleQuery(new Pair<String, Object>(PROVIDER_COLUMN, user.getProviderId()),
                           new Pair<String, Object>(USER_LOGIN, login));
        if (rows.size() == 1) return rows.get(0);
        if (rows.size() > 1) throw new FindException("Found more than one cert for this user");
        return null;
    }

    private static final Logger logger = Logger.getLogger(ClientCertManagerImp.class.getName());

    private static final String PROVIDER_COLUMN = "provider";
    private static final String USER_ID = "userId";
    private static final String USER_LOGIN = "login";

    private static class CertInfoImpl implements CertInfo {
        private final Goid providerId;
        private final String userId;
        private final String login;

        private CertInfoImpl(CertEntryRow cer) {
            this.providerId = cer.getProvider();
            this.userId = cer.getUserId();
            this.login = cer.getLogin();
        }

        @Override
        public String getLogin() {
            return login;
        }

        @Override
        public Goid getProviderId() {
            return providerId;
        }

        @Override
        public String getUserId() {
            return userId;
        }
    }
}
