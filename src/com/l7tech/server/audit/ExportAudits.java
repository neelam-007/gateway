/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.util.ISO8601Date;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.HibernatePersistenceManager;
import com.l7tech.objectmodel.PersistenceContext;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Simple command line utility to export signed audit records.
 */
public class ExportAudits {
    private static final String SQL = "select * from audit left outer join audit_main on audit.objectid=audit_main.objectid left ou" +
            "ter join audit_admin on audit.objectid=audit_admin.objectid left outer join audit_message on audit." +
            "objectid=audit_message.objectid left outer join audit_system on audit.objectid=audit_system.objecti" +
            "d";
    private static final String SIG_XML = "<L7:AuditSignature xmlns:L7=\"http://l7tech.com/ns/2004/Oct/08/audit\" />";
    private static final char DELIM = ':';
    private static final Pattern badCharPattern = Pattern.compile("([\\000-\\037]|\\" + DELIM + ")");

    private ExportAudits(String[] args) {
        // TODO parse args, store output file
    }

    private String quoteMeta(String raw) {
        return badCharPattern.matcher(raw).replaceAll("\\$1");
    }

    /**
     * Exports the audit records from the database to the specified OutputStream.
     * The audits are exported as a colon-delimited text file, using the current default character encoding.
     * The row contains the column names; subsequent rows contain a dump of all the audit records.
     * No signature or other metadata is emitted.
     */
    private void exportAllAudits(OutputStream rawOut) throws SQLException, IOException, HibernateException  {
        PrintStream out = new PrintStream(rawOut);
        HibernatePersistenceManager.initialize();

        Session session = ((HibernatePersistenceContext)PersistenceContext.getCurrent()).getAuditSession();
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = session.connection();
            st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
            rs = st.executeQuery(SQL);
            if (rs == null) throw new SQLException("Unable to obtain audits with query: " + SQL);
            ResultSetMetaData md = rs.getMetaData();

            int columns = md.getColumnCount();
            for (int i = 1; i <= columns; ++i) {
                final String columnName = quoteMeta(md.getColumnName(i));
                out.print(columnName);
                if (i < columns) out.print(DELIM);
            }
            out.print("\n");

            while (rs.next()) {
                for (int i = 1; i <= columns; ++i) {
                    String data = rs.getString(i);
                    if (data != null)
                        out.print(quoteMeta(data));
                    if (i < columns) out.print(DELIM);
                }
                out.print("\n");
            }

            out.flush();
        } finally {
            if (rs != null) rs.close();
            if (st != null) st.close();
            if (conn != null) conn.close();
        }
    }

    public static void main(String[] args) {
        try {
            final ExportAudits exportAudits = new ExportAudits(args);
            final long startTime = System.currentTimeMillis();
            FileOutputStream fileOut = new FileOutputStream("audit_" + startTime + ".zip");
            ZipOutputStream zipOut = new ZipOutputStream(fileOut);
            final String dateString = ISO8601Date.format(new Date(startTime));
            zipOut.setComment(BuildInfo.getBuildString() + " - Exported Audit Records - Created " + dateString);
            ZipEntry ze = new ZipEntry("audit.dat");
            zipOut.putNextEntry(ze);
            exportAudits.exportAllAudits(zipOut);
            zipOut.putNextEntry(new ZipEntry("sig.xml"));

            // Create XML signature
            Document d = XmlUtil.stringToDocument(SIG_XML);
            Element auditSignature = d.getDocumentElement();
            zipOut.close();

        } catch (Exception e) {
            System.err.println("Unable to proceed: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
