package com.l7tech.server;

import com.l7tech.xml.soap.SoapMessageGenerator;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.server.transport.http.HttpTransportModuleTester;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * Test mock servlet API.
 */
public class MockServletApiTest {
    private SoapMessageProcessingServlet messageProcessingServlet;
    private ServiceAdmin serviceAdmin;
    private SoapMessageGenerator.Message[] soapRequests;
    private PublishedService publishedService;
    private static MockServletApi servletApi;

    @BeforeClass
    public static void setUpClass() throws Exception {
        servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/server/resources/testApplicationContext.xml");
    }

    @Before
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


    /**
     * todo: fix the mock spring servlet init
     * @throws Exception
     */
    @Test
    public void testInvokeMessageProcessingServlet() throws Exception {
        HttpTransportModuleTester.setGlobalConnector(new SsgConnector() {
            public boolean offersEndpoint(Endpoint endpoint) {
                return true;
            }
        });

        for (int i = 0; i < soapRequests.length; i++) {
            SoapMessageGenerator.Message soapRequest = soapRequests[i];
            servletApi.reset();
            servletApi.setPublishedService(publishedService);
            servletApi.setSoapRequest(soapRequest.getSOAPMessage(), soapRequest.getSOAPAction() == null ? "" : soapRequest.getSOAPAction());
            HttpServletRequest mhreq = servletApi.getServletRequest();
            MockHttpServletResponse mhres = servletApi.getServletResponse();
            messageProcessingServlet = new SoapMessageProcessingServlet();
            messageProcessingServlet.init(servletApi.getServletConfig());
            messageProcessingServlet.doPost(mhreq, mhres);
        }
    }

}
