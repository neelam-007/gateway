package com.l7tech.server;

import com.l7tech.common.util.Locator;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.security.Keys;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;
import com.mockobjects.servlet.MockHttpServletResponse;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.servlet.http.HttpServletRequest;

/**
 * Test mock servlet API.
 */
public class MockServletApiTest extends TestCase {
    private SoapMessageProcessingServlet messageProcessingServlet;
    private ServiceAdmin serviceAdmin;
    private SoapMessageGenerator.Message[] soapRequests;
    private PublishedService publishedService;

    /**
     * test <code>MockServletApiTest</code> constructor
     */
    public MockServletApiTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * ServerPolicyFactoryTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(MockServletApiTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            /**
             * sets the test environment
             * 
             * @throws Exception on error deleting the stub data store
             */
            protected void setUp() throws Exception {
                Keys.createTestSsgKeystoreProperties();
                System.setProperty("com.l7tech.common.locator.properties", "/com/l7tech/common/locator/test.properties");
            }

            protected void tearDown() throws Exception {
                ;
            }
        };
        return wrapper;
    }

    public void setUp() throws Exception {
        serviceAdmin = (ServiceAdmin)Locator.getDefault().lookup(ServiceAdmin.class);
        if (serviceAdmin == null) {
            throw new AssertionError("could not carry out admins is null");
        }
        EntityHeader[] headers = serviceAdmin.findAllPublishedServices();
        assertTrue("no services have been returned could not execute test", headers.length > 0);
        publishedService = serviceAdmin.findServiceByPrimaryKey(headers[0].getOid());
        Wsdl wsdl = publishedService.parsedWsdl();

        SoapMessageGenerator sg = new SoapMessageGenerator();
        soapRequests = sg.generateRequests(wsdl);
        assertTrue("no operations could be located in the wsdlt", soapRequests.length > 0);
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testInvokeMessageProcessingServlet() throws Exception {
        for (int i = 0; i < soapRequests.length; i++) {
            MockServletApi servletApi = MockServletApi.defaultMessageProcessingServletApi();
            SoapMessageGenerator.Message soapRequest = soapRequests[i];
            servletApi.setPublishedService(publishedService);
            servletApi.setSoapRequest(soapRequest.getSOAPMessage(), soapRequest.getSOAPAction());
            HttpServletRequest mhreq = servletApi.getServletRequest();
            MockHttpServletResponse mhres = new MockHttpServletResponse();
            messageProcessingServlet = new SoapMessageProcessingServlet();
            messageProcessingServlet.init(servletApi.getServletConfig());
            messageProcessingServlet.doPost(mhreq, mhres);
        }
    }

    /**
     * Test <code>ServerPolicyFactoryTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
