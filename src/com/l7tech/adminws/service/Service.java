package com.l7tech.adminws.service;

import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.EntityType;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 */
public class Service {

    public Service() {
    }
    public String resolveWsdlTarget(String url) throws java.rmi.RemoteException {
        try {
            java.net.URL urltarget = new java.net.URL(url);
            java.net.URLConnection connection = urltarget.openConnection();
            java.io.InputStream in = connection.getInputStream();
            byte[] buffer = new byte[2048];
            int read = in.read(buffer);
            StringBuffer out = new StringBuffer();
            while (read > 0) {
                out.append(new String(buffer, 0, read));
                read = in.read(buffer);
            }
            return out.toString();
        } catch (java.io.IOException e) {
            e.printStackTrace(System.err);
            throw new java.rmi.RemoteException("com.l7tech.adminws.service.Service cannot resolve WSDL " + e.getMessage(), e);
        }
    }

    public PublishedService findServiceByPrimaryKey(long oid) throws java.rmi.RemoteException {
        // todo, remove this test code and replace with call to server-side manager
        return createTestPubService();
    }

    public com.l7tech.objectmodel.EntityHeader[] findAllPublishedServices() throws java.rmi.RemoteException {
        // todo, remove this test code and replace with call to server-side manager
        return createTestHeaders();
    }

    public com.l7tech.objectmodel.EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
        // todo, remove this test code and replace with call to server-side manager
        return createTestHeaders();
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    /*
    public static void main (String[] args) throws Exception {
        Service me = new Service();
        System.out.println(me.resolveWsdlTarget("http://192.168.0.2:8080/simplewsdl.xml"));
    }
    */
    private PublishedService createTestPubService() {
        PublishedService out = new PublishedService();
        out.setName("service name");
        out.setOid(132);
        out.setPolicyXml("<this is not really xml>");
        out.setWsdlXml("<this is not really xml>");
        return out;
    }

    private com.l7tech.objectmodel.EntityHeader[] createTestHeaders() {
        com.l7tech.objectmodel.EntityHeader[] out = new com.l7tech.objectmodel.EntityHeader[3];
        out[0] = new com.l7tech.objectmodel.EntityHeader(1, EntityType.SERVICE, "name", "description");
        out[1] = new com.l7tech.objectmodel.EntityHeader(2, EntityType.SERVICE, "name", "description");
        out[2] = new com.l7tech.objectmodel.EntityHeader(3, EntityType.SERVICE, "name", "description");
        return out;
    }
}
