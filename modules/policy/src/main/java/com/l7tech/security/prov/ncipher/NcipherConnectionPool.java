package com.l7tech.security.prov.ncipher;

import com.ncipher.nfast.connect.ClientException;
import com.ncipher.nfast.connect.ConnectionFailed;
import com.ncipher.nfast.connect.utils.EasyConnection;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A simple pool of connections for use by code that uses these connections directly.
 */
class NcipherConnectionPool {
    // Pool of connections to hardserver so we don't make new ones all the time.
    // Currently unbounded.
    private static final Queue<EasyConnection> connectionPool = new LinkedBlockingQueue<EasyConnection>();

    /**
     * Get a connection to the nCipher hardserver.
     * <p/>
     * Caller must ensure that the connection is returned via {@link #returnConnection(com.ncipher.nfast.connect.utils.EasyConnection)}
     * when they are finished using it.
     *
     * @return a connection, either new or from the pool.  Never null.
     * @throws com.ncipher.nfast.connect.ConnectionFailed if the connection cannot be made
     * @throws com.ncipher.nfast.connect.ClientException if there is a problem client side configuration
     */
    public static EasyConnection getConnection() throws ConnectionFailed, ClientException {
        EasyConnection c = connectionPool.poll();
        return c != null ? c : EasyConnection.connect();
    }

    /**
     * Return a connection to the pool.
     * 
     * @param c a connection that, the caller promises, will never again be used (except possibly via this pool).  Required.
     */
    public static void returnConnection(@NotNull EasyConnection c) {        
        connectionPool.add(c);
    }
}
