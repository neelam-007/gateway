/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.audit.Messages;
import com.l7tech.common.security.xml.DsigUtil;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ISO8601Date;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.objectmodel.CompressedStringType;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Simple utility to export signed audit records.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class AuditExporterImpl extends HibernateDaoSupport implements AuditExporter {

    private static final Logger logger = Logger.getLogger(AuditExporterImpl.class.getName());
    private static final String DETAILS_TO_EXPAND = "audit_associated_logs";
    private static final String AUDITDETAILMESSAGEID = "ADMID:";
    private static final String SEPARATOR = "/-/_/-/";
    private static final int FETCH_SIZE_ROWS = Integer.MIN_VALUE;
    private static final String SIG_XML = "<audit:AuditMetadata xmlns:audit=\"http://l7tech.com/ns/2004/Oct/08/audit\" />";
    private static final char DELIM = ':';
    private static final Pattern badCharPattern = Pattern.compile("([^\\040-\\0176]|\\\\|\\" + DELIM + ")");

    private long highestTime;
    private volatile long numExportedSoFar = 0;
    private volatile long approxNumToExport = 1;

    static String quoteMeta(String raw) {
        return badCharPattern.matcher(raw).replaceAll("\\\\$1");
    }

    /**
     * Composes the SQL statement to download audit records.
     *
     * @param fromTime      minimum audit event time (milliseconds from epoch) to filter; -1 for no minimum
     * @param toTime        maximum audit event time (milliseconds from epoch) to filter; -1 for no maximum
     * @param serviceOids   OIDs of services (thus filtering to service events only); null for no service filtering
     * @return SQL statement; never null
     */
    static String composeSql(long fromTime, long toTime, long[] serviceOids) {
        final StringBuilder s = new StringBuilder(
            "SELECT audit_main.*, audit_admin.*, audit_message.*, audit_system.*, " +
            "GROUP_CONCAT(DISTINCT 'ADMID:', audit_detail.message_id, '/-/_/-/', (SELECT COALESCE(GROUP_CONCAT(value ORDER BY position ASC SEPARATOR '/-/_/-/'), '') FROM audit_detail_params WHERE " +
            "audit_detail_params.audit_detail_oid = audit_detail.objectid) ORDER BY ordinal SEPARATOR '/-/_/-/') AS audit_associated_logs " +
            "FROM audit_main " +
            "LEFT OUTER JOIN audit_admin ON audit_main.objectid = audit_admin.objectid " +
            "LEFT OUTER JOIN audit_message ON audit_main.objectid = audit_message.objectid " +
            "LEFT OUTER JOIN audit_system ON audit_main.objectid = audit_system.objectid " +
            "LEFT OUTER JOIN audit_detail ON audit_main.objectid = audit_detail.audit_oid");
        s.append(composeWhereClause(fromTime, toTime, serviceOids));
        s.append(" GROUP BY audit_main.objectid");
        return s.toString();
    }

    /**
     * Composes the SQL statement for counting number of audit records eligible for download.
     *
     * @param fromTime      minimum audit event time (milliseconds from epoch) to filter; -1 for no minimum
     * @param toTime        maximum audit event time (milliseconds from epoch) to filter; -1 for no maximum
     * @param serviceOids   OIDs of services (thus filtering to service events only); null for no service filtering
     * @return SQL statement; never null
     */
    static String composeCountSql(long fromTime, long toTime, long[] serviceOids) {
        final StringBuilder s = new StringBuilder("SELECT COUNT(*) FROM audit_main");
        if (serviceOids != null && serviceOids.length > 0) {
            s.append(", audit_message");
        }
        s.append(composeWhereClause(fromTime, toTime, serviceOids));
        if (serviceOids != null && serviceOids.length > 0) {
            s.append(" AND audit_main.objectid = audit_message.objectid");
        }
        return s.toString();
    }

    /**
     * Composes the SQL WHERE clause based on the given contraints.
     *
     * @param fromTime      minimum audit event time (milliseconds from epoch) to filter; -1 for no minimum
     * @param toTime        maximum audit event time (milliseconds from epoch) to filter; -1 for no maximum
     * @param serviceOids   OIDs of services (thus filtering to service events only); null for no service filtering
     * @return SQL WHERE clause; may be empty but never null
     */
    static String composeWhereClause(long fromTime, long toTime, long[] serviceOids) {
        final StringBuilder s = new StringBuilder();

        if (fromTime != -1) {
            s.append("audit_main.time >= ");
            s.append(fromTime);
        }

        if (toTime != -1) {
            if (s.length() > 0) s.append(" AND ");
            s.append("audit_main.time <= ");
            s.append(toTime);
        }

        if (serviceOids != null && serviceOids.length > 0) {
            if (s.length() > 0) s.append(" AND ");
            s.append("audit_message.service_oid IN (");
            for (int i = 0; i < serviceOids.length; ++ i) {
                if (i != 0) s.append(", ");
                s.append(serviceOids[i]);
            }
            s.append(")");
        }

        if (s.length() > 0) {
            s.insert(0, " WHERE ");
        }

        return s.toString();
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public synchronized long getHighestTime() {
        return highestTime;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public synchronized long getNumExportedSoFar() {
        return numExportedSoFar;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public synchronized long getApproxNumToExport() {
        return approxNumToExport;
    }

    /**
     * Get a Session object that can be used to create a DB connection.
     * If a session is returned, caller is responsible for releasing it when they are finished by calling
     * releaseSessionForExport().
     * <p/>
     * This method always returns the hibernate session.
     * <p/>
     * Unit tests can override this method to produce a test that doesn't require a hibernate session.  Such unit
     * tests must also override getConnectionForExport() so that it doesn't require a Session instance.
     *
     * @return a Session object, or null if a Session is not required to be passed to getConnectionForExport().
     */
    protected Session getSessionForExport() {
        return super.getSession();
    }

    /**
     * Release a Session that was returned by getSessionForExport.
     * <p/>
     * This method calls releaseSession() if session is non-null.
     *
     * @param session the Session to release, or null to take no action.
     */
    protected void releaseSessionForExport(Session session) {
        if (session != null) releaseSession(session);
    }

    /**
     * Get the DB connection that will be used for export, using the provided Session instance if needed.
     * <p/>
     * This method always calls connection() on the provided Session instance.
     *
     * @param session a Session returned by getSessionForExport(), possibly null.
     * @return a JDBC Connection instance.  Never null.
     * @throws java.sql.SQLException if a connection cannot be created
     */
    protected Connection getConnectionForExport(Session session) throws SQLException {
        return session.connection();
    }

    /**
     * Exports the audit records from the database to the specified OutputStream, in UTF-8 format.
     * The audits are exported as a colon-delimited text file with colon as the record delimiter and "\n" as the
     * field delimiter..
     * The row contains the column names; subsequent rows contain a dump of all the audit records.
     * No signature or other metadata is emitted.
     *
     * @param fromTime      minimum audit event time (milliseconds from epoch) to filter; -1 for no minimum
     * @param toTime        maximum audit event time (milliseconds from epoch) to filter; -1 for no maximum
     * @param serviceOids   OIDs of services (thus filtering to service events only); null for no service filtering
     * @param rawOut        the OutputStream to which the colon-delimited dump will be written.
     * @return the time in milliseconds of the most-recent audit record exported.
     */
    private ExportedInfo exportAllAudits(long fromTime,
                                         long toTime,
                                         long[] serviceOids,
                                         OutputStream rawOut)
            throws SQLException, IOException, HibernateException, InterruptedException {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        Session session = null;
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            DigestOutputStream sha1Out = new DigestOutputStream(rawOut, sha1);
            DigestOutputStream md5Out = new DigestOutputStream(sha1Out, md5);
            PrintStream out = new PrintStream(md5Out, false, "UTF-8");

            session = getSessionForExport();
            conn = getConnectionForExport(session);
            st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
            st.setFetchSize(FETCH_SIZE_ROWS);

            final String countSql = composeCountSql(fromTime, toTime, serviceOids);
            if (logger.isLoggable(Level.FINE)) logger.fine("countSql = " + countSql);
            rs = st.executeQuery(countSql);

            if (rs == null) throw new SQLException("Unable to obtain audit count with query: " + countSql);
            rs.next();
            final long ante = rs.getLong(1);
            rs.close();
            rs = null;
            synchronized (this) { approxNumToExport = ante; }

            final String sql = composeSql(fromTime, toTime, serviceOids);
            if (logger.isLoggable(Level.FINE)) logger.fine("sql = " + sql);
            rs = st.executeQuery(sql);
            if (rs == null) throw new SQLException("Unable to obtain audits with query: " + sql);
            ResultSetMetaData md = rs.getMetaData();

            int timecolumn = 3; // initial guess
            int detailsToExpand = -1;
            int columns = md.getColumnCount();
            boolean[] zipColumns = new boolean[columns];
            for (int i = 1; i <= columns; ++i) {
                final String columnName = quoteMeta(md.getColumnName(i));
                if ("time".equalsIgnoreCase(columnName))
                    timecolumn = i;
                else if (DETAILS_TO_EXPAND.equalsIgnoreCase(columnName))
                    detailsToExpand = i;
                else if (columnName.indexOf("_zip") > -1)
                    zipColumns[i-1] = true;
                out.print(columnName);
                if (i < columns) out.print(DELIM);
            }
            out.print("\n");

            boolean needInitialFlush = true;
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
                    if (zipColumns[i-1]) {
                        byte[] data = rs.getBytes(i);
                        if (data != null) {
                            out.print(quoteMeta(CompressedStringType.decompress(data)));
                        }
                    } else {
                        String data = rs.getString(i);
                        if (data != null) {
                            if (i == timecolumn) {
                                long millis = rs.getLong(i);
                                if (millis > highestTime)
                                    highestTime = millis;
                                if (millis < lowestTime)
                                    lowestTime = millis;
                            } else if (i == detailsToExpand) {
                                data = expandDetails(data);
                            } else if (i == 1) {
                                long id = rs.getLong(i);
                                if (id > highestId)
                                    highestId = id;
                                if (id < lowestId)
                                    lowestId = id;
                            }
                            out.print(quoteMeta(data));
                        }
                    }
                    if (i < columns) out.print(DELIM);
                }
                out.print("\n");

                if (needInitialFlush) {
                    out.flush();
                    needInitialFlush = false;
                }
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
            // clear interrupted status - this is essential to avoid SQL errors
            Thread.interrupted();

            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(st);
            releaseSessionForExport(session);
        }
    }

    /**
     * Expand a string of detail messages into a set with full message text.
     *
     * ADMID:4714/-/_/-/string(document('file:.../server.xml')/Server/@port)/-/_/-/ADMID:3017/-/_/-/Warehouse [524288]/-/_/-/601/-/_/-/Error in Assertion Processing
     */
    private static String expandDetails(String details) {
        StringBuffer buffer = new StringBuffer();

        if (details.startsWith(AUDITDETAILMESSAGEID)) {
            buffer.append("[");
            boolean isFirst = true;

            String[] detailParts = details.split(SEPARATOR);
            String currentMessage = null;
            String currentMessageId = null;
            List paramList = new ArrayList();
            for (String detailPart : detailParts) {
                if (detailPart.startsWith(AUDITDETAILMESSAGEID)) {
                    Object[] paramArray = paramList.toArray();
                    paramList.clear();

                    if (currentMessage != null) {
                        if (!isFirst)
                            buffer.append(",");
                        else
                            isFirst = false;
                        String formatted = MessageFormat.format(currentMessage, paramArray);
                        buffer.append(currentMessageId);
                        buffer.append(":");
                        buffer.append(formatted);
                    }

                    try {
                        currentMessage = Messages.getMessageById(Integer.parseInt(detailPart.substring(AUDITDETAILMESSAGEID.length())));
                        currentMessageId = detailPart.substring(AUDITDETAILMESSAGEID.length());
                    } catch (NumberFormatException nfe) {
                        currentMessage = null;
                        currentMessageId = null;
                    }

                } else {
                    paramList.add(detailPart);
                }
            }

            if (currentMessage != null) {
                if (!isFirst)
                    buffer.append(",");
                else
                    isFirst = false;
                String formatted = MessageFormat.format(currentMessage, paramList.toArray());
                buffer.append(currentMessageId);
                buffer.append(":");
                buffer.append(formatted);
            }

            buffer.append("]");
        }

        return buffer.toString();
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

    @Transactional(propagation=Propagation.REQUIRED,readOnly=true,rollbackFor={},noRollbackFor=Throwable.class)
    public void exportAuditsAsZipFile(long fromTime,
                                      long toTime,
                                      long[] serviceOids,
                                      OutputStream fileOut,
                                      X509Certificate signingCert,
                                      PrivateKey signingKey)
            throws IOException, SQLException, HibernateException, SignatureException, InterruptedException
    {
        final long startTime = System.currentTimeMillis();
        BufferedOutputStream buffOut = new BufferedOutputStream(fileOut, 4096);
        ZipOutputStream zipOut = null;
        try {
            zipOut = new ZipOutputStream(buffOut);
            final String dateString = ISO8601Date.format(new Date(startTime));
            zipOut.setComment(BuildInfo.getBuildString() + " - Exported Audit Records - Created " + dateString);
            ZipEntry ze = new ZipEntry("audit.dat");
            zipOut.putNextEntry(ze);
            ExportedInfo exportedInfo = exportAllAudits(fromTime, toTime, serviceOids, zipOut);
            long highestTime = exportedInfo.getLatestTime();
            zipOut.flush();
            buffOut.flush();
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
                                                         signingKey, null, null);
            auditMetadata.appendChild(signature);

            zipOut.putNextEntry(new ZipEntry("sig.xml"));
            byte[] xmlBytes = XmlUtil.nodeToString(d).getBytes("UTF-8");
            zipOut.write(xmlBytes);
            zipOut.close();
            zipOut = null;
            buffOut.close();
            buffOut = null;
            synchronized (this) { this.highestTime = highestTime; }
            return;

        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SignatureStructureException e) {
            throw new CausedSignatureException(e);
        } catch (XSignatureException e) {
            throw new CausedSignatureException(e);
        } finally {
            ResourceUtils.closeQuietly(zipOut);
            ResourceUtils.closeQuietly(buffOut);
            // clear interrupted status - this is essential to avoid SQL errors
            Thread.interrupted();
        }
    }
}
