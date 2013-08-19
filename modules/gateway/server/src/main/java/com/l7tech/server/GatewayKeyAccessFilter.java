package com.l7tech.server;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.keystore.KeyAccessFilter;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Gateway's bean that restricts access to the PrivateKey for certain SsgKeyEntry instances.
 */
public class GatewayKeyAccessFilter implements KeyAccessFilter, InitializingBean {
    private static final Logger logger = Logger.getLogger(GatewayKeyAccessFilter.class.getName());

    private static final ThreadLocal<Boolean> restrictedKeyAccess = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private DefaultKey defaultKey;

    public void setDefaultKey(DefaultKey defaultKey) {
        this.defaultKey = defaultKey;
    }

    @Override
    public boolean isRestrictedAccessKeyEntry(SsgKeyEntry keyEntry) {
        if (defaultKey == null) {
            String msg = "defaultKey not provided yet -- incorrect initialization order";
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }

        boolean restricted = false;
        Pair<Goid, String> avInfo = defaultKey.getAuditViewerAlias();
        if (avInfo != null) {
            Goid kid = avInfo.left;
            String avAlias = avInfo.right;
            if (avAlias != null && avAlias.equalsIgnoreCase(keyEntry.getAlias()) && (Goid.isDefault(kid) || Goid.equals(kid, keyEntry.getKeystoreId()))) {
                    restricted = true;
            }
        }
        return restricted;
    }

    /**
     * Perform some work with access to restricted keys temporarily turned on.
     * <p/>
     * Restricted keys will not allow access to their private key under normal conditions.
     * <p/>
     * Currently the only restricted key is the audit viewer private key.
     *
     * @param callable  something to execute with restricted key access enabled for the duration, on this thread.  Required.
     * @param <T>       the return type of the callable.
     * @return the value returned by the callable.  May be null if the callable returns null.
     * @throws Exception if the callable throws an exception.
     */
    public <T> T doWithRestrictedKeyAccess(Callable<T> callable) throws Exception {
        final boolean wasRestrictedAccess = restrictedKeyAccess.get();
        try {
            restrictedKeyAccess.set(true);
            return callable.call();
        } finally {
            restrictedKeyAccess.set(wasRestrictedAccess);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Install ourself as the restricted access key checker, so casual users of SsgKeyEntry will never
        // accidentally use a restricted PrivateKey (eg, the audit viewer private key) in an unapproved context
        SsgKeyEntry.setRestrictedKeyAccessChecker(new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                return restrictedKeyAccess.get();
            }
        });
    }
}
