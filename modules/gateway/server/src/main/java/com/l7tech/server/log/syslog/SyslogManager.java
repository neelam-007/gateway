package com.l7tech.server.log.syslog;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicReference;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.io.Closeable;

import com.l7tech.server.log.syslog.impl.MinaManagedSyslog;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;

/**
 * Manager for syslog endpoints.
 *
 * <p>The manager is used to obtain a Syslog that sends information to a given
 * syslog server with some predefined values (facility, time zone, etc).</p>
 *
 * <p>Note that a Syslog MUST be closed when no longer required to dispose of
 * any underlying resources.</p>
 *
 * @author Steve Jones
 */
public class SyslogManager implements Closeable {

    //- PUBLIC

    /**
     * Get a syslog for sending messages to the given host/port.
     *
     * <p>The caller is responsible for ensuring that the returned Syslog is
     * closed when no longer required.</p>
     *
     * @param protocol The protocol to use
     * @param syslogHost The host to use
     * @param syslogPort The port to use
     * @param format The syslog format (null for default)
     * @param timeZone The syslog timezone (null for default)
     * @param facility The facility to log as
     * @param host The host to log as (should be FQDN)
     * @param charset The charset to log using (null for default)
     * @param lineDelimiter The delimiter for newlines when using TCP (null for default)
     * @return The existing or newly created Syslog
     */
    public Syslog getSyslog(final SyslogProtocol protocol,
                            final String syslogHost,
                            final int syslogPort,
                            final String format,
                            final String timeZone,
                            final int facility,
                            final String host,
                            final String charset,
                            final String lineDelimiter) {
        if (protocol == null) throw new IllegalArgumentException("protocol must not be null");
        if (syslogHost == null) throw new IllegalArgumentException("syslogHost must not be null");
        if (host == null) throw new IllegalArgumentException("host must not be null");
        if (syslogPort < 1 || syslogPort > 0xFFFF) throw new IllegalArgumentException("invalid syslogPort " + syslogPort);
        if (facility < 0 || facility > 124) throw new IllegalArgumentException("invalid facility " + facility);

        // no delimiter required for UDP
        String delimiter = SyslogProtocol.UDP == protocol ?
                "" :
                lineDelimiter;

        // we don't check for resolution here, since it may start working later
        InetSocketAddress address = new InetSocketAddress(syslogHost, syslogPort);

        return getSyslog(protocol, address, format, timeZone, facility, host, charset, delimiter);
    }

    /**
     * Set the listener used for connection events.
     *
     * @param listener The listener to use (null for none).
     */
    public void setConnectionListener(final SyslogConnectionListener listener) {
        syslogListener.delegate.set(listener);
    }

    /**
     * Close all underlying resources.
     *
     * <p>Clients should still be able to call log after this is done but,
     * any messages will be silently dropped.</p>
     */
    @Override
    public void close() {
        synchronized (syslogLock) {
            Iterator<ManagedSyslog> syslogIter = syslogs.values().iterator();
            while ( syslogIter.hasNext() ) {
                ManagedSyslog msyslog = syslogIter.next();
                syslogIter.remove();
                ResourceUtils.closeQuietly( msyslog );
            }
        }
    }

    //- PACKAGE

    /**
     * Get a syslog for sending messages to the given address.
     *
     * @return The existing or newly created Syslog
     */
    Syslog getSyslog(final SyslogProtocol protocol,
                     final SocketAddress address,
                     final String format,
                     final String timeZone,
                     final int facility,
                     final String host,
                     final String charset,
                     final String delimiter) {
        // access/create managed syslog
        ManagedSyslog mSyslog = getManagedSyslog(protocol, address);

        // return handle
        return mSyslog.getSylog(
                new ManagedSyslog.SyslogFormat(format, timeZone, charset, delimiter, getMaxLength()),
                facility,
                host);
    }

    /**
     * Destroy the given managed syslog if it is not in use.
     *
     * @param syslog The syslog that is no longer required.
     */
    void destroySyslog(final ManagedSyslog syslog) {
        if (syslog == null) throw new IllegalArgumentException("syslog must not be null");

        synchronized (syslogLock) {
            // must check when lock held
            if ( !syslog.isReferenced() ) {
                syslogs.values().remove(syslog);
                ResourceUtils.closeQuietly(syslog);
            }
        }
    }

    /**
     * Get the SyslogConnectionListener for this manager.
     *
     * @return The listener (never null)
     */
    SyslogConnectionListener getSyslogConnectionListener() {
        return syslogListener;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SyslogManager.class.getName());

    private static final int DEFAULT_MAX_LENGTH = 1024;
    private static final int MIN_MAX_LENGTH = 512;
    private static final int MAX_MAX_LENGTH = 65527; // theoretical limit for UDP packet data
    private static final String SYSPROP_MAX_LENGTH = SyslogManager.class.getPackage().getName() + ".maxLength";

    private final DelegatingSyslogConnectionListener syslogListener = new DelegatingSyslogConnectionListener();
    private final Object syslogLock = new Object();
    private final Map<SyslogKey,ManagedSyslog> syslogs = new HashMap<SyslogKey,ManagedSyslog>();


    /**
     * Get the maximum length to use for a log message.
     *
     * This accesses a system property which is constrained to reasonable
     * min/max values. 
     */
    private int getMaxLength() {
        int length = SyspropUtil.getInteger(SYSPROP_MAX_LENGTH, DEFAULT_MAX_LENGTH);

        if ( length < MIN_MAX_LENGTH ) length = MIN_MAX_LENGTH;
        if ( length > MAX_MAX_LENGTH ) length = MAX_MAX_LENGTH;

        return length;
    }

    /**
     * Get a managed syslog for sending messages to the given host/port.
     *
     * @param protocol The protocol to use
     * @param address The address to use
     * @return The existing or newly created ManagedSyslog
     */
    private ManagedSyslog getManagedSyslog(final SyslogProtocol protocol, final SocketAddress address) {
        if (protocol == null) throw new IllegalArgumentException("protocol must not be null");

        ManagedSyslog syslog;

        synchronized (syslogLock) {
            SyslogKey key = new SyslogKey(protocol, address);
            syslog = syslogs.get(key);

            if (syslog == null) {
                ManagedSyslog msyslog = new MinaManagedSyslog(protocol, address);
                msyslog.init(this);
                syslogs.put(key, msyslog);
                syslog = msyslog;
            }

            syslog.reference();
        }

        return syslog;
    }

    /**
     * Key for a syslog target
     */
    private static final class SyslogKey {
        private final SyslogProtocol protocol;
        private final SocketAddress address;

        SyslogKey(final SyslogProtocol protocol, final SocketAddress address) {
            this.protocol = protocol;
            this.address = address;
        }

        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SyslogKey syslogKey = (SyslogKey) o;

            if (!address.equals(syslogKey.address)) return false;
            if (protocol != syslogKey.protocol) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = protocol.hashCode();
            result = 31 * result + address.hashCode();
            return result;
        }
    }

    /**
     * Listener that is used by all ManagedSyslogs.
     */
    private static final class DelegatingSyslogConnectionListener implements SyslogConnectionListener {
        private AtomicReference<SyslogConnectionListener> delegate = new AtomicReference<SyslogConnectionListener>();

        @Override
        public void notifyConnected(final SocketAddress address) {
            SyslogConnectionListener listener = delegate.get();
            if ( listener != null ) {
                try {
                    listener.notifyConnected(address);
                } catch (Exception exception) {
                    logger.log(Level.WARNING, "Unexpected error in syslog connection listener", exception);
                }
            }
        }

        @Override
        public void notifyDisconnected(final SocketAddress address) {
            SyslogConnectionListener listener = delegate.get();
            if ( listener != null ) {
                try {
                    listener.notifyDisconnected(address);
                } catch (Exception exception) {                    
                    logger.log(Level.WARNING, "Unexpected error in syslog connection listener", exception);
                }
            }
        }
    }
}
