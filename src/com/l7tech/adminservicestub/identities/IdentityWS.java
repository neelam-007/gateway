/**
 * IdentityWS.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package com.l7tech.adminservicestub.identities;

public interface IdentityWS extends java.rmi.Remote {
    public com.l7tech.adminservicestub.ListResultEntry[] listProviders() throws java.rmi.RemoteException;
    public com.l7tech.adminservicestub.ListResultEntry[] listUsersInProvider(long providerId) throws java.rmi.RemoteException;
    public com.l7tech.adminservicestub.ListResultEntry[] listGroupsInProvider(long providerId) throws java.rmi.RemoteException;
}
