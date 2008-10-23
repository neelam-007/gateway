/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.util.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.DigestZipOutputStream;
import com.l7tech.server.util.CompressedStringType;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * Simple utility to export signed audit records.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class AuditExporterImpl extends HibernateDaoSupport implements AuditExporter {

    public static enum Dialect { MYSQL, DERBY }

    private static final Logger logger = Logger.getLogger(AuditExporterImpl.class.getName());
    private static final String AUTHENTICATION_TYPE = "authenticationType";
    private static final String DETAILS_TO_EXPAND = "audit_associated_logs";
    private static final String AUDITDETAILMESSAGEID = "ADMID:";
    private static final String SEPARATOR = "/-/_/-/";
    private static final int FETCH_SIZE_ROWS = Integer.MIN_VALUE;
    private static final char DELIM = ':';
    private static final String MD5_ALG = "MD5";
    private static final String SHA1_ALG = "SHA-1";
    private static final List<String> DIGEST_ALGS = new ArrayList<String>();
    static {
        DIGEST_ALGS.add(MD5_ALG);
        DIGEST_ALGS.add(SHA1_ALG);
    }
    private static final String AUDITS_FILENAME = "audit.dat";
    private static final String SIG_FILENAME = "sig.xml";
    private static final String SIG_XML = "<audit:AuditMetadata xmlns:audit=\"http://l7tech.com/ns/2004/Oct/08/audit\" />";

    private static final Pattern badCharPattern = Pattern.compile("([^\\040-\\0176]|\\\\|\\" + DELIM + ")");

    private static AtomicBoolean initialized = new AtomicBoolean(false);

    private Dialect dialect = Dialect.MYSQL;
    private long highestTime;
    private volatile long numExportedSoFar = 0;
    private volatile long approxNumToExport = 1;

    /**
     * NOTE: This content and order is important for audit signature verification. See AuditRecord#serializeSignableProperties
     */
    private static final String AUDIT_COLUMNS = 
            "audit_main.objectid, audit_main.nodeid, audit_main.time, audit_main.audit_level, audit_main.name, audit_main.message, audit_main.ip_address, audit_main.user_name, audit_main.user_id, audit_main.provider_oid, audit_main.signature, " +
            "audit_admin.objectid, audit_admin.entity_class, audit_admin.entity_id, audit_admin.action, " +
            "audit_message.objectid, audit_message.status, audit_message.request_id, audit_message.service_oid, audit_message.operation_name, audit_message.authenticated, audit_message.authenticationType, audit_message.request_length, audit_message.response_length, audit_message.request_zipxml, audit_message.response_zipxml, audit_message.response_status, audit_message.routing_latency, " +
            "audit_system.objectid, audit_system.component_id, audit_system.action";

    private static final int QUERY_EXPORT = 0;
    private static final String[][] QUERIES_BY_DIALECT = {
            {  /* MySQL export query */
                    "SELECT " + AUDIT_COLUMNS +", " +
                    "GROUP_CONCAT(''ADMID:'', audit_detail.message_id, ''/-/_/-/'', (SELECT COALESCE(GROUP_CONCAT(value ORDER BY position ASC SEPARATOR ''/-/_/-/''), '''') FROM audit_detail_params WHERE " +
                    "audit_detail_params.audit_detail_oid = audit_detail.objectid) ORDER BY ordinal SEPARATOR ''/-/_/-/'') AS audit_associated_logs " +
                    "FROM audit_main " +
                    "LEFT OUTER JOIN audit_admin ON audit_main.objectid = audit_admin.objectid " +
                    "LEFT OUTER JOIN audit_message ON audit_main.objectid = audit_message.objectid " +
                    "LEFT OUTER JOIN audit_system ON audit_main.objectid = audit_system.objectid " +
                    "LEFT OUTER JOIN audit_detail ON audit_main.objectid = audit_detail.audit_oid {0}  GROUP BY audit_main.objectid",
            },
            {  /* Derby export query */
                    "SELECT " + AUDIT_COLUMNS +", " +
                    "getAuditDetails(audit_main.objectid) AS audit_associated_logs " +
                    "FROM audit_main " +
                    "LEFT OUTER JOIN audit_admin ON audit_main.objectid = audit_admin.objectid " +
                    "LEFT OUTER JOIN audit_message ON audit_main.objectid = audit_message.objectid " +
                    "LEFT OUTER JOIN audit_system ON audit_main.objectid = audit_system.objectid " +
                    "LEFT OUTER JOIN audit_detail ON audit_main.objectid = audit_detail.audit_oid {0}",
            },
    };

    public AuditExporterImpl() {
    }

    public AuditExporterImpl( final Dialect dialect ) {
        this.setDialect( dialect );
    }

    @Override
    protected void initDao() throws Exception {
        if ( dialect == Dialect.DERBY && initialized.compareAndSet(false, true)) {
            // create function
            String queryCreateFunc = "CREATE FUNCTION GETAUDITDETAILS (AUDITDETAILID INTEGER) RETURNS VARCHAR(16384) LANGUAGE JAVA PARAMETER STYLE JAVA READS SQL DATA RETURNS NULL ON NULL INPUT EXTERNAL NAME '"+AuditExporterImpl.class.getName()+".getAuditDetails'";

            Connection conn = null;
            Statement st = null;
            Session session = null;
            try {
                session = getSession();
                conn = getConnectionForExport(session);
                st = conn.createStatement();
                st.executeUpdate(queryCreateFunc);
                logger.config("Initialized audit exporter support function(s).");
            } catch ( SQLException se ) {
                // ignore expected error when function already exists.
                if ("X0Y68".equals(se.getSQLState()) ) {
                    logger.fine("Audit exporter support function(s) found (not creating).");                    
                } else {
                    throw se;
                }
            } finally {
                ResourceUtils.closeQuietly(st);
                ResourceUtils.closeQuietly(conn);
                releaseSessionForExport(session);
            }
        }
    }

    static String quoteMeta(String raw) {
        return badCharPattern.matcher(raw).replaceAll("\\\\$1");
    }

    /**
     * Composes the SQL statement to download audit records.
     *
     * @return SQL statement; never null
     */
    static String composeSql( Dialect dialect, String clause) {
        return MessageFormat.format( QUERIES_BY_DIALECT[dialect.ordinal()][QUERY_EXPORT], clause );
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
     * Composes the SQL statement for counting number of audit records eligible for download.
     *
     * @param startOid  Minimum objectid to be selected.
     * @param endOid    Maximum objectid to be selected.
     * @return SQL statement; never null
     */
    static String composeCountByOidSql(long startOid, long endOid) {
        final StringBuilder s = new StringBuilder("SELECT COUNT(*) FROM audit_main");
        s.append(composeOidWhereClause(startOid, endOid));
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

    /**
     * Composes the SQL WHERE clause based on the given contraints.
     *
     * @param startOid  Minimum objectid to be selected.
     * @param endOid    Maximum objectid to be selected.
     * @return SQL WHERE clause
     */
    static String composeOidWhereClause(long startOid, long endOid) {
        final StringBuilder s = new StringBuilder();

        s.append(" WHERE audit_main.objectid >= ");
        s.append(startOid);

        s.append(" AND ");

        s.append("audit_main.objectid <= ");
        s.append(endOid);

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
    @SuppressWarnings({"deprecation"})
    protected Connection getConnectionForExport(Session session) throws SQLException {
        return session.connection(); // method is deprecated but an alternative is not available in 3.2.6
    }

    /**
     * Set the dialect for use with this exporter.
     *
     * @param dialect The dialect to use, must not be null
     */
    protected void setDialect( final Dialect dialect ) {
        dialect.ordinal(); // NPE here on null
        this.dialect = dialect;
    }

    /**
     * Exports the audit records from the database to the specified OutputStream, in UTF-8 format.
     * The audits are exported as a colon-delimited text file with colon as the record delimiter and "\n" as the
     * field delimiter..
     * The row contains the column names; subsequent rows contain a dump of all the audit records.
     * No signature or other metadata is emitted.
     *
     * @param countSql      the query used to obtain the count of records to be exported
     * @param selectSql     the query used to obtain the records to be exported
     * @param zipOut        the OutputStream to which the colon-delimited dump will be written.
     * @param previousExported the results of the previous export step in this batch, if any; can be null
     * @return the time in milliseconds of the most-recent audit record exported.
     */
    private ExportedInfo exportAudits(String countSql, String selectSql, DigestZipOutputStream zipOut,
                                      final long maxBytes, final ExportedInfo previousExported)
            throws SQLException, IOException, HibernateException, InterruptedException {
        final long exportStartTime = System.currentTimeMillis();
        Connection conn;
        Statement st = null;
        ResultSet rs = null;
        Session session = null;
        try {
            PrintStream out = new PrintStream(zipOut, false, "UTF-8");

            session = getSessionForExport();
            conn = getConnectionForExport(session);
            st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
            if ( dialect == Dialect.MYSQL )
                st.setFetchSize(FETCH_SIZE_ROWS);

            if (logger.isLoggable(Level.FINE)) logger.fine("countSql = " + countSql);
            rs = st.executeQuery(countSql);

            if (rs == null) throw new SQLException("Unable to obtain audit count with query: " + countSql);
            rs.next();
            final long ante = rs.getLong(1);
            rs.close();
            rs = null;
            synchronized (this) { approxNumToExport = ante; }

            if (logger.isLoggable(Level.FINE)) logger.fine("sql = " + selectSql);
            rs = st.executeQuery(selectSql);
            if (rs == null) throw new SQLException("Unable to obtain audits with query: " + selectSql);
            ResultSetMetaData md = rs.getMetaData();

            int timecolumn = 3; // initial guess
            int authenticationTypeColumn = -1;
            int detailsToExpand = -1;
            int columns = md.getColumnCount();
            boolean[] zipColumns = new boolean[columns];
            for (int i = 1; i <= columns; ++i) {
                final String columnName = quoteMeta(md.getColumnName(i));
                if ("time".equalsIgnoreCase(columnName))
                    timecolumn = i;
                else if (AUTHENTICATION_TYPE.equalsIgnoreCase(columnName))
                    authenticationTypeColumn = i;
                else if (DETAILS_TO_EXPAND.equalsIgnoreCase(columnName))
                    detailsToExpand = i;
                else if (columnName.indexOf("_zip") > -1)
                    zipColumns[i-1] = true;
                out.print(columnName.toLowerCase());
                if (i < columns) out.print(DELIM);
            }
            out.print("\n");

            boolean needInitialFlush = true;
            long lowestId = Long.MAX_VALUE;
            long highestId = Long.MIN_VALUE;
            long lowestTime = Long.MAX_VALUE;
            long highestTime = Long.MIN_VALUE;
            synchronized (this) { numExportedSoFar = 0; }
            boolean lastRow;
            long recordsExported = previousExported != null ? previousExported.getRecordsExported() : 0L;
            while (lastRow = rs.next()) {
                // size check
                if ( maxBytes > 0) { // only bother if the limit is set
                    long recordSize = 0;
                    for (int i = 1; i <= columns; ++i) {
                        Clob clob = rs.getClob(i);
                        if (clob != null) recordSize += clob.length();
                    }
                    if ( (maxBytes - zipOut.getZippedByteCount()) < 2L * recordSize / zipOut.getCompressionRatio() )
                        break;
                }

                synchronized (this) { numExportedSoFar++; }
                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedException();
                for (int i = 1; i <= columns; ++i) {
                    if (zipColumns[i-1]) {
                        byte[] data = rs.getBytes(i);
                        if (data != null) {
                            out.print(quoteMeta( CompressedStringType.decompress(data)));
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
                            } else if (i == authenticationTypeColumn) {
                                data = SecurityTokenType.getByNum(rs.getInt(i)).toString();
                                    // Matches MessageSummaryAuditRecord.serializeOtherProperties().
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
                recordsExported++;

                if (needInitialFlush) {
                    out.flush();
                    needInitialFlush = false;
                }
            }

            out.flush();

            synchronized (this) { this.highestTime = highestTime;}

            final long finalLowestId = lowestId;
            final long finalHighestId = highestId;
            final long finalLowestTime = lowestTime;
            final long finalHighestTime = highestTime;
            final boolean finalHasTransferredFullRange = ! lastRow;
            final long finalRecordsExported = recordsExported;
            final DigestZipOutputStream finalZipOut = zipOut;

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

                public boolean hasTransferredFullRange() {
                    return finalHasTransferredFullRange;
                }

                public long getReceivedBytes() {
                    return finalZipOut.getRawByteCount();
                }

                public long getTransferredBytes() {
                    return finalZipOut.getZippedByteCount();
                }

                public long getRecordsExported() {
                    return finalRecordsExported;
                }

                public long getExportStartTime() {
                    return previousExported != null ? previousExported.getExportStartTime() : exportStartTime;
                }

                public long getExportEndTime() {
                    return System.currentTimeMillis();
                }
            };

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
            List<String> paramList = new ArrayList<String>();
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
                        buffer.append("\\:");   // Matches AuditDetail#serializeSignableProperties().
                        buffer.append(formatted);
                    }

                    try {
                        int id = Integer.parseInt(detailPart.substring(AUDITDETAILMESSAGEID.length()));
                        AuditDetailMessage message = Messages.getAuditDetailMessageById(id);
                        currentMessage = message==null? null : message.getMessage();
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
                String formatted = MessageFormat.format(currentMessage, paramList.toArray());
                buffer.append(currentMessageId);
                buffer.append("\\:");   // Matches AuditDetail#serializeSignableProperties().
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

    private static void addElement(Element parent, String indent, String ns, String p, String name, String value) {
        parent.appendChild(DomUtils.createTextNode(parent, indent));
        Element e = DomUtils.createAndAppendElementNS(parent, name, ns, p);
        e.appendChild(DomUtils.createTextNode(e, value));
        parent.appendChild(DomUtils.createTextNode(parent, "\n"));
    }

    public void addXmlSignature(DigestZipOutputStream zip, ExportedInfo exportedInfo,
                                        X509Certificate signingCert, PrivateKey signingKey) throws SignatureException {
        try {
            if (zip == null) {
                logger.warning("Cannot add signature; null output stream.");
                return;
            } else if (exportedInfo == null) {
                logger.warning("Cannot add signature; null export data.");
                return;
            } else if (signingCert == null) {
                logger.warning("Cannot add signature; null signing cert.");
                return;
            } else if (signingKey == null) {
                logger.warning("Cannot add signature; null signing key.");
                return;
            }

            final String startTimeString = ISO8601Date.format(new Date(exportedInfo.getExportStartTime()));
            final String endTimeString = ISO8601Date.format(new Date(exportedInfo.getExportEndTime()));

            // Create XML signature
            Document d;
            d = XmlUtil.stringToDocument(SIG_XML);
            Element auditMetadata = d.getDocumentElement();
            String ns = auditMetadata.getNamespaceURI();
            String p = auditMetadata.getPrefix();
            auditMetadata.appendChild(DomUtils.createTextNode(auditMetadata, "\n"));
            final String i1 = "    ";
            addElement(auditMetadata, i1, ns, p, "exportProcessStarting", startTimeString);
            addElement(auditMetadata, i1, ns, p, "exportProcessStartingMillis", String.valueOf(exportedInfo.getExportStartTime()));
            auditMetadata.appendChild(DomUtils.createTextNode(auditMetadata, "\n"));
            addElement(auditMetadata, i1, ns, p, "exportProcessFinishing", endTimeString);
            addElement(auditMetadata, i1, ns, p, "exportProcessFinishingMillis", String.valueOf(exportedInfo.getExportEndTime()));
            auditMetadata.appendChild(DomUtils.createTextNode(auditMetadata, "\n" + i1));

            Element ead = DomUtils.createAndAppendElementNS(auditMetadata, "ExportedAuditData", ns, p);
            ead.setAttribute("filename", AUDITS_FILENAME);
            ead.appendChild(DomUtils.createTextNode(auditMetadata, "\n"));
            auditMetadata.appendChild(DomUtils.createTextNode(auditMetadata, "\n"));

            final String i2 = "        ";
            addElement(ead, i2, ns, p, "lowestAuditRecordId", String.valueOf(exportedInfo.getLowestId()));
            addElement(ead, i2, ns, p, "earliestAuditRecordDate",
                       ISO8601Date.format(new Date(exportedInfo.getEarliestTime())));
            addElement(ead, i2, ns, p, "earliestAuditRecordDateMillis", String.valueOf(exportedInfo.getEarliestTime()));
            ead.appendChild(DomUtils.createTextNode(ead, "\n"));
            addElement(ead, i2, ns, p, "highestAuditRecordId", String.valueOf(exportedInfo.getHighestId()));
            addElement(ead, i2, ns, p, "latestAuditRecordDate",
                       ISO8601Date.format(new Date(exportedInfo.getLatestTime())));
            addElement(ead, i2, ns, p, "latestAuditRecordDateMillis", String.valueOf(exportedInfo.getLatestTime()));
            ead.appendChild(DomUtils.createTextNode(ead, "\n"));
            addElement(ead, i2, ns, p, "sha1Digest", HexUtils.hexDump(zip.getDigest(SHA1_ALG)));
            addElement(ead, i2, ns, p, "md5Digest", HexUtils.hexDump(zip.getDigest(MD5_ALG)));
            ead.appendChild(DomUtils.createTextNode(ead, i1));

            Element signature = DsigUtil.createEnvelopedSignature(auditMetadata,
                                                         signingCert,
                                                         signingKey, null, null);
            auditMetadata.appendChild(signature);

            zip.putNextEntry(new ZipEntry(SIG_FILENAME));
            byte[] xmlBytes = XmlUtil.nodeToString(d).getBytes("UTF-8");
            zip.write(xmlBytes);
            zip.flush();

        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SignatureStructureException e) {
            throw new CausedSignatureException(e);
        } catch (XSignatureException e) {
            throw new CausedSignatureException(e);
        } catch (IOException e) {
            throw new CausedSignatureException(e);
        }
    }

    private void exportAuditsAsZipFile(String countSql, String selectSql, OutputStream outputStream,
                                      X509Certificate signingCert, PrivateKey signingKey)
            throws IOException, SQLException, HibernateException, SignatureException, InterruptedException
    {
        DigestZipOutputStream zip = null;
        try {
            zip = newAuditExportOutputStream(outputStream);
            ExportedInfo exportedInfo = exportAudits(countSql, selectSql, zip, -1, null);
            addXmlSignature(zip, exportedInfo, signingCert, signingKey);
        } finally {
            // clear interrupted status - this is essential to avoid SQL errors
            Thread.interrupted();
            ResourceUtils.closeQuietly(zip);
        }
    }

    /**
     * Derby function to extract audit details in the expected format.
     *
     * ADMID:4714/-/_/-/string(document('file:.../server.xml')/Server/@port)/-/_/-/ADMID:3017/-/_/-/Warehouse [524288]/-/_/-/601/-/_/-/Error in Assertion Processing
     */
    public static String getAuditDetails( long auditRecordId ) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:default:connection");

        StringBuilder details = new StringBuilder();
        PreparedStatement statement = null;
        ResultSet results = null;
        try {
            statement = connection.prepareStatement(  "select audit_detail.message_id, audit_detail_params.value from audit_detail left outer join audit_detail_params on audit_detail.objectid = audit_detail_params.audit_detail_oid where audit_detail.audit_oid = ?  order by audit_detail.ordinal, audit_detail_params.position" );
            statement.setLong( 1, auditRecordId );
            results = statement.executeQuery();
            int messageId = Integer.MIN_VALUE;
            boolean isFirst = true;
            while ( results.next() ) {
                int auditDetailId = results.getInt(1);
                if ( auditDetailId != messageId ) {
                    messageId = auditDetailId;

                    if ( isFirst ) {
                        isFirst = false;
                    } else {
                        details.append(SEPARATOR);
                    }

                    details.append(AUDITDETAILMESSAGEID);
                    details.append(auditDetailId);
                }

                details.append(SEPARATOR);
                details.append(results.getString(2));
            }
        } finally {
            ResourceUtils.closeQuietly(results);
            ResourceUtils.closeQuietly(statement);
        }

        return details.toString();
    }

    @Transactional(propagation=Propagation.REQUIRED,readOnly=true,rollbackFor={},noRollbackFor=Throwable.class)
    public void exportAuditsAsZipFile(long fromTime,
                                      long toTime,
                                      long[] serviceOids,
                                      OutputStream outputStream,
                                      X509Certificate signingCert,
                                      PrivateKey signingKey)
            throws IOException, SQLException, HibernateException, SignatureException, InterruptedException
    {
        exportAuditsAsZipFile(composeCountSql(fromTime, toTime, serviceOids),
                              composeSql(dialect, composeWhereClause(fromTime, toTime, serviceOids)),
                              outputStream, signingCert, signingKey);
    }

    @Transactional(propagation=Propagation.REQUIRED,readOnly=true,rollbackFor={},noRollbackFor=Throwable.class)
    public ExportedInfo exportAudits(long startOid, long endOid, DigestZipOutputStream zipOut, long maxBytes, ExportedInfo previous)
        throws IOException, SQLException, InterruptedException
    {
        return exportAudits(composeCountByOidSql(startOid, endOid), composeSql(dialect, composeOidWhereClause(startOid, endOid)),
                            zipOut, maxBytes, previous);
    }

    public DigestZipOutputStream newAuditExportOutputStream(OutputStream os) throws IOException {
        try {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Creating new audit exporter zip output stream.");
            DigestZipOutputStream zip = new DigestZipOutputStream(os, DIGEST_ALGS);
            String zipComment = BuildInfo.getBuildString() + " - Exported Audit Records - Created " + ISO8601Date.format(new Date(System.currentTimeMillis()));
            zip.setComment(zipComment);
            zip.putNextEntry(new ZipEntry(AUDITS_FILENAME));
            zip.resetDigests(DIGEST_ALGS);
            return zip;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

}
