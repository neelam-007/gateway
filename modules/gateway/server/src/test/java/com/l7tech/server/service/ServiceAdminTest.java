package com.l7tech.server.service;

import com.l7tech.common.TestDocuments;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.wsdl.Wsdl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
public class ServiceAdminTest {

    static ApplicationContext applicationContext = null;
    PublishedService service;

    @BeforeClass
    public static void setUp() throws Exception {
        applicationContext = ApplicationContexts.getTestApplicationContext();
    }

    @Test
    public void testCreateService() throws Exception {
        System.out.println("creating service");
        PublishedService originalService = new PublishedService();
        String differentStringEverytime = Long.toString(System.currentTimeMillis());
        Wsdl wsdl = getTestWsdl(differentStringEverytime);
        originalService.setName(wsdl.getDefinition().getTargetNamespace());
        StringWriter sw = new StringWriter();
        wsdl.toWriter(sw);

        originalService.getPolicy().setXml("<test" + differentStringEverytime + "/>");
        originalService.setWsdlUrl("http://test" + differentStringEverytime + "?wsdl");
        originalService.setWsdlXml(sw.toString());
        System.out.println("saving service");
        ServiceAdmin sadmin = (ServiceAdmin)applicationContext.getBean("serviceAdmin");
        Goid res = sadmin.savePublishedService(originalService);
        System.out.println("saved with id=" + res);
    }

    private Wsdl getTestWsdl(String append) throws FileNotFoundException, WSDLException {
        // relative path from $SRC_ROOT
        InputStream in = TestDocuments.getInputStream( TestDocuments.WSDL );
        Wsdl wsdl = Wsdl.newInstance(null, new InputStreamReader(in));

        String targetNS =
                wsdl.getDefinition().getTargetNamespace() + "test" + append;
        wsdl.getDefinition().setTargetNamespace(targetNS);

        for (Binding b : wsdl.getBindings()) {
            //noinspection unchecked
            final List<BindingOperation> bops = b.getBindingOperations();
            for (BindingOperation bindingOperation : bops) {
                BindingInput input = bindingOperation.getBindingInput();
                Iterator eels = input.getExtensibilityElements().iterator();
                ExtensibilityElement ee;
                while (eels.hasNext()) {
                    ee = (ExtensibilityElement) eels.next();
                    if (ee instanceof SOAPBody) {
                        SOAPBody body = (SOAPBody) ee;
                        body.setNamespaceURI(body.getNamespaceURI() + "test" + append);
                    } else if (ee instanceof SOAPOperation) {
                        SOAPOperation sop = (SOAPOperation) ee;
                        sop.setSoapActionURI(sop.getSoapActionURI() + "test" + append);
                    }
                }
            }
        }

        Port wsdlPort = wsdl.getSoapPort();
        //noinspection unchecked
        List<ExtensibilityElement> elements = wsdlPort.getExtensibilityElements();
        for (ExtensibilityElement ee : elements) {
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
}
