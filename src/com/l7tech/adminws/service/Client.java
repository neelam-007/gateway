package com.l7tech.adminws.service;

import org.apache.axis.client.Call;

import javax.xml.rpc.ServiceException;
import javax.xml.namespace.QName;
import java.net.MalformedURLException;

import com.l7tech.service.PublishedService;
import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.adminws.AdminWSClientStub;
import com.l7tech.util.Locator;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 * This is the admin ws client for the service ws. It is used by the ServiceManagerClientImp class.
 */
public class Client extends AdminWSClientStub {
    public Client(String targetURL) {
        super(targetURL);
    }

    public String resolveWsdlTarget(String url) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(SERVICE_URN, "resolveWsdlTarget"));
		call.setReturnClass(String.class);
        call.addParameter(new javax.xml.namespace.QName("", "url"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, javax.xml.rpc.ParameterMode.IN);
        return (String)call.invoke(new Object[]{url});
    }

    public PublishedService findServiceByPrimaryKey(long oid) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(SERVICE_URN, "findServiceByPrimaryKey"));
        call.setReturnClass(PublishedService.class);
        call.addParameter(new javax.xml.namespace.QName("", "oid"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, javax.xml.rpc.ParameterMode.IN);
        return (PublishedService)call.invoke(new Object[]{new Long(oid)});
    }

    public com.l7tech.objectmodel.EntityHeader[] findAllPublishedServices() throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(SERVICE_URN, "findAllPublishedServices"));
		call.setReturnClass(com.l7tech.objectmodel.EntityHeader[].class);
        com.l7tech.objectmodel.EntityHeader[] output = (com.l7tech.objectmodel.EntityHeader[])call.invoke(new Object[]{});
        if (output == null) return new com.l7tech.objectmodel.EntityHeader[0];
        return output;
    }

    public com.l7tech.objectmodel.EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(SERVICE_URN, "findAllPublishedServicesByOffset"));
		call.setReturnClass(com.l7tech.objectmodel.EntityHeader[].class);
        call.addParameter(new javax.xml.namespace.QName("", "offset"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "windowSize"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
        com.l7tech.objectmodel.EntityHeader[] output = (com.l7tech.objectmodel.EntityHeader[])call.invoke(new Object[]{new java.lang.Integer(offset), new java.lang.Integer(windowSize)});
        if (output == null) return new com.l7tech.objectmodel.EntityHeader[0];
        return output;
    }

    /**
     * this save method handles both save and updates.
     * the distinction happens on the server side by inspecting the oid of the object
     * if the oid appears to be "virgin" a save is invoked, otherwise an update call is made.
     * @param service the object to be saved or updated
     * @throws java.rmi.RemoteException
     */
    public long savePublishedService(PublishedService service) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(SERVICE_URN, "savePublishedService"));
        call.addParameter(new javax.xml.namespace.QName("", "service"), new javax.xml.namespace.QName(SERVICE_URN, "PublishedService"), com.l7tech.service.PublishedService.class, javax.xml.rpc.ParameterMode.IN);
        call.setReturnClass(Long.class);
        Long res = (Long)call.invoke(new Object[]{service});
        return res.longValue();
    }

    public void deletePublishedService(long oid) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(SERVICE_URN, "deletePublishedService"));
        call.addParameter(new javax.xml.namespace.QName("", "oid"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, javax.xml.rpc.ParameterMode.IN);
        call.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        call.invoke(new Object[]{new java.lang.Long(oid)});
        return;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    protected void registerTypeMappings(Call call) {
        QName qn = new QName(SERVICE_URN, "PublishedService");
        call.registerTypeMapping(PublishedService.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(PublishedService.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(PublishedService.class, qn));
        qn = new QName(SERVICE_URN, "EntityHeader");
        call.registerTypeMapping(com.l7tech.objectmodel.EntityHeader.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.objectmodel.EntityHeader.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.objectmodel.EntityHeader.class, qn));
        qn = new QName(SERVICE_URN, "ArrayOfHeaders");
        call.registerTypeMapping(com.l7tech.objectmodel.EntityHeader[].class, qn, new org.apache.axis.encoding.ser.ArraySerializerFactory(), new org.apache.axis.encoding.ser.ArrayDeserializerFactory());
        qn = new QName(SERVICE_URN, "EntityType");
        call.registerTypeMapping(com.l7tech.objectmodel.EntityType.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.objectmodel.EntityType.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.objectmodel.EntityType.class, qn));
    }

    private static final String SERVICE_URN = "http://www.layer7-tech.com/service";
}
