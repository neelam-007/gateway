package com.l7tech.adminws.service;

import com.l7tech.service.PublishedService;

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
        PublishedService out = new PublishedService();
        out.setName("service name");
        out.setOid(oid);
        out.setPolicyXml("<this is not really xml>");
        out.setWsdlXml("<this is not really xml>");
        return out;
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
}
