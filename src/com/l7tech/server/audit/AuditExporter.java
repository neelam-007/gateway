/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.audit;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.hibernate.HibernateException;

import java.io.OutputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.sql.SQLException;

/**
 * @author alex
 */
public interface AuditExporter {
    @Transactional(propagation= Propagation.SUPPORTS)
    long getHighestTime();

    @Transactional(propagation=Propagation.SUPPORTS)
    long getNumExportedSoFar();

    @Transactional(propagation=Propagation.SUPPORTS)
    long getApproxNumToExport();

    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    void exportAuditsAsZipFile(OutputStream fileOut,
                               X509Certificate signingCert,
                               PrivateKey signingKey)
            throws IOException, SQLException, HibernateException, SignatureException, InterruptedException;

    public interface ExportedInfo {
        long getLowestId();
        long getHighestId();
        long getEarliestTime();
        long getLatestTime();
        byte[] getSha1Hash();
        byte[] getMd5Hash();
    }
}
