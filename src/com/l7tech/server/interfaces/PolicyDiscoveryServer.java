/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.interfaces;

import java.rmi.RemoteException;
import java.rmi.Remote;

/**
 * This is the remote interface the Gateway provides, and the Agent consumes, for downloading
 * policies from the Gateway to the Agent.
 * @author mike
 */
public interface PolicyDiscoveryServer extends Remote {
    class PolicyInfo {
        public String policyVersion;
        public String policyXml;
    }

    class PolicyNotFoundException extends Exception {}

    /**
     * Look up the policy version and XML for the given published service OID.  
     *
     * @param serviceOid the Object ID of the policy to retrieve.  If the specified policy does not allow anonymous
     *                   access, the request will fail unless the policy grants access to the caller (as
     *                   identified by transport-level authentication; typically, HTTP Basic over SSL).
     * @return a PolicyInfo structure describing the service policy and policy version.  The policy will have been
     *                      filtered to contain only that subset relevant to the calling user.
     * @throws PolicyNotFoundException if the specified serviceOid does not exist, or the caller is not permitted
     *                                 to access it.
     * @throws RemoteException in case of authentication or other trouble.
     */
    PolicyInfo getPolicy(long serviceOid) throws PolicyNotFoundException, RemoteException;
}
