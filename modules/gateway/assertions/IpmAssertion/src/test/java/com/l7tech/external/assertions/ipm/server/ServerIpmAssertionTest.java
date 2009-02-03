package com.l7tech.external.assertions.ipm.server;

import com.l7tech.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.IOUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.ipm.IpmAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.Enumeration;
import java.util.Collections;
import java.net.URL;

/**
 * Test the IpmAssertion.
 */
public class ServerIpmAssertionTest extends TestCase {
    private static final Logger log = Logger.getLogger(ServerIpmAssertionTest.class.getName());

    static final String RESOURCE_DIR = "com/l7tech/external/assertions/ipm/resources/";
    static final String TEMPLATE_PAC_REPLY = "template-pac-reply.xml";
    static final String SOAP_PAC_REPLY = "soap-pac-reply.xml";

    public ServerIpmAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerIpmAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }


    static String loadFile(String filename) throws IOException {
        InputStream is = ServerIpmAssertionTest.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + filename);
        byte[] got = IOUtils.slurpStream(is);
        return new String(got);
    }

    public void testUnpackToVariable() throws Exception {
        String template = loadFile(TEMPLATE_PAC_REPLY);
        String requestStr = loadFile(SOAP_PAC_REPLY);

        IpmAssertion ass = new IpmAssertion();
        ass.template(template);
        ass.setSourceVariableName("databuff");
        ass.setTargetVariableName("ipmresult");

        ServerIpmAssertion sass = new ServerIpmAssertion(ass, null);

        Message request = new Message();
        request.initialize(ContentTypeHeader.XML_DEFAULT, requestStr.getBytes("UTF-8"));
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setVariable("databuff", extractDataBuff(requestStr));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        String ipmresult = context.getVariable("ipmresult").toString();
        assertTrue(ipmresult.length() > 0);
        XmlUtil.stringToDocument(ipmresult);
    }
    
    public void testUnpackToMessage() throws Exception {
        doUnpackToMessage();
    }

    private static void doUnpackToMessage() throws Exception {
        String template = loadFile(TEMPLATE_PAC_REPLY);
        String requestStr = loadFile(SOAP_PAC_REPLY);

        IpmAssertion ass = new IpmAssertion();
        ass.template(template);
        ass.setSourceVariableName("databuff");
        ass.setTargetVariableName(null);
        ass.setUseResponse(true);

        ServerIpmAssertion sass = new ServerIpmAssertion(ass, null);

        Message request = new Message();
        request.initialize(ContentTypeHeader.XML_DEFAULT, requestStr.getBytes("UTF-8"));
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setVariable("databuff", extractDataBuff(requestStr));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        String ipmresult = new String(IOUtils.slurpStream(response.getMimeKnob().getFirstPart().getInputStream(false)));
        assertTrue(ipmresult.length() > 0);
        XmlUtil.stringToDocument(ipmresult);
    }

    static String extractDataBuff(String requestSoapStr) throws SAXException {
        Document doc = XmlUtil.stringToDocument(requestSoapStr);
        NodeList found = doc.getElementsByTagNameNS("http://soapam.com/service/Pacquery/", "DATA_BUFF");
        int numFound = found.getLength();
        if (numFound < 1) throw new SAXException("Did not find a DATA_BUFF element");
        if (numFound > 1) throw new SAXException("Found more than one DATA_BUFF element");
        return found.item(0).getTextContent();
    }

    /**
     * Filtering class loader that will delegate to its parent for all but one
     * package prefix.
     */
    private static class FilterClassLoader extends ClassLoader {

        /**
         * Create a filter classloader with the given parent and filter.
         *
         * <p>The parent is not delegated to in the usual manner. Only requests to
         * load classes / resources that are not "under" the filter prefix are delegated
         * to the parent.</p>
         *
         * @param parent the classloader to delegate to for matching classes/resources
         * @param filter the package/resource prefix for non delegated classes/resources
         */
        public FilterClassLoader(ClassLoader parent, String filter) {
            super();
            if (parent == null) throw new IllegalArgumentException("parent must not be null.");
            resourcePrefix = asResource(filter);
            filteredParent = parent;
        }

        //- PROTECTED

        /**
         *
         */
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (propagate(asResource(name))) {
                return filteredParent.loadClass(name);
            }
            throw new ClassNotFoundException(name);
        }

        /**
         *
         */
        @Override
        protected URL findResource(String name) {
            if (propagate(asResource(name))) {
                return filteredParent.getResource(name);
            }
            return null;
        }

        /**
         *
         */
        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            if (propagate(asResource(name))) {
                return filteredParent.getResources(name);
            }
            return Collections.enumeration( Collections.<URL>emptyList() );
        }

        //- PRIVATE

        private final ClassLoader filteredParent;
        private final String resourcePrefix;

        /**
         * Convert a classes binary name to a resource path
         *
         * @return the /resource/path
         */
        private String asResource(String pathOrClassName) {
            String resource = null;

            if (pathOrClassName != null) {
                String res = pathOrClassName.replace('.', '/');
                if (!res.startsWith("/")) {
                    res = "/" + res;
                }
                resource = res;
            }

            return resource;
        }

        /**
         * Check if the request should be passed to the parent.
         *
         * @param resourcePath The path to check
         * @return true to delegate
         */
        private boolean propagate(String resourcePath) {
            return resourcePrefix != null && !resourcePath.startsWith(resourcePrefix);
        }
    }
}
