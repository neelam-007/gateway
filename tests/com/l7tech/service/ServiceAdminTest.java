package com.l7tech.service;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.xml.Wsdl;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 10:22:59 AM
 * To change this template use Options | File Templates.
 */
public class ServiceAdminTest extends TestCase {

    static ApplicationContext applicationContext = null;
    PublishedService service;

    /**
     * test <code>AbstractLocatorTest</code> constructor
     */
    public ServiceAdminTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * LogCLientTest <code>TestCase</code>
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite(ServiceAdminTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            /**
             * test setup that deletes the stub data store; will trigger
             * store recreate
             * sets the environment
             * @throws Exception on error deleting the stub data store
             */
            protected void setUp() throws Exception {
                applicationContext = ApplicationContexts.getTestApplicationContext();

            }

            protected void tearDown() throws Exception {
                ;
            }
        };
        return wrapper;
    }

    public void testCreateService() throws Exception {
        System.out.println("creating service");
        PublishedService originalService = new PublishedService();
        String differentStringEverytime = Long.toString(System.currentTimeMillis());
        Wsdl wsdl = getTestWsdl(differentStringEverytime);
        originalService.setName(wsdl.getDefinition().getTargetNamespace());
        StringWriter sw = new StringWriter();
        wsdl.toWriter(sw);

        originalService.setPolicyXml("<test" + differentStringEverytime + "/>");
        originalService.setWsdlUrl("http://test" + differentStringEverytime + "?wsdl");
        originalService.setWsdlXml(sw.toString());
        System.out.println("saving service");
        ServiceAdmin sadmin = (ServiceAdmin)applicationContext.getBean("serviceAdmin");
        long res = sadmin.savePublishedService(originalService);
        System.out.println("saved with id=" + res);
    }

    private Wsdl getTestWsdl(String append) throws FileNotFoundException, WSDLException {
        // relative path from $SRC_ROOT
        String wsdlFile = "tests/com/l7tech/service/resources/StockQuoteService.wsdl";
        Wsdl wsdl = Wsdl.newInstance(null, new FileReader(wsdlFile));

        String targetNS =
                wsdl.getDefinition().getTargetNamespace() + "test" + append;
        wsdl.getDefinition().setTargetNamespace(targetNS);

        for (Iterator bindings = wsdl.getBindings().iterator(); bindings.hasNext();) {
            Binding b = (Binding) bindings.next();
            for (Iterator operations = b.getBindingOperations().iterator(); operations.hasNext();) {
                BindingOperation bindingOperation = (BindingOperation) operations.next();

                BindingInput input = bindingOperation.getBindingInput();
                Iterator eels = input.getExtensibilityElements().iterator();
                ExtensibilityElement ee;
                while (eels.hasNext()) {
                    ee = (ExtensibilityElement) eels.next();
                    if (ee instanceof SOAPBody) {
                        SOAPBody body = (SOAPBody) ee;
                        body.setNamespaceURI(body.getNamespaceURI() + "test" + append);
                    } else  if ( ee instanceof SOAPOperation ) {
                        SOAPOperation sop = (SOAPOperation)ee;
                        sop.setSoapActionURI(sop.getSoapActionURI() + "test" + append);
                    }
                }
            }
        }

        Port wsdlPort = wsdl.getSoapPort();
        List elements = wsdlPort.getExtensibilityElements();
        ExtensibilityElement ee;
        for (int i = 0; i < elements.size(); i++) {
            ee = (ExtensibilityElement) elements.get(i);
            if (ee instanceof SOAPAddress) {
                SOAPAddress sadd = (SOAPAddress) ee;
                sadd.setLocationURI(sadd.getLocationURI() + "test" + append);
            }
        }

        return wsdl;
    }

    public void xtestCreateServices() throws Exception {
        for (int i = 0; i < 50; i++) {
            testCreateService();
        }
    }

    /**
     * Test <code>AbstractLocatorTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
