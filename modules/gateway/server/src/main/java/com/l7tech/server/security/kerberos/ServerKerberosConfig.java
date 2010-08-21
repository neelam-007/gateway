package com.l7tech.server.security.kerberos;

import com.l7tech.server.ServerConfig;
import com.l7tech.kerberos.KerberosUtils;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.InitializingBean;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.ParseException;

/**
 * Server side initializer for KerberosConfig.  This class performs the check against the
 * cluster properties for Realm/KDC values that affect how the krb5.conf file is configured.
 *
 * @author: vchan
 */
public class ServerKerberosConfig implements InitializingBean, PropertyChangeListener {

    //- PUBLIC

    public ServerKerberosConfig( final ServerConfig config,
                                 final MasterPasswordManager clusterEncryptionManager ) {
        this.config = config;
        this.clusterEncryptionManager = clusterEncryptionManager;
    }

    /**
     * Initializer run during server startup.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        String keytabBase64 = config.getPropertyCached(KERBEROS_KEYTAB_PROP);
        String realm = config.getProperty(ServerConfig.PARAM_KERBEROS_CONFIG_REALM);
        String kdc = config.getProperty(ServerConfig.PARAM_KERBEROS_CONFIG_KDC);

        synchronized( configSync ) {
            this.keytabB64 = keytabBase64;
            this.realm = realm;
            this.kdc = kdc;
            updateKerberosConfig( false, keytabBase64, kdc, realm );
        }
    }

    /**
     * Performs the cluster property change update for the Kerberos Realm/KDC values.
     *
     * @param evt the property change event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        boolean updated = false;

        if ( KERBEROS_KEYTAB_PROP.equals(evt.getPropertyName() ) ) {
            updated = true;
            synchronized( configSync ) {
                keytabB64 = evt.getNewValue() == null ? null :  evt.getNewValue().toString();
            }
        } else if (ServerConfig.PARAM_KERBEROS_CONFIG_REALM.equals(evt.getPropertyName())) {
            updated = true;
            synchronized( configSync ) {
                if (evt.getNewValue() == null) {
                    realm = null;
                } else {
                    realm = evt.getNewValue().toString();
                }
            }
        } else if ((ServerConfig.PARAM_KERBEROS_CONFIG_KDC).equals(evt.getPropertyName()) ) {
            updated = true;
            synchronized( configSync ) {
                if (evt.getNewValue() == null) {
                    kdc = null;
                } else {
                    kdc = evt.getNewValue().toString();
                }
            }
        }

        if ( updated ) {
            synchronized( configSync ) {
                updateKerberosConfig( true, keytabB64, kdc, realm );
            }
        }
    }

    //- PRIVATE

    // logger
    private static final Logger logger = Logger.getLogger(ServerKerberosConfig.class.getName());
    private static final Object configSync = new Object();
    private static final String KERBEROS_KEYTAB_PROP = "krb5Keytab";
    private final ServerConfig config;
    private final MasterPasswordManager clusterEncryptionManager;

    private String keytabB64;
    private String kdc;
    private String realm;

    /**
     * Re-create the Kerberos config file "krb5.conf" based on the updated cluster properties for
     * Realm / KDC.
     */
    private void updateKerberosConfig( final boolean deleteIfMissing,
                                       final String keytabB64,
                                       final String kdc,
                                       final String realm ) {
        logger.config("(Re)Generating kerberos configuration.");
        try {
            byte[] keytab = null;
            if ( keytabB64 != null ) {
                keytab = HexUtils.decodeBase64(new String(clusterEncryptionManager.decryptPassword(keytabB64)));
            }
            KerberosUtils.configureKerberos( deleteIfMissing, keytab, kdc, realm );
        } catch (KerberosException ke) {
            logger.log(Level.WARNING, "Unable to update Kerberos config following cluster property change.", ke);
        } catch (ParseException pe) {
            logger.log(Level.WARNING, "Unable to update Kerberos config following cluster property change (decryption failed) '"+pe.getMessage()+"'.", ExceptionUtils.getDebugException(pe));
        }
    }
}
