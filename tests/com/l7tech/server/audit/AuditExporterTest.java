/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PasswordPropertyCrypto;
import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.hibernate.Session;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

/**
 * @author mike
 */
public class AuditExporterTest extends TestCase {
    private static Logger logger = Logger.getLogger(AuditExporterTest.class.getName());

    public AuditExporterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AuditExporterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testQuoteMeta() throws Exception {
        String raw1 = "foo:bar\nbaz, blat boof: here is some unicode: \uf0a8\uf0ad\udead\ubeef\n\t\tlast line";
        String quo1 = "foo\\:bar\\\nbaz, blat boof\\: here is some unicode\\: \\\uf0a8\\\uf0ad\\\udead\\\ubeef\\\n\\\t\\\tlast line";
        String raw2 = ":::blat,blaz&&uqi\\ajqhwo:foompa:geblart\\:smoof backspace this: \b\b\b\b\ndone\n\t \t\n";
        String quo2 = "\\:\\:\\:blat,blaz&&uqi\\\\ajqhwo\\:foompa\\:geblart\\\\\\:smoof backspace this\\: \\\b\\\b\\\b\\\b\\\ndone\\\n\\\t \\\t\\\n";
        String quotedCombined = AuditExporterImpl.quoteMeta(raw1) + ":" + AuditExporterImpl.quoteMeta(raw2) + "\n";
        String expectedQuotedCombined = quo1 + ":" + quo2 + "\n";
        assertEquals(quotedCombined, expectedQuotedCombined);
    }

    private AuditExporterImpl createInstancePointedAtDefaultPartitionDb() throws Exception {
        final OSSpecificFunctions osFunctions = OSDetector.getOSSpecificFunctions("default_");
        PasswordPropertyCrypto passwordCrypto = osFunctions.getPasswordPropertyCrypto();
        Map<String, String> dbProps = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[]{
                DBInformation.PROP_DB_USERNAME,
                DBInformation.PROP_DB_PASSWORD,
                DBInformation.PROP_DB_URL,
        });
        final String databaseURL = dbProps.get(DBInformation.PROP_DB_URL);
        final String databaseUser = dbProps.get(DBInformation.PROP_DB_USERNAME);
        final String databasePasswdCrypt = dbProps.get(DBInformation.PROP_DB_PASSWORD);
        final String databasePasswd = passwordCrypto.decryptIfEncrypted(databasePasswdCrypt);

        logger.info("using database url " + databaseURL);
        logger.info("using database user " + databaseUser);
        logger.info("using database passwd " + databasePasswd);

        final DBActions dbActions = new DBActions(osFunctions);

        return new AuditExporterImpl() {
            protected Session getSessionForExport() {
                return null; // test does not require hibernate
            }

            protected Connection getConnectionForExport(Session session) throws SQLException {
                return dbActions.getConnection(databaseURL, databaseUser, databasePasswd);
            }
        };
    }
    

    public void testComposeSql() throws Exception {
        String sql = AuditExporterImpl.composeSql(-1, -1, null);
        System.out.println(sql);
    }

    public void testAuditExport() throws Exception {
        File zipFile = new File("AuditExporterTest.zip");
        FileOutputStream fileOut = null;
        try {
            AuditExporterImpl exporter = createInstancePointedAtDefaultPartitionDb(); 
            fileOut = new FileOutputStream(zipFile);
            exporter.exportAuditsAsZipFile(-1, -1, null, fileOut,
                                                      TestDocuments.getDotNetServerCertificate(),
                                                      TestDocuments.getDotNetServerPrivateKey());

            ZipFile zip = new ZipFile(zipFile, ZipFile.OPEN_READ);

            // Uncomment this to read all exported audits as a string -- only do this if there aren't a zillion of them in the current test DB!
            //String exported = new String(HexUtils.slurpStream(zip.getInputStream(zip.getEntry("audit.dat"))));

            String sigxml = new String(HexUtils.slurpStream(zip.getInputStream(zip.getEntry("sig.xml"))));
            Document sigDoc = XmlUtil.stringToDocument(sigxml);
            assertNotNull(sigDoc);
        } finally {
            ResourceUtils.closeQuietly(fileOut);
        }
    }
}
