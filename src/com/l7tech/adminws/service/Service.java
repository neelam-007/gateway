package com.l7tech.adminws.service;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 */
public class Service {

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

    /*
    public static void main (String[] args) throws Exception {
        Service me = new Service();
        System.out.println(me.resolveWsdlTarget("http://192.168.0.2:8080/simplewsdl.xml"));
    }
    */
}
