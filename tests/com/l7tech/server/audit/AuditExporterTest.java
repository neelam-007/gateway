/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.objectmodel.HibernatePersistenceManager;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.FileOutputStream;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class AuditExporterTest extends TestCase {
    private static Logger log = Logger.getLogger(AuditExporterTest.class.getName());

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
        String quotedCombined = AuditExporter.quoteMeta(raw1) + ":" + AuditExporter.quoteMeta(raw2) + "\n";
        String expectedQuotedCombined = quo1 + ":" + quo2 + "\n";
        assertEquals(quotedCombined, expectedQuotedCombined);
    }

    public void testAuditExport() throws Exception {
        HibernatePersistenceManager.initialize(null);
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream("AuditExporterTest.zip");
            new AuditExporter().exportAuditsAsZipFile(fileOut,
                                                      TestDocuments.getDotNetServerCertificate(),
                                                      TestDocuments.getDotNetServerPrivateKey());
        } finally {
            if (fileOut != null) fileOut.close();
        }
    }
}
