package com.l7tech.adminws.service;

import org.apache.axis.client.Call;

import javax.xml.rpc.ServiceException;
import javax.xml.namespace.QName;
import java.net.MalformedURLException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 */
public class Client {
    public Client(String targetURL, String username, String password) {
        this.url = targetURL;
        this.username = username;
        this.password = password;
    }

    public String resolveWsdlTarget(String url) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(SERVICE_URN, "resolveWsdlTarget"));
		call.setReturnClass(String.class);
        call.addParameter(new javax.xml.namespace.QName("", "url"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, javax.xml.rpc.ParameterMode.IN);
        return (String)call.invoke(new Object[]{url});
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    public static void main(String[] args) throws Exception {
        Client me = new Client("http://localhost:8080/ssg/services/serviceAdmin", "ssgadmin", "ssgadminpasswd");
        System.out.println(me.resolveWsdlTarget("blah"));
    }

    private Call createStubCall() throws java.rmi.RemoteException {
        // create service, call
        org.apache.axis.client.Service service = new org.apache.axis.client.Service();
        // todo, should i reuse this object instead of re-instantiating every call?
        Call call = null;
        try {
            call = (Call)service.createCall();
            if (username != null && username.length() > 0 && password != null && password.length() > 0) {
                call.setUsername(username);
                call.setPassword(password);
            }
        } catch (ServiceException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        }
        try {
            call.setTargetEndpointAddress(new java.net.URL(url));
        } catch (MalformedURLException e) {
            throw new java.rmi.RemoteException(e.getMessage(), e);
        }
        registerTypeMappings(call);
        return call;
    }

    private void registerTypeMappings(Call call) {
        /*
        QName qn = new QName(IDENTITY_URN, "ArrayOfHeaders");
        call.registerTypeMapping(com.l7tech.objectmodel.EntityHeader[].class, qn, new org.apache.axis.encoding.ser.ArraySerializerFactory(), new org.apache.axis.encoding.ser.ArrayDeserializerFactory());
        qn = new QName(IDENTITY_URN, "EntityHeader");
        call.registerTypeMapping(com.l7tech.objectmodel.EntityHeader.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.objectmodel.EntityHeader.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.objectmodel.EntityHeader.class, qn));
        qn = new QName(IDENTITY_URN, "EntityType");
        call.registerTypeMapping(com.l7tech.objectmodel.EntityType.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.objectmodel.EntityType.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.objectmodel.EntityType.class, qn));
        qn = new QName(IDENTITY_URN, "IdentityProviderConfig");
        call.registerTypeMapping(com.l7tech.identity.imp.IdentityProviderConfigImp.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.identity.imp.IdentityProviderConfigImp.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.identity.imp.IdentityProviderConfigImp.class, qn));
        qn = new QName(IDENTITY_URN, "IdentityProviderConfigType");
        call.registerTypeMapping(com.l7tech.identity.imp.IdentityProviderTypeImp.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.identity.imp.IdentityProviderTypeImp.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.identity.imp.IdentityProviderTypeImp.class, qn));
        qn = new QName(IDENTITY_URN, "User");
        call.registerTypeMapping(com.l7tech.identity.internal.imp.UserImp.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.identity.internal.imp.UserImp.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.identity.internal.imp.UserImp.class, qn));
        qn = new QName(IDENTITY_URN, "Group");
        call.registerTypeMapping(com.l7tech.identity.internal.imp.GroupImp.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.identity.internal.imp.GroupImp.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.identity.internal.imp.GroupImp.class, qn));
        */
    }

    private static final String SERVICE_URN = "http://www.layer7-tech.com/service";
    private String url;
    private String username;
    private String password;
}
