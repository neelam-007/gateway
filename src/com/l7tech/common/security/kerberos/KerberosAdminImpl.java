package com.l7tech.common.security.kerberos;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Implementation of the KerberosAdmin interface.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class KerberosAdminImpl implements KerberosAdmin {

    //- PUBLIC

    public Keytab getKeytab() throws KerberosException {
        try {
            return KerberosClient.getKerberosAcceptPrincipalKeytab();
        }
        catch(KerberosException ke) {
            logger.log(Level.WARNING, "Kerberos keytab is invalid", ke);
            throw ke;
        }
    }

    public String getPrincipal() throws KerberosException {
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

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(KerberosAdminImpl.class.getName());
}
