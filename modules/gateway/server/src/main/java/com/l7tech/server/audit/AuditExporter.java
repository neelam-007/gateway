/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.audit;

import org.hibernate.HibernateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
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

    /**
     * Export all audit events from the database to the specified OutputStream as a Zip file, including a signature.
     *
     * @param fromTime      minimum audit event time (milliseconds from epoch) to filter; -1 for no minimum
     * @param toTime        maximum audit event time (milliseconds from epoch) to filter; -1 for no maximum
     * @param serviceOids   OIDs of services (thus filtering to service events only); null for no service filtering
     * @param fileOut       OutputStream to which the ZIP file will be written
     */
    @Transactional(propagation=Propagation.REQUIRED, readOnly=true)
    void exportAuditsAsZipFile(long fromTime,
                               long toTime,
                               long[] serviceOids,
                               OutputStream fileOut,
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
