/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.cert;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.objectmodel.*;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class TrustedCertManagerImp extends HibernateEntityManager implements TrustedCertManager {
    public TrustedCert findByPrimaryKey(long oid) throws FindException {
        try {
            TrustedCert cert = (TrustedCert)PersistenceManager.findByPrimaryKey( getContext(), getImpClass(), oid );
            return cert;
        } catch ( SQLException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new FindException("Couldn't retrieve cert", e);
        }
    }

    public TrustedCert findBySubjectDn(String dn) throws FindException {
        StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS " ).append(getImpClass().getName());
        hql.append(" WHERE " ).append(getTableName()).append(".subjectDn = ?");
        try {
            List found = PersistenceManager.find( getContext(), hql.toString(), dn, String.class );
            switch ( found.size() ) {
                case 0:
                    return null;
                case 1:
                    return (TrustedCert)found.get(0);
                default:
                    throw new FindException("Found multiple TrustedCerts with the same DN");
            }
        } catch ( SQLException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new FindException("Couldn't retrieve cert", e);
        }
    }

    public long save(TrustedCert cert) throws SaveException {
        try {
            return PersistenceManager.save( getContext(), cert );
        } catch ( SQLException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new SaveException("Couldn't save cert", e );
        }
    }

    public void update(TrustedCert cert) throws UpdateException {
        try {
            TrustedCert original = findByPrimaryKey(cert.getOid());
            if ( original == null ) throw new UpdateException("Can't update cert that doesn't exist");

            if ( original.getVersion() != cert.getVersion() )
                throw new StaleUpdateException("TrustedCert with oid=" + cert.getOid() + " was modified by another transaction");

            original.copyFrom(cert);
            PersistenceManager.update( getContext(), original );
        } catch ( SQLException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new UpdateException("Couldn't update cert", e );
        } catch (FindException e) {
            logger.log(Level.WARNING, e.toString(), e);
            throw new UpdateException("Couldn't find cert to be udpated");
        }
    }

    public Class getImpClass() {
        return TrustedCert.class;
    }

    public Class getInterfaceClass() {
        return TrustedCert.class;
    }

    public String getTableName() {
        return "trusted_cert";
    }
}
