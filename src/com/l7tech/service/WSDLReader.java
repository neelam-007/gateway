package com.l7tech.service;

import java.util.Set;
import java.net.URL;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 */
public class WSDLReader {


    public static SOAPWebService[] serviceFromWSDL(String wsdlURL) throws javax.wsdl.WSDLException, java.net.MalformedURLException {
        javax.wsdl.factory.WSDLFactory fac = javax.wsdl.factory.WSDLFactory.newInstance();
        javax.wsdl.xml.WSDLReader reader = fac.newWSDLReader();
        javax.wsdl.Definition def = reader.readWSDL(wsdlURL);

        // initialize output
        java.util.Collection services = def.getServices().values();
        SOAPWebService[] out = new SOAPWebService[services.size()];

        // get each service
        int count = 0;
        java.util.Iterator i = services.iterator();
        while (i.hasNext()) {
            javax.wsdl.Service svc = (javax.wsdl.Service)i.next();
            out[count] = mapServiceToLocalType(svc);
            ++count;
        }

        return out;
    }

    private static SOAPWebService mapServiceToLocalType(javax.wsdl.Service svc) throws java.net.MalformedURLException {
        // what about service urn and name ?
        // svc.getQName().getNamespaceURI()  svc.getQName().getLocalPart()
        java.util.Set operations = extractOperations(svc);
        String url = extractURL(svc);
        return new SOAPWebService( "name", operations, new URL(url), svc.getQName().getNamespaceURI());
    }

    private static String extractURL(javax.wsdl.Service svc) {
        java.util.Collection porks = svc.getPorts().values();
        java.util.Iterator i = porks.iterator();
        while (i.hasNext()) {
            javax.wsdl.Port pork = (javax.wsdl.Port)i.next();
            java.util.List extElms = pork.getExtensibilityElements();
            // look for the http address in the pork
            for (int listcount = 0; listcount < extElms.size(); listcount++) {
                Object obj = extElms.get(listcount);

                if (obj instanceof javax.wsdl.extensions.soap.SOAPAddress) {
                    String serviceURI = ((javax.wsdl.extensions.soap.SOAPAddress)obj).getLocationURI();
                    return serviceURI;
                }
            }
        }
        System.err.println(" error - soapaddress not found in com.l7tech.service.WSDLReader.extractURL");
        return "";
    }

    private static java.util.Set extractOperations(javax.wsdl.Service svc) {
        java.util.Collection porks = svc.getPorts().values();
        java.util.Iterator i = porks.iterator();
        java.util.Set output = null;
        // what is there are more than one ports?
        while (i.hasNext()) {
            javax.wsdl.Port pork = (javax.wsdl.Port)i.next();
            javax.wsdl.Binding binding = pork.getBinding();
            java.util.Set operationsInThisPork = extractOperations(binding);
            if (output == null || output.size() < 1) output = operationsInThisPork;
            else break;
        }
        return output;
    }

    private static java.util.Set extractOperations(javax.wsdl.Binding binding) {
        java.util.Set output = new java.util.HashSet();
        java.util.List operations = binding.getPortType().getOperations();
        for (int i = 0; i < operations.size(); i++) {
            javax.wsdl.Operation op = (javax.wsdl.Operation)operations.get(i);
            // operation name op.getName()
            // operation urn? op.getInput().getMessage().getQName().getNamespaceURI()
            // todo, add operation to operations List once there is a way to construct an operation
        }
        return output;
    }


    /*
    // for testing only
    public static void main(String[] args) throws Exception {
        serviceFromWSDL("http://192.168.0.2:8080/simplewsdl.xml");
    }
    */

}
