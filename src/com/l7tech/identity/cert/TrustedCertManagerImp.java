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
            // todo version check
            PersistenceManager.update( getContext(), cert );
        } catch ( SQLException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new UpdateException("Couldn't update cert", e );
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
