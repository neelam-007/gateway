package com.l7tech.common.security.kerberos;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.security.rbac.Secured;

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
@Secured
public class KerberosAdminImpl implements KerberosAdmin {

    //- PUBLIC

    public Keytab getKeytab() throws KerberosException {
        try {
            return KerberosClient.getKerberosAcceptPrincipalKeytab();
        }
        catch(KerberosException ke) {
            logger.log(Level.WARNING, "Kerberos keytab is invalid", ke);
            throw new KerberosException(ke.getMessage());
        }
    }

    public String getPrincipal() throws KerberosException {
        try {
            return KerberosClient.getKerberosAcceptPrincipal(true);
        } catch(KerberosException ke) {
            // Not really an error, since this is usually a configuration problem.
            // Note that we still throw the exception back to the caller so
            // the admin knows what happened
            logger.log(Level.INFO, "Kerberos error getting principal", ExceptionUtils.getDebugException(ke));
            throw new KerberosException(ke.getMessage());
        }
    }

    public Map<String,String> getConfiguration() {
        new KerberosClient(); // To ensure kerberos is initialized.

        Map<String,String> configMap = new HashMap<String,String>();

        String kdc = KerberosUtils.getKerberosKdc();
        String realm = KerberosUtils.getKerberosRealm();

        if (kdc != null) configMap.put("kdc", kdc);
        if (realm != null) configMap.put("realm", realm);

        return Collections.unmodifiableMap(configMap);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(KerberosAdminImpl.class.getName());
}
