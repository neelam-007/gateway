package com.l7tech.common.security.kerberos;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Implementation of the KerberosAdmin interface.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class KerberosAdminImpl implements KerberosAdmin {

    public Keytab getKeytab() throws KerberosException, RemoteException {
        return KerberosClient.getKerberosAcceptPrincipalKeytab();
    }

    public String getPrincipal() throws KerberosException, RemoteException {
        return KerberosClient.getKerberosAcceptPrincipal();
    }

    public Map getConfiguration() {
        new KerberosClient(); // To ensure kerberos is initialized.

        Map configMap = new HashMap();

        String kdc = KerberosUtils.getKerberosKdc();
        String realm = KerberosUtils.getKerberosRealm();

        if (kdc != null) configMap.put("kdc", kdc);
        if (realm != null) configMap.put("realm", realm);

        return Collections.unmodifiableMap(configMap);
    }
}
