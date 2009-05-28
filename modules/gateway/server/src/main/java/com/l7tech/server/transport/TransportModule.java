package com.l7tech.server.transport;

import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.LifecycleBean;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Triple;
import org.springframework.context.ApplicationEvent;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass for transport modules for connectors for incoming FTP, HTTP, etc.
 * 
 * @see com.l7tech.server.transport.http.HttpTransportModule
 * @see com.l7tech.server.transport.ftp.FtpServerManager
 */
public abstract class TransportModule extends LifecycleBean {
    protected final Logger logger;
    protected final SsgConnectorManager ssgConnectorManager;
    protected final ClusterPropertyManager clusterPropertyManager;

    protected final Map<Long, Throwable> connectorErrors = new ConcurrentHashMap<Long, Throwable>();

    private final AtomicReference<Triple<Long, String, Set<InterfaceTag>>> interfaceTags = new AtomicReference<Triple<Long, String, Set<InterfaceTag>>>(null);

    public static final class ListenerException extends Exception {
        public ListenerException(String message) {
            super(message);
        }

        public ListenerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    protected TransportModule(String name, Logger logger, String licenseFeature, LicenseManager licenseManager, SsgConnectorManager ssgConnectorManager, ClusterPropertyManager clusterPropertyManager)
    {
        super(name, logger, licenseFeature, licenseManager);
        this.logger = logger;
        this.ssgConnectorManager = ssgConnectorManager;
        this.clusterPropertyManager = clusterPropertyManager;
    }

    /**
     * Attempt to add and spin up the specified connector on this transport module.
     *
     * @param connector  the connector to add.  Required.
     * @throws ListenerException if a proble is immediately detected while starting the new connector.
     */
    protected abstract void addConnector(SsgConnector connector) throws ListenerException;

    /**
     * Shut down and remove any connector from this transport module with the specified oid.
     * <p/>
     * If the specified oid is not recognized as a previously-added connector,
     * this method silently does nothing.
     *
     * @param oid the oid of the connector to remove.
     */
    protected abstract void removeConnector(long oid);

    /**
     * Get the set of scheme names recognized by this transport module.
     *
     * @return a set of scheme names, ie "HTTP" and "HTTPS".  Never null.
     */
    protected abstract Set<String> getSupportedSchemes();

    /**
     * Check if the specified scheme is recognized by this transport module.
     * <p/>
     * This method just checks if the named scheme is listed in {@link #getSupportedSchemes}.
     *
     * @param scheme  the scheme being considered, ie "HTTP" or "FTPS".  Required.
     * @return true if connectors with this scheme will be added by this transport module.
     */
    protected boolean schemeIsOwnedByThisModule(String scheme) {
        return getSupportedSchemes().contains(scheme);
    }

    /**
     * Check if the specified connector should be recognized by this transport module.
     * <p/>
     * This method just checks the scheme with {@link #schemeIsOwnedByThisModule(String)}.
     *
     * @param connector the connector being considered.  Required.
     * @return true if this conector should be added by this transport module.
     */
    protected boolean connectorIsOwnedByThisModule(SsgConnector connector) {
        return schemeIsOwnedByThisModule(connector.getScheme());
    }

    /**
     * Check if the specified connector is configured correctly for this transport module.
     * <p/>
     * The general contract is that for connectors owned by this module, they will only be put
     * in service if they are enabled and this method returns true; otherwise they will
     * be taken out of service.
     * <p/>
     * This method always returns true.
     *
     * @param connector the connector to examine.  Required.
     * @return true if this connector configuration looks valid for this transport module; 
     *         false if it is invalid and should be treated as though it were disabled
     */
    protected boolean isValidConnectorConfig(SsgConnector connector) {
        return true;
    }

    /**
     * Translate a bind address that references an interface tag into a concrete IP address.
     * 
     * @param bindAddress
     * @return
     */
    protected String translateBindAddress(String bindAddress, int port) throws ListenerException {
        if (!looksLikeInterfaceTagName(bindAddress))
            return bindAddress;

        // Try to look up as interface tag
        Triple<Long, String, Set<InterfaceTag>> info = getInterfaceTagsCached();
        if (info == null)
            throw new ListenerException("No interface tags exist; unable to find match for listen port " + port + " using interface " + bindAddress);

        List<InetAddress> localAddrs;
        try {
            localAddrs = findAllLocalInetAddresses();
        } catch (SocketException e) {
            throw new ListenerException("Unable to look up network interfaces while finding a match for listen port " +
                               port + " using interface " + bindAddress + ": " + ExceptionUtils.getMessage(e));
        }

        InterfaceTag tag = findTagByName(info.right, bindAddress);
        if (tag == null)
            throw new ListenerException("No interface tag named " + bindAddress + " is known (for listen port " + port + ")");

        Set<String> patterns = tag.getIpPatterns();
        InetAddress match = null;
        for (InetAddress addr : localAddrs) {
            for (String pattern : patterns) {
                if (InetAddressUtil.patternMatchesAddress(pattern, addr)) {
                    if (match != null)
                        logger.log(Level.WARNING, "Interface tag " + bindAddress + " contains patterns matching more than one network addresses on this node.  Will use first match of " + match);
                    match = addr;
                }
            }
        }

        if (match == null)
            throw new ListenerException("No address pattern for interface tag named " + bindAddress + " matches any network address on thsi node (for listen port " + port + ")");

        return match.getHostAddress();
    }

    protected boolean looksLikeInterfaceTagName(String maybeTagname) {
        // It's an interface tag if's non empty and doesn't start with an ASCII digit
        if (maybeTagname == null || maybeTagname.length() < 1)
            return false;
        char initial = maybeTagname.charAt(0);
        return (!(initial >= '0' && initial <= '9'));
    }

    private InterfaceTag findTagByName(Collection<InterfaceTag> tags, String desiredName) {
        for (InterfaceTag tag : tags) {
            if (tag.getName().equalsIgnoreCase(desiredName))
                return tag;
        }
        return null;
    }

    private List<InetAddress> findAllLocalInetAddresses() throws SocketException {
        Set<InetAddress> collected = new HashSet<InetAddress>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements())
            collectAddresses(interfaces.nextElement(), collected);

        return Functions.sort(collected, new Comparator<InetAddress>() {
            public int compare(InetAddress left, InetAddress right) {
                return ArrayUtils.compareArrays(left.getAddress(), right.getAddress());
            }
        });
    }

    private void collectAddresses(NetworkInterface face, Set<InetAddress> collect) {
        Enumeration<InetAddress> addrs = face.getInetAddresses();
        while (addrs.hasMoreElements())
            collect.add(addrs.nextElement());

        Enumeration<NetworkInterface> subs = face.getSubInterfaces();
        while (subs.hasMoreElements())
            collectAddresses(subs.nextElement(), collect);
    }

    private Triple<Long, String, Set<InterfaceTag>> loadInterfaceTags() throws FindException, ParseException {
        ClusterProperty cprop = clusterPropertyManager.findByUniqueName(InterfaceTag.PROPERTY_NAME);
        if (cprop == null)
            return null;
        String stringForm = cprop.getValue();
        Set<InterfaceTag> tags = stringForm == null ? Collections.<InterfaceTag>emptySet() : InterfaceTag.parseMultiple(stringForm);
        return new Triple<Long, String, Set<InterfaceTag>>(cprop.getOid(), stringForm, tags);
    }

    protected Triple<Long, String, Set<InterfaceTag>> getInterfaceTagsCached() {
        Triple<Long, String, Set<InterfaceTag>> tagInfo = interfaceTags.get();
        if (tagInfo == null) {
            try {
                tagInfo = loadInterfaceTags();
                interfaceTags.set(tagInfo);
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to load interface tags: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Invalid interface tags: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
        return tagInfo;
    }

    protected static boolean isEventIgnorable(ApplicationEvent applicationEvent) {
        return applicationEvent instanceof AuditDetailEvent ||
                applicationEvent instanceof MessageProcessed ||
                applicationEvent instanceof FaultProcessed;
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (TransportModule.isEventIgnorable(applicationEvent)) {
            return;
        }

        super.onApplicationEvent(applicationEvent);

        if (!isStarted())
            return;

        if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent event = (EntityInvalidationEvent)applicationEvent;
            if (SsgConnector.class.equals(event.getEntityClass()))
                handleSsgConnectorInvalidationEvent(event);
            else if (ClusterProperty.class.equals(event.getEntityClass()))
                handleClusterPropertyInvalidationEvent(event);
        }
    }

    private void handleClusterPropertyInvalidationEvent(EntityInvalidationEvent event) {
        Triple<Long, String, Set<InterfaceTag>> tags = interfaceTags.get();
        if (tags == null || tags.left == null)
            return;
        
        if (containsMatchingOid(event.getEntityIds(), tags.left)) {
            interfaceTags.set(null);
            Triple<Long, String, Set<InterfaceTag>> newTags = getInterfaceTagsCached();
            if (((newTags.middle == null) != (tags.middle == null)) || !newTags.middle.equals(tags.middle))
                restartAllConnectorsUsingInterfaceTags();
        }
    }

    private boolean containsMatchingOid(long[] ids, long target) {
        for (long id : ids) {
            if (id == target)
                return true;
        }
        return false;
    }

    private void restartAllConnectorsUsingInterfaceTags() {
        try {
            for (SsgConnector connector : ssgConnectorManager.findAll()) {
                if (looksLikeInterfaceTagName(connector.getProperty(SsgConnector.PROP_BIND_ADDRESS)))
                    handleSsgConnectorOperation(EntityInvalidationEvent.UPDATE, connector.getOid());
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to update connectors using interface tags: unable to fetch list of connectors: " + ExceptionUtils.getMessage(e), ExceptionUtils.getMessage(e));
        }
    }

    private void handleSsgConnectorInvalidationEvent(EntityInvalidationEvent event) {
        long[] ids = event.getEntityIds();
        char[] operations = event.getEntityOperations();
        for (int i = 0; i < ids.length; i++)
            handleSsgConnectorOperation(operations[i], ids[i]);
    }

    private void handleSsgConnectorOperation(char operation, long connectorId) {
        try {
            switch (operation) {
                case EntityInvalidationEvent.CREATE:
                case EntityInvalidationEvent.UPDATE:
                    createOrUpdateConnector(connectorId);
                    break;
                case EntityInvalidationEvent.DELETE:
                    removeConnector(connectorId);
                    break;
                default:
                    logger.warning("Unrecognized entity operation: " + operation);
                    break;
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Error processing change for connector oid " + connectorId + ": " + ExceptionUtils.getMessage(t), t);
            connectorErrors.put(connectorId, t);
        }
    }

    private void createOrUpdateConnector(long connectorId) throws FindException, ListenerException {
        SsgConnector c = ssgConnectorManager.findByPrimaryKey(connectorId);
        if (c == null) {
            // Already removed
            return;
        }
        if (c.isEnabled() && connectorIsOwnedByThisModule(c) && isValidConnectorConfig(c))
            addConnector(c);
        else
            removeConnector(connectorId);
    }
}
