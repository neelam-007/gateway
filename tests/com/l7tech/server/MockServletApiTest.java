package com.l7tech.server;

import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * Test mock servlet API.
 */
public class MockServletApiTest extends TestCase {
    private SoapMessageProcessingServlet messageProcessingServlet;
    private ServiceAdmin serviceAdmin;
    private SoapMessageGenerator.Message[] soapRequests;
    private PublishedService publishedService;
    private static MockServletApi servletApi;

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
                servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/common/testApplicationContext.xml");
            }

            protected void tearDown() throws Exception {
                ;
            }
        };
        return wrapper;
    }

    public void setUp() throws Exception {
        ApplicationContext context = servletApi.getApplicationContext();
        serviceAdmin = (ServiceAdmin)context.getBean("serviceAdmin");
        EntityHeader[] headers = serviceAdmin.findAllPublishedServices();
        assertTrue("no services have been returned could not execute test", headers.length > 0);
        publishedService = serviceAdmin.findServiceByID(headers[0].getStrId());
        Wsdl wsdl = publishedService.parsedWsdl();

        SoapMessageGenerator sg = new SoapMessageGenerator();
        soapRequests = sg.generateRequests(wsdl);
        assertTrue("no operations could be located in the wsdlt", soapRequests.length > 0);
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * todo: fix the mock spring servlet init
     * @throws Exception
     */
    public void testInvokeMessageProcessingServlet() throws Exception {
        for (int i = 0; i < soapRequests.length; i++) {
            SoapMessageGenerator.Message soapRequest = soapRequests[i];
            servletApi.reset();
            servletApi.setPublishedService(publishedService);
            servletApi.setSoapRequest(soapRequest.getSOAPMessage(), soapRequest.getSOAPAction());
            HttpServletRequest mhreq = servletApi.getServletRequest();
            MockHttpServletResponse mhres = servletApi.getServletResponse();
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
