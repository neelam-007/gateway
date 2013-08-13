package com.l7tech.common.http.prov.apache;

import com.l7tech.util.ConfigFactory;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.protocol.HttpContext;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class IdentityBindingHttpConnectionManager extends PoolingClientConnectionManager {

    private static final boolean BINDING_ENABLED = ConfigFactory.getBooleanProperty("com.l7tech.common.http.prov.apache.identityBindingEnabled", true);
    private static final int DEFAULT_BINDING_TIMEOUT = ConfigFactory.getIntProperty("com.l7tech.common.http.prov.apache.identityBindingTimeout", 30000);
    private static final Logger logger = Logger.getLogger(IdentityBindingHttpConnectionManager.class.getName());
    private int bindingTimeout = DEFAULT_BINDING_TIMEOUT;
    private ThreadLocal<ThreadLocalInfo> info = new ThreadLocal<ThreadLocalInfo>();

    @Override
    public void releaseConnection(ManagedClientConnection conn, long keepalive, TimeUnit tunit) {
        if (conn.getState() == null) {
            super.releaseConnection(conn, keepalive, tunit);
        } else {
            super.releaseConnection(conn, bindingTimeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Bind the current connection.
     */
    public void bind(HttpContext context) {
        if (BINDING_ENABLED) {
            ThreadLocalInfo tli = getInfo();

            if (tli == null) {
                logger.warning("Attempt to bind with no id set"); // using fake id!
                tli = setInfo(new Object());
            }
            context.setAttribute(ClientContext.USER_TOKEN, tli.getId());
        }
    }
    /**
     * Set the id that will be used from this thread.
     *
     * @param identity The identity of the current user.
     */
    public void setId( final Object identity ) {
        if ( BINDING_ENABLED ) {
            setInfo(identity);
        }
    }

    private ThreadLocalInfo setInfo(final Object identity) {
        ThreadLocalInfo newInfo = info.get();
        if (identity == null) {
            newInfo = null;
        } else if (newInfo == null || !newInfo.getId().equals(identity)) {
            //The current thread may contain an identity, which is different to this identity,
            //During the cleanup, the previous bound connection will be cleaned up.
            newInfo = new ThreadLocalInfo(identity);
        }
        info.set(newInfo);
        return newInfo;
    }

    private ThreadLocalInfo getInfo() {
        return info.get();
    }

    /**
     * Holder for ThreadLocal data
     */
    private static final class ThreadLocalInfo {
        private final Object id;

        private ThreadLocalInfo(final Object id) {
            this.id = id;
        }

        public Object getId() {
            return id;
        }
    }
}
