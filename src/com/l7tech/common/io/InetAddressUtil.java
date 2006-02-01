/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for {@link java.net.InetAddress}.
 */
public class InetAddressUtil {
    private static final Logger logger = Logger.getLogger(InetAddressUtil.class.getName());

    private static final InetAddress localHost;
    static {
        InetAddress lh;
        try {
            lh = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // Misconfigured local hostname or DNS -- fall back to hardcoded 127.0.0.1
            logger.log(Level.WARNING, "Unable to get localhost: " + e.getMessage(), e);
            try {
                lh = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
            } catch (UnknownHostException e1) {
                throw new RuntimeException(e1); // can't happen
            }
        }
        localHost = lh;
    }

    /**
     * Non-throwing version of InetAddress.getLocalHost().  This version will return 127.0.0.1 if the local hostname
     * is bogus or can't be resolved.  The value returned by this method is <em>not</em> guaranteed to be up-to-date
     * if the system hostname changes after the JVM process has started.
     * <p/>
     * Since this method might return 127.0.0.1, it should not be used when ambiguity about which machine's
     * loopback address is itended exists and such ambiguity cannot be tolerated (for example, when this address will be
     * sent to a remote host, which may attempt to connect back to it).
     *
     * @return A reasonable InetAddress to use representing "this here host"; 127.0.0.1 as a last resort.  Never null.
     */
    public static InetAddress getLocalHost() {
        return localHost;
    }
}
