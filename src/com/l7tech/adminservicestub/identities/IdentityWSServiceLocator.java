/**
 * IdentityWSServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package com.l7tech.adminservicestub.identities;

public class IdentityWSServiceLocator extends org.apache.axis.client.Service implements com.l7tech.adminservicestub.identities.IdentityWSService {

    // Use to get a proxy class for identities
    private final java.lang.String identities_address = "http://ssg.layer7-tech.com/identities";

    public java.lang.String getidentitiesAddress() {
        return identities_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String identitiesWSDDServiceName = "identities";

    public java.lang.String getidentitiesWSDDServiceName() {
        return identitiesWSDDServiceName;
    }

    public void setidentitiesWSDDServiceName(java.lang.String name) {
        identitiesWSDDServiceName = name;
    }

    public com.l7tech.adminservicestub.identities.IdentityWS getidentities() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(identities_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getidentities(endpoint);
    }

    public com.l7tech.adminservicestub.identities.IdentityWS getidentities(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.l7tech.adminservicestub.identities.IdentitiesSoapBindingStub _stub = new com.l7tech.adminservicestub.identities.IdentitiesSoapBindingStub(portAddress, this);
            _stub.setPortName(getidentitiesWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.l7tech.adminservicestub.identities.IdentityWS.class.isAssignableFrom(serviceEndpointInterface)) {
                com.l7tech.adminservicestub.identities.IdentitiesSoapBindingStub _stub = new com.l7tech.adminservicestub.identities.IdentitiesSoapBindingStub(new java.net.URL(identities_address), this);
                _stub.setPortName(getidentitiesWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        String inputPortName = portName.getLocalPart();
        if ("identities".equals(inputPortName)) {
            return getidentities();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://www.layer7-tech.com/adminservice/identity", "IdentityWSService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("identities"));
        }
        return ports.iterator();
    }

}
