package com.l7tech.adminws.identity;

import org.apache.axis.client.Call;
import javax.xml.namespace.QName;
import com.l7tech.adminws.AdminWSClientStub;

import java.io.IOException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 26, 2003
 *
 */
public class Client extends AdminWSClientStub {

    public Client() {
        super();
    }
    public String echoVersion() throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "echoVersion"));
		call.setReturnClass(String.class);
        return (String)call.invoke(new Object[]{});
    }
    public com.l7tech.objectmodel.EntityHeader[] findAllIdentityProviderConfig() throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "findAllIdentityProviderConfig"));
		call.setReturnClass(com.l7tech.objectmodel.EntityHeader[].class);
        com.l7tech.objectmodel.EntityHeader[] output = (com.l7tech.objectmodel.EntityHeader[])call.invoke(new Object[]{});
        if (output == null) return new com.l7tech.objectmodel.EntityHeader[0];
        return output;
    }
    public com.l7tech.objectmodel.EntityHeader[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "findAllIdentityProviderConfigByOffset"));
		call.setReturnClass(com.l7tech.objectmodel.EntityHeader[].class);
        call.addParameter(new javax.xml.namespace.QName("", "offset"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "windowSize"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
        com.l7tech.objectmodel.EntityHeader[] output = (com.l7tech.objectmodel.EntityHeader[])call.invoke(new Object[]{new java.lang.Integer(offset), new java.lang.Integer(windowSize)});
        if (output == null) return new com.l7tech.objectmodel.EntityHeader[0];
        return output;
    }
    public com.l7tech.identity.IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "findIdentityProviderConfigByPrimaryKey"));
        return (com.l7tech.identity.IdentityProviderConfig)call.invoke(new Object[]{new java.lang.Long(oid)});
    }
    public long saveIdentityProviderConfig(com.l7tech.identity.IdentityProviderConfig identityProviderConfig) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "saveIdentityProviderConfig"));
        Long res = (Long)call.invoke(new Object[]{identityProviderConfig});
        return res.longValue();

    }
    public void deleteIdentityProviderConfig(long oid) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "deleteIdentityProviderConfig"));
        call.addParameter(new javax.xml.namespace.QName("", "oid"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, javax.xml.rpc.ParameterMode.IN);
        call.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        call.invoke(new Object[]{new java.lang.Long(oid)});
        return;
    }
    public com.l7tech.objectmodel.EntityHeader[] findAllUsers(long identityProviderConfigId) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "findAllUsers"));
        call.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, javax.xml.rpc.ParameterMode.IN);
		call.setReturnClass(com.l7tech.objectmodel.EntityHeader[].class);
        com.l7tech.objectmodel.EntityHeader[] output = (com.l7tech.objectmodel.EntityHeader[])call.invoke(new Object[]{new java.lang.Long(identityProviderConfigId)});
        if (output == null) return new com.l7tech.objectmodel.EntityHeader[0];
        return output;
    }
    public com.l7tech.objectmodel.EntityHeader[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "findAllUsersByOffset"));
        call.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "offset"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "windowSize"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
		call.setReturnClass(com.l7tech.objectmodel.EntityHeader[].class);
        com.l7tech.objectmodel.EntityHeader[] output = (com.l7tech.objectmodel.EntityHeader[])call.invoke(new Object[]{new java.lang.Long(identityProviderConfigId), new java.lang.Integer(offset), new java.lang.Integer(windowSize)});
        if (output == null) return new com.l7tech.objectmodel.EntityHeader[0];
        return output;
    }
    public com.l7tech.identity.User findUserByPrimaryKey(long identityProviderConfigId, String userId) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "findUserByPrimaryKey"));
        return (com.l7tech.identity.User)call.invoke(new Object[]{new java.lang.Long(identityProviderConfigId), userId});
    }
    public void deleteUser(long identityProviderConfigId, String userId) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "deleteUser"));
        call.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "userId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, javax.xml.rpc.ParameterMode.IN);
        call.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        call.invoke(new Object[]{new java.lang.Long(identityProviderConfigId), userId});
    }
    public long saveUser(long identityProviderConfigId, com.l7tech.identity.User user) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "saveUser"));
        call.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "user"), new javax.xml.namespace.QName(IDENTITY_URN, "User"), com.l7tech.identity.User.class, javax.xml.rpc.ParameterMode.IN);
        call.setReturnClass(Long.class);
        Long res = (Long)call.invoke(new Object[]{new java.lang.Long(identityProviderConfigId), user});
        return res.longValue();
    }
    public com.l7tech.objectmodel.EntityHeader[] findAllGroups(long identityProviderConfigId) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "findAllGroups"));
        call.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, javax.xml.rpc.ParameterMode.IN);
		call.setReturnClass(com.l7tech.objectmodel.EntityHeader[].class);
        com.l7tech.objectmodel.EntityHeader[] output = (com.l7tech.objectmodel.EntityHeader[])call.invoke(new Object[]{new java.lang.Long(identityProviderConfigId)});
        if (output == null) return new com.l7tech.objectmodel.EntityHeader[0];
        return output;
    }
    public com.l7tech.objectmodel.EntityHeader[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "findAllGroupsByOffset"));
        call.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "offset"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "windowSize"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, javax.xml.rpc.ParameterMode.IN);
		call.setReturnClass(com.l7tech.objectmodel.EntityHeader[].class);
        com.l7tech.objectmodel.EntityHeader[] output = (com.l7tech.objectmodel.EntityHeader[])call.invoke(new Object[]{new java.lang.Long(identityProviderConfigId), new java.lang.Integer(offset), new java.lang.Integer(windowSize)});
        if (output == null) return new com.l7tech.objectmodel.EntityHeader[0];
        return output;
    }
    public com.l7tech.identity.Group findGroupByPrimaryKey(long identityProviderConfigId, String groupId) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "findGroupByPrimaryKey"));
        return (com.l7tech.identity.Group)call.invoke(new Object[]{new java.lang.Long(identityProviderConfigId), groupId});
    }
    public void deleteGroup(long identityProviderConfigId, String groupId) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "deleteGroup"));
        call.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "groupId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, javax.xml.rpc.ParameterMode.IN);
        call.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        call.invoke(new Object[]{new java.lang.Long(identityProviderConfigId), groupId});
    }
    public long saveGroup(long identityProviderConfigId, com.l7tech.identity.Group group) throws java.rmi.RemoteException {
        Call call = createStubCall();
        call.setOperationName(new QName(IDENTITY_URN, "saveGroup"));
        call.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, javax.xml.rpc.ParameterMode.IN);
        call.addParameter(new javax.xml.namespace.QName("", "group"), new javax.xml.namespace.QName(IDENTITY_URN, "Group"), com.l7tech.identity.Group.class, javax.xml.rpc.ParameterMode.IN);
        call.setReturnClass(Long.class);
        Long res = (Long)call.invoke(new Object[]{new java.lang.Long(identityProviderConfigId), group});
        return res.longValue();
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    protected void registerTypeMappings(Call call) {
        QName qn = new QName(IDENTITY_URN, "ArrayOfHeaders");
        call.registerTypeMapping(com.l7tech.objectmodel.EntityHeader[].class, qn, new org.apache.axis.encoding.ser.ArraySerializerFactory(), new org.apache.axis.encoding.ser.ArrayDeserializerFactory());
        qn = new QName(IDENTITY_URN, "EntityHeader");
        call.registerTypeMapping(com.l7tech.objectmodel.EntityHeader.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.objectmodel.EntityHeader.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.objectmodel.EntityHeader.class, qn));
        qn = new QName(IDENTITY_URN, "EntityType");
        call.registerTypeMapping(com.l7tech.objectmodel.EntityType.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.objectmodel.EntityType.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.objectmodel.EntityType.class, qn));
        qn = new QName(IDENTITY_URN, "IdentityProviderConfig");
        call.registerTypeMapping(com.l7tech.identity.IdentityProviderConfig.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.identity.IdentityProviderConfig.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.identity.IdentityProviderConfig.class, qn));
        qn = new QName(IDENTITY_URN, "User");
        call.registerTypeMapping(com.l7tech.identity.User.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.identity.User.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.identity.User.class, qn));
        qn = new QName(IDENTITY_URN, "Group");
        call.registerTypeMapping(com.l7tech.identity.Group.class, qn, new org.apache.axis.encoding.ser.BeanSerializerFactory(com.l7tech.identity.Group.class, qn), new org.apache.axis.encoding.ser.BeanDeserializerFactory(com.l7tech.identity.Group.class, qn));
    }

    protected String getFullServiceTarget() throws IOException {
        return getServiceBaseURL() + Service.SERVICE_DEPENDENT_URL_PORTION;
    }

    public static final String IDENTITY_URN = "http://www.layer7-tech.com/identity";
}
