package com.l7tech.adminws.logging;

import org.apache.axis.client.Call;

import javax.xml.rpc.ServiceException;
import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

/**
 * Layer 7 Technologies, inc.
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 12:59:51 PM
 *
 */
public class Client {

    public Client(String targetURL, String username, String password) {
        this.url = targetURL;
        this.username = username;
        this.password = password;
    }

    public String[] getSystemLog(int offset, int size) throws RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(LOGGING_URN, "getSystemLog"));
        call.addParameter(new javax.xml.namespace.QName("", "offset"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "size"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
		call.setReturnClass(String[].class);
        return (String[])call.invoke(new Object[]{new Integer(offset), new Integer(size)});
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private Call createStubCall() throws java.rmi.RemoteException {
        if (sessionCall != null) {
            sessionCall.clearOperation();
            //sessionCall.clearHeaders();
            //sessionCall.removeAllParameters();
            return sessionCall;
        }
        // create service, call
        org.apache.axis.client.Service service = new org.apache.axis.client.Service();
        Call call = null;
        try {
            call = (Call)service.createCall();
            if (username != null && username.length() > 0 && password != null && password.length() > 0) {
                call.setUsername(username);
                call.setPassword(password);
            }
            call.setMaintainSession(true);
        } catch (ServiceException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        }
        try {
            call.setTargetEndpointAddress(new java.net.URL(url));
        } catch (MalformedURLException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        }
        registerTypeMappings(call);
        sessionCall = call;
        return call;
    }

    private void registerTypeMappings(Call call) {
        QName qn = new QName(LOGGING_URN, "ArrayOfStrings");
        call.registerTypeMapping(String[].class, qn, new org.apache.axis.encoding.ser.ArraySerializerFactory(), new org.apache.axis.encoding.ser.ArrayDeserializerFactory());
    }

    private String url;
    private String username;
    private String password;
    private static final String LOGGING_URN = "http://www.layer7-tech.com/logging";
    // this static object is meant to maintain a session
    // so that not all calls result in an authentication operation
    // on the other side
    private static Call sessionCall = null;
}
