/**
 * IdentitiesSoapBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package com.l7tech.adminws.identity;

public class IdentitiesSoapBindingStub extends org.apache.axis.client.Stub implements com.l7tech.adminws.identity.Identity {
    private java.util.Vector cachedSerClasses = new java.util.Vector();
    private java.util.Vector cachedSerQNames = new java.util.Vector();
    private java.util.Vector cachedSerFactories = new java.util.Vector();
    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[15];
        org.apache.axis.description.OperationDesc oper;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findAlllIdentityProviderConfig");
        oper.setReturnType(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "ArrayOf_tns1_Header"));
        oper.setReturnClass(com.l7tech.adminws.identity.Header[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "findAlllIdentityProviderConfigReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findAllIdentityProviderConfigByOffset");
        oper.addParameter(new javax.xml.namespace.QName("", "offset"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.addParameter(new javax.xml.namespace.QName("", "windowSize"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "ArrayOf_tns1_Header"));
        oper.setReturnClass(com.l7tech.adminws.identity.Header[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "findAllIdentityProviderConfigByOffsetReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findIdentityProviderConfigByPrimaryKey");
        oper.addParameter(new javax.xml.namespace.QName("", "oid"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(new javax.xml.namespace.QName("http://service.adminws.l7tech.com", "IdentityProviderConfig"));
        oper.setReturnClass(com.l7tech.adminws.identity.IdentityProviderConfig.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "findIdentityProviderConfigByPrimaryKeyReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[2] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("saveIdentityProviderConfig");
        oper.addParameter(new javax.xml.namespace.QName("", "identityProviderConfig"), new javax.xml.namespace.QName("http://service.adminws.l7tech.com", "IdentityProviderConfig"), com.l7tech.adminws.identity.IdentityProviderConfig.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        oper.setReturnClass(long.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "saveIdentityProviderConfigReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteIdentityProviderConfig");
        oper.addParameter(new javax.xml.namespace.QName("", "oid"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findUserByPrimaryKey");
        oper.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.addParameter(new javax.xml.namespace.QName("", "userId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(new javax.xml.namespace.QName("http://service.adminws.l7tech.com", "User"));
        oper.setReturnClass(com.l7tech.adminws.identity.User.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "findUserByPrimaryKeyReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[5] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteUser");
        oper.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.addParameter(new javax.xml.namespace.QName("", "userId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[6] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("saveUser");
        oper.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.addParameter(new javax.xml.namespace.QName("", "user"), new javax.xml.namespace.QName("http://service.adminws.l7tech.com", "User"), com.l7tech.adminws.identity.User.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        oper.setReturnClass(long.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "saveUserReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[7] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findAllUsers");
        oper.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "ArrayOf_tns1_Header"));
        oper.setReturnClass(com.l7tech.adminws.identity.Header[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "findAllUsersReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[8] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findAllUsersByOffset");
        oper.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.addParameter(new javax.xml.namespace.QName("", "offset"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.addParameter(new javax.xml.namespace.QName("", "windowSize"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "ArrayOf_tns1_Header"));
        oper.setReturnClass(com.l7tech.adminws.identity.Header[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "findAllUsersByOffsetReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[9] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findGroupByPrimaryKey");
        oper.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.addParameter(new javax.xml.namespace.QName("", "groupId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(new javax.xml.namespace.QName("http://service.adminws.l7tech.com", "Group"));
        oper.setReturnClass(com.l7tech.adminws.identity.Group.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "findGroupByPrimaryKeyReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[10] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteGroup");
        oper.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.addParameter(new javax.xml.namespace.QName("", "groupId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[11] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("saveGroup");
        oper.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.addParameter(new javax.xml.namespace.QName("", "group"), new javax.xml.namespace.QName("http://service.adminws.l7tech.com", "Group"), com.l7tech.adminws.identity.Group.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        oper.setReturnClass(long.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "saveGroupReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[12] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findAllGroups");
        oper.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "ArrayOf_tns1_Header"));
        oper.setReturnClass(com.l7tech.adminws.identity.Header[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "findAllGroupsReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[13] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("findAllGroupsByOffset");
        oper.addParameter(new javax.xml.namespace.QName("", "identityProviderConfigId"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"), long.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.addParameter(new javax.xml.namespace.QName("", "offset"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.addParameter(new javax.xml.namespace.QName("", "windowSize"), new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, org.apache.axis.description.ParameterDesc.IN, false, false);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "ArrayOf_tns1_Header"));
        oper.setReturnClass(com.l7tech.adminws.identity.Header[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "findAllGroupsByOffsetReturn"));
        oper.setStyle(org.apache.axis.enum.Style.RPC);
        oper.setUse(org.apache.axis.enum.Use.ENCODED);
        _operations[14] = oper;

    }

    public IdentitiesSoapBindingStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public IdentitiesSoapBindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public IdentitiesSoapBindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
        if (service == null) {
            super.service = new org.apache.axis.client.Service();
        } else {
            super.service = service;
        }
            java.lang.Class cls;
            javax.xml.namespace.QName qName;
            java.lang.Class beansf = org.apache.axis.encoding.ser.BeanSerializerFactory.class;
            java.lang.Class beandf = org.apache.axis.encoding.ser.BeanDeserializerFactory.class;
            java.lang.Class enumsf = org.apache.axis.encoding.ser.EnumSerializerFactory.class;
            java.lang.Class enumdf = org.apache.axis.encoding.ser.EnumDeserializerFactory.class;
            java.lang.Class arraysf = org.apache.axis.encoding.ser.ArraySerializerFactory.class;
            java.lang.Class arraydf = org.apache.axis.encoding.ser.ArrayDeserializerFactory.class;
            java.lang.Class simplesf = org.apache.axis.encoding.ser.SimpleSerializerFactory.class;
            java.lang.Class simpledf = org.apache.axis.encoding.ser.SimpleDeserializerFactory.class;
            qName = new javax.xml.namespace.QName("http://service.adminws.l7tech.com", "Header");
            cachedSerQNames.add(qName);
            cls = com.l7tech.adminws.identity.Header.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://service.adminws.l7tech.com", "IdentityProviderConfig");
            cachedSerQNames.add(qName);
            cls = com.l7tech.adminws.identity.IdentityProviderConfig.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://service.adminws.l7tech.com", "User");
            cachedSerQNames.add(qName);
            cls = com.l7tech.adminws.identity.User.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://service.adminws.l7tech.com", "Group");
            cachedSerQNames.add(qName);
            cls = com.l7tech.adminws.identity.Group.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "ArrayOf_tns1_Header");
            cachedSerQNames.add(qName);
            cls = com.l7tech.adminws.identity.Header[].class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(arraysf);
            cachedDeserFactories.add(arraydf);

    }

    private org.apache.axis.client.Call createCall() throws java.rmi.RemoteException {
        try {
            org.apache.axis.client.Call _call =
                    (org.apache.axis.client.Call) super.service.createCall();
            if (super.maintainSessionSet) {
                _call.setMaintainSession(super.maintainSession);
            }
            if (super.cachedUsername != null) {
                _call.setUsername(super.cachedUsername);
            }
            if (super.cachedPassword != null) {
                _call.setPassword(super.cachedPassword);
            }
            if (super.cachedEndpoint != null) {
                _call.setTargetEndpointAddress(super.cachedEndpoint);
            }
            if (super.cachedTimeout != null) {
                _call.setTimeout(super.cachedTimeout);
            }
            if (super.cachedPortName != null) {
                _call.setPortName(super.cachedPortName);
            }
            java.util.Enumeration keys = super.cachedProperties.keys();
            while (keys.hasMoreElements()) {
                java.lang.String key = (java.lang.String) keys.nextElement();
                _call.setProperty(key, super.cachedProperties.get(key));
            }
            // All the type mapping information is registered
            // when the first call is made.
            // The type mapping information is actually registered in
            // the TypeMappingRegistry of the service, which
            // is the reason why registration is only needed for the first call.
            synchronized (this) {
                if (firstCall()) {
                    // must set encoding style before registering serializers
                    _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
                    _call.setEncodingStyle(org.apache.axis.Constants.URI_SOAP11_ENC);
                    for (int i = 0; i < cachedSerFactories.size(); ++i) {
                        java.lang.Class cls = (java.lang.Class) cachedSerClasses.get(i);
                        javax.xml.namespace.QName qName =
                                (javax.xml.namespace.QName) cachedSerQNames.get(i);
                        java.lang.Class sf = (java.lang.Class)
                                 cachedSerFactories.get(i);
                        java.lang.Class df = (java.lang.Class)
                                 cachedDeserFactories.get(i);
                        _call.registerTypeMapping(cls, qName, sf, df, false);
                    }
                }
            }
            return _call;
        }
        catch (java.lang.Throwable t) {
            throw new org.apache.axis.AxisFault("Failure trying to get the Call object", t);
        }
    }

    public com.l7tech.adminws.identity.Header[] findAlllIdentityProviderConfig() throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "findAlllIdentityProviderConfig"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return (com.l7tech.adminws.identity.Header[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.l7tech.adminws.identity.Header[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.l7tech.adminws.identity.Header[].class);
            }
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "findAllIdentityProviderConfigByOffset"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Integer(offset), new java.lang.Integer(windowSize)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return (com.l7tech.adminws.identity.Header[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.l7tech.adminws.identity.Header[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.l7tech.adminws.identity.Header[].class);
            }
        }
    }

    public com.l7tech.adminws.identity.IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "findIdentityProviderConfigByPrimaryKey"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(oid)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return (com.l7tech.adminws.identity.IdentityProviderConfig) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.l7tech.adminws.identity.IdentityProviderConfig) org.apache.axis.utils.JavaUtils.convert(_resp, com.l7tech.adminws.identity.IdentityProviderConfig.class);
            }
        }
    }

    public long saveIdentityProviderConfig(com.l7tech.adminws.identity.IdentityProviderConfig identityProviderConfig) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "saveIdentityProviderConfig"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {identityProviderConfig});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return ((java.lang.Long) _resp).longValue();
            } catch (java.lang.Exception _exception) {
                return ((java.lang.Long) org.apache.axis.utils.JavaUtils.convert(_resp, long.class)).longValue();
            }
        }
    }

    public void deleteIdentityProviderConfig(long oid) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "deleteIdentityProviderConfig"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(oid)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        getResponseHeaders(_call);
        extractAttachments(_call);
    }

    public com.l7tech.adminws.identity.User findUserByPrimaryKey(long identityProviderConfigId, long userId) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "findUserByPrimaryKey"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(identityProviderConfigId), new java.lang.Long(userId)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return (com.l7tech.adminws.identity.User) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.l7tech.adminws.identity.User) org.apache.axis.utils.JavaUtils.convert(_resp, com.l7tech.adminws.identity.User.class);
            }
        }
    }

    public void deleteUser(long identityProviderConfigId, long userId) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "deleteUser"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(identityProviderConfigId), new java.lang.Long(userId)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        getResponseHeaders(_call);
        extractAttachments(_call);
    }

    public long saveUser(long identityProviderConfigId, com.l7tech.adminws.identity.User user) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "saveUser"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(identityProviderConfigId), user});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return ((java.lang.Long) _resp).longValue();
            } catch (java.lang.Exception _exception) {
                return ((java.lang.Long) org.apache.axis.utils.JavaUtils.convert(_resp, long.class)).longValue();
            }
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllUsers(long identityProviderConfigId) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[8]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "findAllUsers"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(identityProviderConfigId)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return (com.l7tech.adminws.identity.Header[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.l7tech.adminws.identity.Header[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.l7tech.adminws.identity.Header[].class);
            }
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[9]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "findAllUsersByOffset"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(identityProviderConfigId), new java.lang.Integer(offset), new java.lang.Integer(windowSize)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return (com.l7tech.adminws.identity.Header[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.l7tech.adminws.identity.Header[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.l7tech.adminws.identity.Header[].class);
            }
        }
    }

    public com.l7tech.adminws.identity.Group findGroupByPrimaryKey(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[10]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "findGroupByPrimaryKey"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(identityProviderConfigId), new java.lang.Long(groupId)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return (com.l7tech.adminws.identity.Group) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.l7tech.adminws.identity.Group) org.apache.axis.utils.JavaUtils.convert(_resp, com.l7tech.adminws.identity.Group.class);
            }
        }
    }

    public void deleteGroup(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[11]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "deleteGroup"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(identityProviderConfigId), new java.lang.Long(groupId)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        getResponseHeaders(_call);
        extractAttachments(_call);
    }

    public long saveGroup(long identityProviderConfigId, com.l7tech.adminws.identity.Group group) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[12]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "saveGroup"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(identityProviderConfigId), group});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return ((java.lang.Long) _resp).longValue();
            } catch (java.lang.Exception _exception) {
                return ((java.lang.Long) org.apache.axis.utils.JavaUtils.convert(_resp, long.class)).longValue();
            }
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllGroups(long identityProviderConfigId) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[13]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "findAllGroups"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(identityProviderConfigId)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return (com.l7tech.adminws.identity.Header[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.l7tech.adminws.identity.Header[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.l7tech.adminws.identity.Header[].class);
            }
        }
    }

    public com.l7tech.adminws.identity.Header[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[14]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "findAllGroupsByOffset"));

        setRequestHeaders(_call);
        setAttachments(_call);
        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {new java.lang.Long(identityProviderConfigId), new java.lang.Integer(offset), new java.lang.Integer(windowSize)});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            getResponseHeaders(_call);
            extractAttachments(_call);
            try {
                return (com.l7tech.adminws.identity.Header[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.l7tech.adminws.identity.Header[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.l7tech.adminws.identity.Header[].class);
            }
        }
    }

}
