/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Test DOM parsing cost vs. cloning cost
 */
public class DomPerfTest {
    private static final Logger logger = Logger.getLogger(DomPerfTest.class.getName());

    public static void main(String[] args) throws Exception {
        final Document origDoc = TestDocuments.getTestDocument(TestDocuments.DOTNET_SIGNED_REQUEST);
        final String origXml = XmlUtil.nodeToString(origDoc);
        logger.info("Test document size = " + origXml.length());
        
        Runnable testClone = new Runnable() {
            public void run() {
                Document newDoc = (Document)origDoc.cloneNode(true);
            }
        };
        BenchmarkRunner benchClone = new BenchmarkRunner(testClone, 20000, "testClone");
        benchClone.setThreadCount(1);
        benchClone.run();

        Runnable testParse = new Runnable() {
            public void run() {
                try {
                    Document newDoc = XmlUtil.stringToDocument(origXml);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (SAXException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        BenchmarkRunner benchParse = new BenchmarkRunner(testParse, 20000, "testParse");
        benchParse.setThreadCount(1);
        benchParse.run();
    }
}
