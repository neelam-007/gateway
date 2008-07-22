package com.l7tech.server.admin;

import javax.xml.namespace.QName;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.XFireFactory;
import org.codehaus.xfire.aegis.type.TypeMapping;
import org.codehaus.xfire.aegis.AegisBindingProvider;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.invoker.ObjectInvoker;
import org.codehaus.xfire.service.binding.ObjectServiceFactory;

import com.l7tech.gateway.common.service.ServiceAdminPublic;
import com.l7tech.server.service.ServiceAdminImpl;
import com.l7tech.server.admin.ws.PolicyType;
import com.l7tech.policy.Policy;

/**
 * Test for admin web service.
 *
 * @author Steve Jones
 */
public class AdminWebServiceTest extends TestCase {

    /**
     *
     */
    public AdminWebServiceTest(String name) {
        super(name);
    }

    /**
     *
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(AdminWebServiceTest.class));
        return suite;
    }

    /**
     *
     */
    public void testWsdlGeneration() {
        XFire xfire = XFireFactory.newInstance().getXFire();

        ObjectServiceFactory factory = new ObjectServiceFactory(xfire.getTransportManager());
        Service service = factory.create(ServiceAdminPublic.class, "Service", null, null);
        service.setProperty(ObjectInvoker.SERVICE_IMPL_CLASS, ServiceAdminImpl.class);

        TypeMapping tm = ((AegisBindingProvider) service.getBindingProvider()).getTypeMapping(service);
        tm.register(Policy.class, new QName("http://types.l7tech.com", "policy"), new PolicyType());

        xfire.getServiceRegistry().register(service);
        xfire.generateWSDL("Service", System.out );
    }

}
