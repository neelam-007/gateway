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
        final KerberosException[] exceptionHolder = new KerberosException[1];
        final String[] principalHolder = new String[1];

        Thread testThread = new Thread( new Runnable() {
            public void run() {
                try {
                    principalHolder[0] = KerberosClient.getKerberosAcceptPrincipal(true);
                } catch(KerberosException ke) {
                    // Not really an error, since this is usually a configuration problem.
                    // Note that we still throw the exception back to the caller so
                    // the admin knows what happened
                    logger.log(Level.INFO, "Kerberos error getting principal", ExceptionUtils.getDebugException(ke));
                    exceptionHolder[0] = new KerberosException(ke.getMessage());
                }
            }
        }, "KerberosConfigurationTester" );

        testThread.start();

        try {
            testThread.join( 30000 );
        } catch ( InterruptedException ie ) {
            throw new KerberosConfigException("Kerberos configuration error '"+ ExceptionUtils.getMessage(ie)+"'.", ie);
        }

        if ( testThread.isAlive() ) {
            throw new KerberosException("Kerberos configuration error 'Authentication timed out'.");
        }

        if ( exceptionHolder[0] != null ) {
            throw exceptionHolder[0];
        }

        return principalHolder[0];
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
