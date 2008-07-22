package com.l7tech.server.tomcat;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.io.InputStream;

import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Document;

/**
 *
 */
public class ServerXmlParserTest extends TestCase {
    private static final Logger log = Logger.getLogger(ServerXmlParserTest.class.getName());

    public ServerXmlParserTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerXmlParserTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testLoadServerXml() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("com/l7tech/server/tomcat/resources/test-server.xml");
        if (is == null) throw new NullPointerException("missing test-server.xml (xml resources have not been copied to the build tree?)");
        Document doc = XmlUtil.parse(is);

        ServerXmlParser serverXml = new ServerXmlParser();
        serverXml.load(doc);

        List<Map<String,String>> connectors = serverXml.getConnectors();
        assertEquals(connectors.size(), 3);
        assertEquals("8080", connectors.get(0).get("port"));
        assertEquals("https", connectors.get(1).get("scheme"));
        assertEquals("false", connectors.get(2).get("clientAuth"));
        assertEquals("com.l7tech.server.tomcat.SsgSSLImplementation", connectors.get(2).get("SSLImplementation"));
    }
}
