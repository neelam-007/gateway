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
import java.io.IOException;
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
    public void afterPropertiesSet() throws Exception {
        String keytabBase64 = config.getPropertyCached(KERBEROS_KEYTAB_PROP);
        if ( keytabBase64 != null) {
            updateKerberosConfig( keytabBase64 );
        }
    }

    /**
     * Performs the cluster property change update for the Kerberos Realm/KDC values.
     *
     * @param evt the property change event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ( KERBEROS_KEYTAB_PROP.equals(evt.getPropertyName() ) && evt.getNewValue() != null ) {
            updateKerberosConfig( evt.getNewValue().toString() );                    
        }
    }

    //- PRIVATE

    // logger
    private static final Logger logger = Logger.getLogger(ServerKerberosConfig.class.getName());
    private static final Object configSync = new Object();
    private static final String KERBEROS_KEYTAB_PROP = "krb5Keytab";
    private final ServerConfig config;
    private final MasterPasswordManager clusterEncryptionManager;

    /**
     * Re-create the Kerberos config file "krb5.conf" based on the updated cluster properties for
     * Realm / KDC.
     */
    private void updateKerberosConfig( final String keytabB64 ) {
        synchronized( configSync ) {
            logger.config("(Re)Generating kerberos configuration.");
            try {
                KerberosUtils.configureKerberos( HexUtils.decodeBase64(new String(clusterEncryptionManager.decryptPassword(keytabB64))) );
            } catch (KerberosException ke) {
                logger.log(Level.WARNING, "Unable to update Kerberos config following cluster property change.", ke);
            } catch (ParseException pe) {
                logger.log(Level.WARNING, "Unable to update Kerberos config following cluster property change (decryption failed) '"+pe.getMessage()+"'.", ExceptionUtils.getDebugException(pe));
            } catch (IOException ke) {
                logger.log(Level.WARNING, "Unable to update Kerberos config following cluster property change (invalid data).", ExceptionUtils.getDebugException(ke));
            }
        }
    }
}
