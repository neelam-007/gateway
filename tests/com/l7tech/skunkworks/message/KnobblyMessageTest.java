package com.l7tech.skunkworks.message;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.xml.TestDocuments;

/**
 * @author alex
 * @version $Revision$
 */
public class KnobblyMessageTest extends TestCase {
    /**
     * test <code>KnobblyMessageTest</code> constructor
     */
    public KnobblyMessageTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * KnobblyMessageTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(KnobblyMessageTest.class);
        return suite;
    }

    public void testStuff() throws Exception {
        Message msg = new Message();
        MimeFacet mime = new MimeFacet(msg, new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, TestDocuments.getInputStream(TestDocuments.PLACEORDER_CLEARTEXT));
    }


    /**
     * Test <code>KnobblyMessageTest</code> main.
     */
    public static void main(String[] args) throws
            Throwable {
        junit.textui.TestRunner.run(suite());
    }
}