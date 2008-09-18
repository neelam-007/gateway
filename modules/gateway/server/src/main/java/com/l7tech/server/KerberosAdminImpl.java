package com.l7tech.server;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.kerberos.*;
import com.l7tech.gateway.common.admin.KerberosAdmin;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.objectmodel.ObjectModelException;

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

    public KerberosAdminImpl( final ClusterPropertyManager clusterPropertyManager,
                              final MasterPasswordManager clusterEncryptionManager ) {
        this.clusterPropertyManager = clusterPropertyManager;
        this.clusterEncryptionManager = clusterEncryptionManager;
    }

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

    public void installKeytab(byte[] data) throws KerberosException {
        try {
            ClusterProperty property = clusterPropertyManager.findByUniqueName(KEYTAB_PROPERTY);
            if (property == null) {
                property = new ClusterProperty();
                property.setName(KEYTAB_PROPERTY);
            }
            property.setValue( clusterEncryptionManager.encryptPassword(HexUtils.encodeBase64(data).toCharArray()) );

            long oid = property.getOid();
            if ( oid == ClusterProperty.DEFAULT_OID ) {
                clusterPropertyManager.save(property);
            } else {
                clusterPropertyManager.update(property);
            }
        } catch ( ObjectModelException ome ) {
            throw new KerberosException( "Error persisting keytab.", ome );        
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(KerberosAdminImpl.class.getName());
    private static final String KEYTAB_PROPERTY = "krb5.keytab";

    private final ClusterPropertyManager clusterPropertyManager;
    private final MasterPasswordManager clusterEncryptionManager;
}
