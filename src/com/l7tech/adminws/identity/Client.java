package com.l7tech.adminws.identity;

import org.apache.axis.client.Call;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;
import java.net.MalformedURLException;

import com.l7tech.adminws.CredentialsInvalidatorCallback;
import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.util.Locator;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 26, 2003
 *
 */
public class Client implements CredentialsInvalidatorCallback {

    public Client(String targetURL, String username, String password) {
        this.url = targetURL;
        this.username = username;
        this.password = password;
    }

    public synchronized void invalidateCredentials() {
        sessionCall = null;
        this.username = null;
        this.password = null;
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

    private Call createStubCall() throws java.rmi.RemoteException {
        if (sessionCall != null) {
            sessionCall.clearOperation();
            //sessionCall.clearHeaders();
            //sessionCall.removeAllParameters();
            return sessionCall;
        }

        ClientCredentialManager credentialManager = (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        if (username == null || password == null) {
            username = credentialManager.getUsername();
            password = credentialManager.getPassword();
        }
        credentialManager.registerForInvalidation(this);
        
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

    private static final String IDENTITY_URN = "http://www.layer7-tech.com/identity";
    private String url;
    private String username;
    private String password;
    // this static object is meant to maintain a session
    // so that not all calls result in an authentication operation
    // on the other side
    private static Call sessionCall = null;
}
