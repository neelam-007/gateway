package com.l7tech.server.admin;

import javax.xml.namespace.QName;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import com.l7tech.gateway.common.service.ServiceAdminPublic;
import com.l7tech.server.service.ServiceAdminImpl;
import com.l7tech.server.admin.ws.PolicyType;
import com.l7tech.policy.Policy;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.ServiceInfo;

import java.util.List;

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
        //TODO figure out a way to instantiate service w/out requiring a container (jetty or otherwise)
//        System.setProperty("org.apache.cxf.nofastinfoset", "true");
//        ReflectionServiceFactoryBean serviceFactory = new ReflectionServiceFactoryBean();
//        serviceFactory.setServiceClass(ServiceAdminPublic.class);
//
//
//        ServerFactoryBean svrFactory = new ServerFactoryBean(serviceFactory);
//        svrFactory.setAddress("http://localhost:8080/admin/services/Service");
//        svrFactory.setServiceBean(ServiceAdminImpl.class);
//
//        svrFactory.setStart(false);
//
//        Service service = svrFactory.create().getEndpoint().getService();

//        TypeMapping tm = ((AegisDatabinding)service.getDataBinding()).getAegisContext().getTypeMapping();
//        tm.register(Policy.class, new QName("http://types.l7tech.com", "policy"), new PolicyType());
//
//        List<ServiceInfo> serviceInfos = service.getServiceInfos();
//        System.out.println("Size: " + serviceInfos.size());
        



//        XFire xfire = XFireFactory.newInstance().getXFire();
//
//        ObjectServiceFactory factory = new ObjectServiceFactory(xfire.getTransportManager());
//        Service service = factory.create(ServiceAdminPublic.class, "Service", null, null);
//        service.setProperty(ObjectInvoker.SERVICE_IMPL_CLASS, ServiceAdminImpl.class);
//
//        TypeMapping tm = ((AegisBindingProvider) service.getBindingProvider()).getTypeMapping(service);
//        tm.register(Policy.class, new QName("http://types.l7tech.com", "policy"), new PolicyType());
//
//        xfire.getServiceRegistry().register(service);
//        xfire.generateWSDL("Service", System.out );
    }

}
