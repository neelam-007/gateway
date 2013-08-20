/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.audit;

import com.l7tech.objectmodel.Goid;
import org.hibernate.HibernateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;

import com.l7tech.common.io.DigestZipOutputStream;

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
                               Goid[] serviceOids,
                               OutputStream fileOut,
                               X509Certificate signingCert,
                               PrivateKey signingKey)
            throws IOException, SQLException, HibernateException, SignatureException, InterruptedException;


    /**
     * Initializes and returns a DigestZipOutputStream ready to be used for the actual export.
     * The digest hashes must be reset, so that they will apply to the actual file within the archive.
     */
    public DigestZipOutputStream newAuditExportOutputStream(OutputStream os)
        throws IOException;

    /**
     * Exports audit records with the object id in the given range to the provided DigestZipOutputStream.
     * If the same output stream is used in a batch, the previous result may/should be provided to this call.
     *
     * @param bytesRemaining    No more than this number of bytes should be written to the zipped stream; 0 = unlimited.
     * @return                  Metainformation about the exported data.
     */
    @Transactional(propagation=Propagation.REQUIRED, readOnly=true)
    ExportedInfo exportAudits(long startOid, long endOid, DigestZipOutputStream zipOut, long bytesRemaining, ExportedInfo previous)
        throws IOException, SQLException, InterruptedException;

    /**
     * Adds a XML signature to the zip stream.
     *
     * @param exportedInfo  Information to be added to the signature.
     */
    public void addXmlSignature(DigestZipOutputStream zip, ExportedInfo exportedInfo,
                                X509Certificate signingCert, PrivateKey signingKey)
        throws SignatureException;

    public interface ExportedInfo {
        long getEarliestTime();
        long getLatestTime();

        boolean hasTransferredFullRange();
        long getTransferredBytes();
        long getReceivedBytes();
        long getRecordsExported();

        long getExportStartTime();
        long getExportEndTime();
    }
}
