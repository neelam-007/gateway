/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.security.xml.DsigUtil;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ISO8601Date;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.PersistenceContext;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Simple command line utility to export signed audit records.
 */
public class AuditExporter {
    private static final Logger logger = Logger.getLogger(AuditExporter.class.getName());

    private static final String SQL = "select * from audit_main left ou" +
            "ter join audit_admin on audit_main.objectid=audit_admin.objectid left outer join audit_message on audit_main." +
            "objectid=audit_message.objectid left outer join audit_system on audit_main.objectid=audit_system.objecti" +
            "d";
    private static final String COUNT_SQL = "select count(*) from audit_main";
    private static final String SIG_XML = "<audit:AuditMetadata xmlns:audit=\"http://l7tech.com/ns/2004/Oct/08/audit\" />";
    private static final char DELIM = ':';
    private static final Pattern badCharPattern = Pattern.compile("([^\\040-\\0176]|\\\\|\\" + DELIM + ")");

    private long highestTime;
    private long numExportedSoFar = 0;
    private long approxNumToExport = 1;

    public AuditExporter() {

    }

    static String quoteMeta(String raw) {
        return badCharPattern.matcher(raw).replaceAll("\\\\$1");
    }

    public synchronized long getHighestTime() {
        return highestTime;
    }

    public synchronized long getNumExportedSoFar() {
        return numExportedSoFar;
    }

    public synchronized long getApproxNumToExport() {
        return approxNumToExport;
    }

    interface ExportedInfo {
        long getLowestId();
        long getHighestId();
        long getEarliestTime();
        long getLatestTime();
        byte[] getSha1Hash();
        byte[] getMd5Hash();
    }

    /**
     * Exports the audit records from the database to the specified OutputStream, in UTF-8 format.
     * The audits are exported as a colon-delimited text file with colon as the record delimiter and "\n" as the
     * field delimiter..
     * The row contains the column names; subsequent rows contain a dump of all the audit records.
     * No signature or other metadata is emitted.
     * @param rawOut the OutputStream to which the colon-delimited dump will be written.
     * @return the time in milliseconds of the most-recent audit record exported.
     */
    private ExportedInfo exportAllAudits(OutputStream rawOut) throws SQLException, IOException, HibernateException, InterruptedException {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            DigestOutputStream sha1Out = new DigestOutputStream(rawOut, sha1);
            DigestOutputStream md5Out = new DigestOutputStream(sha1Out, md5);
            PrintStream out = new PrintStream(md5Out, false, "UTF-8");

            Session session = ((HibernatePersistenceContext)PersistenceContext.getCurrent()).getAuditSession();
            conn = session.connection();
            st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);

            rs = st.executeQuery(COUNT_SQL);

            if (rs == null) throw new SQLException("Unable to obtain audit count with query: " + COUNT_SQL);
            rs.next();
            final long ante = rs.getLong(1);
            logger.warning("Total audits: " + ante);
            rs.close();
            rs = null;
            synchronized (this) { approxNumToExport = ante; }

            rs = st.executeQuery(SQL);
            if (rs == null) throw new SQLException("Unable to obtain audits with query: " + SQL);
            ResultSetMetaData md = rs.getMetaData();

            int timecolumn = 3;
            int columns = md.getColumnCount();
            for (int i = 1; i <= columns; ++i) {
                final String columnName = quoteMeta(md.getColumnName(i));
                if ("time".equalsIgnoreCase(columnName))
                    timecolumn = i;
                out.print(columnName);
                if (i < columns) out.print(DELIM);
            }
            out.print("\n");

            long lowestId = Long.MAX_VALUE;
            long highestId = Long.MIN_VALUE;
            long lowestTime = Long.MAX_VALUE;
            long highestTime = Long.MIN_VALUE;
            synchronized (this) { numExportedSoFar = 0; }
            while (rs.next()) {
                synchronized (this) { numExportedSoFar++; }
                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedException();
                for (int i = 1; i <= columns; ++i) {
                    String data = rs.getString(i);
                    if (data != null) {
                        if (i == timecolumn) {
                            long millis = rs.getLong(i);
                            if (millis > highestTime)
                                highestTime = millis;
                            if (millis < lowestTime)
                                lowestTime = millis;
                        } else if (i == 1) {
                            long id = rs.getLong(i);
                            if (id > highestId)
                                highestId = id;
                            if (id < lowestId)
                                lowestId = id;
                        }
                        out.print(quoteMeta(data));
                    }
                    if (i < columns) out.print(DELIM);
                }
                out.print("\n");
            }

            out.flush();

            final long finalLowestId = lowestId;
            final long finalHighestId = highestId;
            final long finalLowestTime = lowestTime;
            final long finalHighestTime = highestTime;
            final byte[] sha1Digest = sha1Out.getMessageDigest().digest();
            final byte[] md5Digest = md5Out.getMessageDigest().digest();

            return new ExportedInfo() {
                public long getLowestId() {
                    return finalLowestId;
                }

                public long getHighestId() {
                    return finalHighestId;
                }

                public long getEarliestTime() {
                    return finalLowestTime;
                }

                public long getLatestTime() {
                    return finalHighestTime;
                }

                public byte[] getSha1Hash() {
                    return sha1Digest;
                }

                public byte[] getMd5Hash() {
                    return md5Digest;
                }
            };

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        } finally {
            try {
                if (rs != null) rs.close();
            } finally {
                try {
                    if (st != null) st.close();
                } finally {
                    if (conn != null) conn.close();
                }
            }
        }
    }

    private static class CausedSignatureException extends SignatureException {
        public CausedSignatureException() {
        }

        public CausedSignatureException(String msg) {
            super(msg);
        }

        public CausedSignatureException(Throwable cause) {
            super();
            initCause(cause);
        }

        public CausedSignatureException(String msg, Throwable cause) {
            super(msg);
            initCause(cause);
        }
    }

    private void addElement(Element parent, String indent, String ns, String p, String name, String value) {
        parent.appendChild(XmlUtil.createTextNode(parent, indent));
        Element e = XmlUtil.createAndAppendElementNS(parent, name, ns, p);
        e.appendChild(XmlUtil.createTextNode(e, value));
        parent.appendChild(XmlUtil.createTextNode(parent, "\n"));
    }

    /**
     * Export all audit events from the database to the specified OutputStream as a Zip file, including a signature.
     * @param fileOut OutputStream to which the Zip file will be written.
     */
    public void exportAuditsAsZipFile(OutputStream fileOut,
                                             X509Certificate signingCert,
                                             PrivateKey signingKey)
            throws IOException, SQLException, HibernateException, SignatureException, InterruptedException
    {
        final long startTime = System.currentTimeMillis();
        ZipOutputStream zipOut = null;
        try {
            zipOut = new ZipOutputStream(fileOut);
            final String dateString = ISO8601Date.format(new Date(startTime));
            zipOut.setComment(BuildInfo.getBuildString() + " - Exported Audit Records - Created " + dateString);
            ZipEntry ze = new ZipEntry("audit.dat");
            zipOut.putNextEntry(ze);
            ExportedInfo exportedInfo = exportAllAudits(zipOut);
            long highestTime = exportedInfo.getLatestTime();
            zipOut.flush();
            final long endTime = System.currentTimeMillis();
            final String endTimeString = ISO8601Date.format(new Date(endTime));

            // Create XML signature
            Document d = null;
            d = XmlUtil.stringToDocument(SIG_XML);
            Element auditMetadata = d.getDocumentElement();
            String ns = auditMetadata.getNamespaceURI();
            String p = auditMetadata.getPrefix();
            auditMetadata.appendChild(XmlUtil.createTextNode(auditMetadata, "\n"));
            final String i1 = "    ";
            addElement(auditMetadata, i1, ns, p, "exportProcessStarting", dateString);
            addElement(auditMetadata, i1, ns, p, "exportProcessStartingMillis", String.valueOf(startTime));
            auditMetadata.appendChild(XmlUtil.createTextNode(auditMetadata, "\n"));
            addElement(auditMetadata, i1, ns, p, "exportProcessFinishing", endTimeString);
            addElement(auditMetadata, i1, ns, p, "exportProcessFinishingMillis", String.valueOf(endTime));
            auditMetadata.appendChild(XmlUtil.createTextNode(auditMetadata, "\n" + i1));

            Element ead = XmlUtil.createAndAppendElementNS(auditMetadata, "ExportedAuditData", ns, p);
            ead.setAttribute("filename", "audit.dat");
            ead.appendChild(XmlUtil.createTextNode(auditMetadata, "\n"));
            auditMetadata.appendChild(XmlUtil.createTextNode(auditMetadata, "\n"));

            final String i2 = "        ";
            addElement(ead, i2, ns, p, "lowestAuditRecordId", String.valueOf(exportedInfo.getLowestId()));
            addElement(ead, i2, ns, p, "earliestAuditRecordDate",
                       ISO8601Date.format(new Date(exportedInfo.getEarliestTime())));
            addElement(ead, i2, ns, p, "earliestAuditRecordDateMillis", String.valueOf(exportedInfo.getEarliestTime()));
            ead.appendChild(XmlUtil.createTextNode(ead, "\n"));
            addElement(ead, i2, ns, p, "highestAuditRecordId", String.valueOf(exportedInfo.getHighestId()));
            addElement(ead, i2, ns, p, "latestAuditRecordDate",
                       ISO8601Date.format(new Date(exportedInfo.getLatestTime())));
            addElement(ead, i2, ns, p, "latestAuditRecordDateMillis", String.valueOf(exportedInfo.getLatestTime()));
            ead.appendChild(XmlUtil.createTextNode(ead, "\n"));
            addElement(ead, i2, ns, p, "sha1Digest", HexUtils.hexDump(exportedInfo.getSha1Hash()));
            addElement(ead, i2, ns, p, "md5Digest", HexUtils.hexDump(exportedInfo.getMd5Hash()));
            ead.appendChild(XmlUtil.createTextNode(ead, i1));

            Element signature = DsigUtil.createEnvelopedSignature(auditMetadata,
                                                         signingCert,
                                                         signingKey);
            auditMetadata.appendChild(signature);

            zipOut.putNextEntry(new ZipEntry("sig.xml"));
            XmlUtil.nodeToOutputStream(d, zipOut);
            zipOut.close();
            zipOut = null;
            synchronized (this) { this.highestTime = highestTime; }
            return;
            
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SignatureStructureException e) {
            throw new CausedSignatureException(e);
        } catch (XSignatureException e) {
            throw new CausedSignatureException(e);
        } finally {
            if (zipOut != null) zipOut.close();
        }
    }
}
