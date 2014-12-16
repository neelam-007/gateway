package com.l7tech.external.assertions.cassandra;

import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.springframework.context.ApplicationContext;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/30/14
 * Time: 5:03 PM
 *
 */
public class CassandraAssertionModuleLoadListener {

    // inject licenseManager

    public static synchronized void onModuleUnloaded() {
        //TODO: perform module unload
    }

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        // if (licenseManager.isFeatureEnabled(new CassandraQueryAssertion().getFeatureSetName()) {
        // do module load steps
        // }
    }
}
