package com.l7tech.manager.automator;

import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.PublishedService;

import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import java.io.FileInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 21-Apr-2008
 * Time: 10:46:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceManager {
    private ServiceAdmin serviceAdmin;

    public ServiceManager(ServiceAdmin serviceAdmin) {
        this.serviceAdmin = serviceAdmin;
    }

    /**
     * Creates a new published service using the provided paramaters.
     * @param name The name of the new service
     * @param routingUri The routing URI that the new service should use
     * @param laxResolution Whether the service should accept all soap operations or not
     * @param wsdlXml The WSDL as a string
     * @param policyXml The policy as a string
     * @throws Exception
     */
    private void addServiceResolutionService(String name, String routingUri, boolean laxResolution, String wsdlXml, String policyXml)
    throws Exception {
        PublishedService publishedService = new PublishedService();
        Set<String> httpMethods = new HashSet<String>(1);
        httpMethods.add("POST");
        publishedService.setHttpMethods(httpMethods);
        if(routingUri != null) {
            publishedService.setRoutingUri(routingUri);
        }
        publishedService.setLaxResolution(laxResolution);
        publishedService.setWsdlUrl(null);
        publishedService.setWsdlXml(wsdlXml);
        publishedService.setName(name);
        publishedService.getPolicy().setXml(policyXml);

        serviceAdmin.savePublishedService(publishedService);
    }

    /**
     * Loads the contents of the specified file into a string and returns the string.
     * @param filename The name of the file to read
     * @return The contents of the file
     * @throws IOException
     */
    private String loadXmlFromFile(String filename) throws IOException {
        FileInputStream fis = new FileInputStream(filename);
        StringBuilder retVal = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        while(bytesRead >= 0) {
            bytesRead = fis.read(buffer);
            if(bytesRead > 0) {
                retVal.append(new String(buffer, 0, bytesRead, "UTF-8"));
            }
        }
        fis.close();

        return retVal.toString();
    }

    /**
     * Creates the published services that are used for measuring service resolution performance.
     */
    public void addServiceResolutionServices() {
        String wsdlXml = null;
        String policyXml = null;

        try {
            wsdlXml = loadXmlFromFile("etc/service_resolution/service_resolution2.wsdl");
            policyXml = loadXmlFromFile("etc/service_resolution/service_resolution2_policy.xml");
            addServiceResolutionService("ServiceResolution2", null, false, wsdlXml, policyXml);
        } catch(Exception e) {
            System.err.println("Failed to create service resolution 2 service");
            e.printStackTrace();
        }

        try {
            wsdlXml = loadXmlFromFile("etc/service_resolution/service_resolution.wsdl");
            policyXml = loadXmlFromFile("etc/service_resolution/service_resolution_policy.xml");
            addServiceResolutionService("ServiceResolution", "/serviceresolution", true, wsdlXml, policyXml);
        } catch(Exception e) {
            System.err.println("Failed to create service resolution (URI resolution) service");
            e.printStackTrace();
        }

        try {
            wsdlXml = loadXmlFromFile("etc/service_resolution/service_resolution.wsdl");
            policyXml = loadXmlFromFile("etc/service_resolution/service_resolution_policy.xml");
            addServiceResolutionService("ServiceResolution", null, false, wsdlXml, policyXml);
        } catch(Exception e) {
            System.err.println("Failed to create service resolution service");
            e.printStackTrace();
        }
    }
}
