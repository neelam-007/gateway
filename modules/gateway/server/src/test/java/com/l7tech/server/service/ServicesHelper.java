package com.l7tech.server.service;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.common.TestDocuments;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The test class that helps preparing and publishing services in the
 * test mode
 *
 * @author emil
 * @version 21-Mar-2005
 */
public class ServicesHelper {
    private final ServiceAdmin serviceAdmin;

    public ServicesHelper(ServiceAdmin serviceAdmin) {
        this.serviceAdmin = serviceAdmin;
    }

    /**
     * Deletes all the test services
     *
     * @throws FindException   on find error
     * @throws DeleteException on delete error
     */
    public void deleteAllServices() throws FindException, DeleteException {
        EntityHeader[] headers = serviceAdmin.findAllPublishedServices();
        for (int i = 0; i < headers.length; i++) {
            EntityHeader header = headers[i];
            serviceAdmin.deletePublishedService(header.getStrId());
        }
    }

    /**
     * Publish the test service returning the <code>ServiceDescriptor</code>
     *
     * @param name         the service name
     * @param wsdlResource the wsdl resource to use
     * @param policy
     * @return the service descritpor corresponding to the published service
     */
    public ServiceDescriptor publish(String name, String wsdlResource, final Assertion policy)
            throws IOException, SAXException, SaveException, VersionException, UpdateException, PolicyAssertionException {

        ServiceDescriptor descriptor = new ServiceDescriptor(
                name, TestDocuments.getTestDocumentAsXml(wsdlResource), policy
        );
        PublishedService ps = new PublishedService();
        ps.setPolicy(new Policy(PolicyType.PRIVATE_SERVICE, null, WspWriter.getPolicyXml(policy), true));
        ps.setName(name);
        ps.setWsdlXml(descriptor.wsdlXml);
        serviceAdmin.savePublishedService(ps);
        return descriptor;
    }

    public ServiceDescriptor[] getAlllDescriptors() throws IOException, FindException {
        Collection descriptors = new ArrayList();
        EntityHeader[] headers = serviceAdmin.findAllPublishedServices();
        for (int i = 0; i < headers.length; i++) {
            EntityHeader header = headers[i];
            PublishedService service = serviceAdmin.findServiceByID(header.getStrId());
            descriptors.add(
                    new ServiceDescriptor(
                            service.getName(), service.getWsdlXml(), service.getPolicy().getAssertion()
                    )
            );
        }
        return (ServiceDescriptor[]) descriptors.toArray(new ServiceDescriptor[]{});
    }

    public static class ServiceDescriptor {
        final String name;
        final String wsdlXml;
        final Assertion policy;

        public ServiceDescriptor(String name, String wsdlXml, Assertion policy) {
            this.name = name;
            this.policy = policy;
            this.wsdlXml = wsdlXml;
        }

        public String getName() {
            return name;
        }

        public String getWsdlXml() {
            return wsdlXml;
        }

        public Assertion getPolicy() {
            return policy;
        }
    }

}
