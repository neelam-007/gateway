/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.TestDocuments;

import org.hibernate.Session;
import org.w3c.dom.Document;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

/**
 * @author mike
 */
public class AuditExporterTest {
    private static final String url = "jdbc:mysql://localhost/ssg";
    private static final String username = "gateway";
    private static final String password = "7layer";

    @Test
    public void testQuoteMeta() throws Exception {
        String raw1 = "foo:bar\nbaz, blat boof: here is some unicode: \uf0a8\uf0ad\udead\ubeef\n\t\tlast line";
        String quo1 = "foo\\:bar\\\nbaz, blat boof\\: here is some unicode\\: \\\uf0a8\\\uf0ad\\\udead\\\ubeef\\\n\\\t\\\tlast line";
        String raw2 = ":::blat,blaz&&uqi\\ajqhwo:foompa:geblart\\:smoof backspace this: \b\b\b\b\ndone\n\t \t\n";
        String quo2 = "\\:\\:\\:blat,blaz&&uqi\\\\ajqhwo\\:foompa\\:geblart\\\\\\:smoof backspace this\\: \\\b\\\b\\\b\\\b\\\ndone\\\n\\\t \\\t\\\n";
        String quotedCombined = AuditExporterImpl.quoteMeta(raw1) + ":" + AuditExporterImpl.quoteMeta(raw2) + "\n";
        String expectedQuotedCombined = quo1 + ":" + quo2 + "\n";
        Assert.assertEquals(quotedCombined, expectedQuotedCombined);
    }

    private AuditExporterImpl createTestInstance() throws Exception {
        SyspropUtil.setProperty("jdbc.drivers", "com.mysql.jdbc.Driver");
        
        return new AuditExporterImpl() {
            protected Session getSessionForExport() {
                return null; // test does not require hibernate
            }

            protected Connection getConnectionForExport(Session session) throws SQLException {
                return DriverManager.getConnection(url, username, password);
            }
        };
    }
    
    public void testComposeSql() throws Exception {
        String sql = AuditExporterImpl.composeSql(AuditExporterImpl.Dialect.MYSQL, AuditExporterImpl.composeCountSql(-1, -1, null));
        System.out.println(sql);
    }

    @Test
    @Ignore("Developer test, requires database.")
    public void testAuditExport() throws Exception {
        File zipFile = new File("AuditExporterTest.zip");
        FileOutputStream fileOut = null;
        try {
            AuditExporterImpl exporter = createTestInstance();
            fileOut = new FileOutputStream(zipFile);
            exporter.exportAuditsAsZipFile(-1, -1, null, fileOut,
                                                      TestDocuments.getDotNetServerCertificate(),
                                                      TestDocuments.getDotNetServerPrivateKey());

            ZipFile zip = new ZipFile(zipFile, ZipFile.OPEN_READ);

            // Uncomment this to read all exported audits as a string -- only do this if there aren't a zillion of them in the current test DB!
            //String exported = new String(HexUtils.slurpStream(zip.getInputStream(zip.getEntry("audit.dat"))));

            String sigxml = new String( IOUtils.slurpStream(zip.getInputStream(zip.getEntry("sig.xml"))));
            Document sigDoc = XmlUtil.stringToDocument(sigxml);
            Assert.assertNotNull(sigDoc);
        } finally {
            ResourceUtils.closeQuietly(fileOut);
        }
    }
}
