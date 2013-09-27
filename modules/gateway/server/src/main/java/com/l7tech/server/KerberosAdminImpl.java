package com.l7tech.server;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.kerberos.*;
import com.l7tech.gateway.common.admin.KerberosAdmin;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.objectmodel.ObjectModelException;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.internal.ktab.KeyTabEntry;

import java.util.*;
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

    /**
     * Retrieve all the keytab entry under a keytab file
     * @return List of KeyTabEntryInfo, this never return null, if there is no keytab entry in the keytab file,
     *         it returns an emply list.
     * @throws KerberosException
     */
    public List<KeyTabEntryInfo> getKeyTabEntryInfos() throws KerberosException {
        List<KeyTabEntryInfo> keyTabEntryInfos = new ArrayList<KeyTabEntryInfo>();
        try {
            KeyTab keyTab = KerberosClient.getKerberosAcceptPrincipalKeytab();

            if (keyTab != null) {
                KeyTabEntry[] keyTabEntries = keyTab.getEntries();
                for (int i = 0; i < keyTabEntries.length; i++) {
                    KeyTabEntry keyTabEntry = keyTabEntries[i];
                    KeyTabEntryInfo info = new KeyTabEntryInfo();
                    info.setRealm(keyTabEntry.getService().getRealm().toString());
                    info.setKdc(KerberosUtils.getKdc(info.getRealm()));
                    info.setPrincipalName(keyTabEntry.getService().getName());
                    info.setDate(keyTabEntry.getTimeStamp() == null ? null : keyTabEntry.getTimeStamp().toDate());
                    info.setVersion(keyTabEntry.getKey().getKeyVersionNumber());
                    info.setEtype(keyTabEntry.getKey().getEType());
                    keyTabEntryInfos.add(info);
                }
            }
        } catch(KerberosException ke) {
            logger.log(Level.WARNING, "Kerberos keytab is invalid", ke);
            throw new KerberosException(ke.getMessage());
        }
        return keyTabEntryInfos;
    }

    public void validatePrincipal(final String principal) throws KerberosException {
        final KerberosException[] exceptionHolder = new KerberosException[1];
        final String[] principalHolder = new String[1];

        Thread testThread = new Thread( new Runnable() {
            public void run() {
                try {
                    principalHolder[0] = KerberosClient.getKerberosAcceptPrincipal(principal, true);
                } catch(KerberosException ke) {
                    // Not really an error, since this is usually a configuration problem.
                    // Note that we still throw the exception back to the caller so
                    // the admin knows what happened
                    logger.log(Level.WARNING, "Kerberos error getting principal: " + principal, ExceptionUtils.getDebugException(ke));
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
            logger.log(Level.WARNING, "Kerberos authentication timed out: " + principal );
            throw new KerberosException("Kerberos configuration error 'Authentication timed out'.");
        }

        if ( exceptionHolder[0] != null ) {
            throw exceptionHolder[0];
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

    public void installKeytab(byte[] data) throws KerberosException {
        try {
            ClusterProperty property = clusterPropertyManager.findByUniqueName(KEYTAB_PROPERTY);
            if (property == null) {
                property = new ClusterProperty();
                property.setName(KEYTAB_PROPERTY);
            }
            property.setValue( clusterEncryptionManager.encryptPassword(HexUtils.encodeBase64(data).toCharArray()) );

            Goid goid = property.getGoid();
            if ( ClusterProperty.DEFAULT_GOID.equals(goid) ) {
                clusterPropertyManager.save(property);
            } else {
                clusterPropertyManager.update(property);
            }
        } catch ( ObjectModelException ome ) {
            ome.printStackTrace();
            throw new KerberosException( "Error persisting keytab.", ome );        
        }
    }

    @Override
    public void deleteKeytab() throws KerberosException {
        try {
            ClusterProperty property = clusterPropertyManager.findByUniqueName(KEYTAB_PROPERTY);
            if (property == null) {
                throw new KerberosException( "Deletion not possible" );
            }
            clusterPropertyManager.delete( property );
        } catch ( ObjectModelException ome ) {
            ome.printStackTrace();
            throw new KerberosException( "Error deleting keytab.", ome );
        }
    }

    @Override
    public void setKeytabValidate(boolean validate) throws KerberosException {
        try {
            ClusterProperty property = clusterPropertyManager.findByUniqueName(KEYTAB_VALIDATE);
            if (property == null) {
                property = new ClusterProperty();
                property.setName(KEYTAB_VALIDATE);
            }
            property.setValue(Boolean.toString(validate));

            Goid goid = property.getGoid();
            if ( ClusterProperty.DEFAULT_GOID.equals(goid) ) {
                clusterPropertyManager.save(property);
            } else {
                clusterPropertyManager.update(property);
            }
        } catch ( ObjectModelException ome ) {
            throw new KerberosException( "Error persisting " + KEYTAB_VALIDATE, ome );
        }
    }

    @Override
    public boolean getKeytabValidate() throws KerberosException {
        try {
            ClusterProperty property = clusterPropertyManager.findByUniqueName(KEYTAB_VALIDATE);
            if (property == null) {
                //Default to true;
                return true;
            }
            return Boolean.parseBoolean(property.getValue());
        } catch ( ObjectModelException ome ) {
            throw new KerberosException( "Error retrieving " + KEYTAB_VALIDATE, ome );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(KerberosAdminImpl.class.getName());
    private static final String KEYTAB_PROPERTY = "krb5.keytab";
    private static final String KEYTAB_VALIDATE = "krb5.keytab.validate";

    private final ClusterPropertyManager clusterPropertyManager;
    private final MasterPasswordManager clusterEncryptionManager;
}
