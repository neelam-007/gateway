package com.l7tech.server.transport;

import com.l7tech.objectmodel.*;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.transport.SsgConnector.Endpoint;
import com.l7tech.common.util.*;
import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.common.io.PortRange;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;

/**
 * Implementation of {@link SsgConnectorManager}.
 */
public class SsgConnectorManagerImpl
        extends HibernateEntityManager<SsgConnector, EntityHeader>
        implements SsgConnectorManager, InitializingBean, ApplicationListener
{
    protected static final Logger logger = Logger.getLogger(SsgConnectorManagerImpl.class.getName());

    public static final String SYSPROP_FIREWALL_RULES_FILENAME = "com.l7tech.server.firewall.rules.filename";
    public static final String SYSPROP_FIREWALL_UPDATE_PROGRAM = "com.l7tech.server.firewall.update.program";
    private static final String DEFAULT_FIREWALL_UPDATE_PROGRAM = "libexec/update_firewall";
    private static final String FIREWALL_RULES_FILENAME = SyspropUtil.getString(SYSPROP_FIREWALL_RULES_FILENAME,
                                                                                "firewall_rules");

    private final ServerConfig serverConfig;
    private final Map<Long, SsgConnector> knownConnectors = new LinkedHashMap<Long, SsgConnector>();
    private final Map<Endpoint, SsgConnector> httpConnectorsByService = Collections.synchronizedMap(new HashMap<Endpoint, SsgConnector>());
    private final Map<Endpoint, SsgConnector> httpsConnectorsByService = Collections.synchronizedMap(new HashMap<Endpoint, SsgConnector>());
    private final File sudo;

    public SsgConnectorManagerImpl(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        File sudo = null;
        try {
            sudo = SudoUtils.findSudo();
        } catch (IOException e) {
            /* FALLTHROUGH and do without */
        }
        this.sudo = sudo;
    }

    public Class<? extends Entity> getImpClass() {
        return SsgConnector.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return SsgConnector.class;
    }

    public String getTableName() {
        return "connector";
    }

    public EntityType getEntityType() {
        return EntityType.CONNECTOR;
    }

    /**
     * Find a connector that uses the specified scheme and provides access to the specified endpoint.
     * <p/>
     * This can be used to provide good default values for the old serverconfig properties
     * httpPort, httpsPort, clusterhttpport, and clusterhttpsport.
     *
     * @param scheme  a scheme, ie SsgConnector.HTTP or SsgConnector.HTTPS.  Required.
     * @param endpoint  an endpoint that must be supported, ie {@link SsgConnector.Endpoint#OTHER_SERVLETS}.
     * @return an appropriate connector, or null if there is no connector with the specified scheme that
     *         provides access to the specified endpoint.
     * @throws com.l7tech.objectmodel.FindException if there is a problem searching for a matching connector
     */
    public SsgConnector find(String scheme, Endpoint endpoint) throws FindException {
        if (scheme == null) throw new NullPointerException("scheme");
        if (endpoint == null) throw new NullPointerException("endpoint");

        Map<Endpoint, SsgConnector> cache = null;
        if (SsgConnector.SCHEME_HTTP.equals(scheme))
            cache = httpConnectorsByService;
        else if (SsgConnector.SCHEME_HTTPS.equals(scheme))
            cache = httpsConnectorsByService;

        {
            SsgConnector connector = cache == null ? null : cache.get(endpoint);
            if (connector != null)
                return connector;
        }

        // Cache miss.  Find a connector with the desired properties.
        // There won't ever be a lot of these so we'll just scan all the ones with the right scheme.
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("enabled", Boolean.TRUE);
        map.put("scheme", scheme);
        List<SsgConnector> list = findMatching(map);
        SsgConnector found = null;
        for (SsgConnector c : list) {
            if (c.offersEndpoint(endpoint) && scheme.equals(c.getScheme())) {
                found = c;
                break;
            }
        }

        if (found != null && cache != null)
            cache.put(endpoint, found);

        return found;
    }

    private void savePublicPort(String varname, String scheme, Endpoint endpoint) throws FindException {
        SsgConnector c = find(scheme, endpoint);
        if (c == null) {
            serverConfig.removeProperty(varname);
        } else {
            serverConfig.putProperty(varname, Integer.toString(c.getPort()));
        }
        logger.info("Default " + scheme + " port: " + serverConfig.getProperty(varname));
    }

    private void updateDefaultPorts() {
        try {
            savePublicPort("defaultHttpPort", SsgConnector.SCHEME_HTTP, Endpoint.OTHER_SERVLETS);
            savePublicPort("defaultHttpsPort", SsgConnector.SCHEME_HTTPS, Endpoint.OTHER_SERVLETS);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to update default HTTP and HTTPS ports for built-in services: " + ExceptionUtils.getMessage(e), e);
        }
    }

    protected void initDao() throws Exception {
        super.initDao();

        // Initialize known connections
        for (SsgConnector connector : findAll())
            if (connector.isEnabled()) knownConnectors.put(connector.getOid(), connector);

        updateDefaultPorts();

        File program = getFirewallUpdater();
        if (program != null && sudo != null)
            logger.log(Level.INFO, "Using firewall rules updater program: sudo " + program);
        writeFirewallDropfile();
    }

    private void writeFirewallDropfile() {
        File conf = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_CONFIG_DIRECTORY, "/ssg/etc/conf", true);
        String firewallRules = new File(conf, FIREWALL_RULES_FILENAME).getPath();

        try {
            FileUtils.saveFileSafely(firewallRules,  new FileUtils.Saver() {
                public void doSave(FileOutputStream fos) throws IOException {
                    writeFirewallRules(fos);
                }
            });

            runFirewallUpdater(firewallRules);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to update port list dropfile " + FIREWALL_RULES_FILENAME + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Passes the specified firewall rules file to the firewall updater program, if one is configured.
     *
     * @param rulesFile the rules file.  Required.
     */
    private void runFirewallUpdater(String rulesFile) {
        if (sudo == null)
            return;
        File program = getFirewallUpdater();
        if (program == null)
            return;

        try {
            ProcUtils.exec(null, sudo, new String[] { program.getAbsolutePath(), rulesFile, "start" }, null, false);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to execute firewall rules program: " + program + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    /** @return the program to be run whenever the firewall rules change, or null to take no such action. */
    private File getFirewallUpdater() {
        File ssgHome = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_SSG_HOME_DIRECTORY, "/ssg", false);
        File defaultProgram = new File(ssgHome, DEFAULT_FIREWALL_UPDATE_PROGRAM);
        String program = SyspropUtil.getString(SYSPROP_FIREWALL_UPDATE_PROGRAM, defaultProgram.getPath());
        if (program == null || program.length() < 1)
            return null;
        File file = new File(program);
        return file.exists() && file.canExecute() ? file : null;
    }

    private void writeFirewallRules(OutputStream fos) {
        PrintStream ps = new PrintStream(fos);
        try {
            final ArrayList<SsgConnector> list = new ArrayList<SsgConnector>(knownConnectors.values());

            // Add a pseudo-connector for the inter-node communication port
            int rmiPort = serverConfig.getIntProperty(ServerConfig.PARAM_CLUSTER_PORT, 2124);            
            SsgConnector rc = new SsgConnector();
            rc.setPort(rmiPort);
            list.add(rc);

            for (SsgConnector connector : list) {
                String device = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);

                List<PortRange> ranges = connector.getTcpPortsUsed();
                for (PortRange range : ranges) {
                    int portStart = range.getPortStart();
                    int portEnd = range.getPortEnd();

                    ps.print("[0:0] -I INPUT $Rule_Insert_Point ");
                    if (InetAddressUtil.isValidIpAddress(device)) {
                        ps.print(" -d ");
                        ps.print(device);
                    }
                    ps.print(" -p tcp -m tcp --dport ");
                    if (portStart == portEnd)
                        ps.print(portStart);
                    else
                        ps.printf("%d:%d", portStart, portEnd);
                    ps.print(" -j ACCEPT");
                    ps.println();
                }
            }
        } finally {
            ps.flush();
        }
    }

    /**
     * Read the specified firewall rules and parse them into a list of port ranges.
     *
     * @param is the firewall rules to examine. Required.
     * @return a List of PortRange objects.  May be empty but never null.
     * @throws IOException if there is a problem reading or parsing the port range information.
     */
    public static List<PortRange> parseFirewallRules(InputStream is) throws IOException {
        List<PortRange> ranges = new ArrayList<PortRange>();
        Pattern p = Pattern.compile(" -I INPUT \\$Rule_Insert_Point\\s*(?:-d (192.168.1.156))?\\s*-p tcp -m tcp --dport\\s*(\\d+)(?:\\:(\\d+))?\\s*-j ACCEPT");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                String device = m.group(1);
                int portStart = parseInt(m.group(2));
                String portEndStr = m.group(3);
                int portEnd = portEndStr == null ? portStart : parseInt(portEndStr);
                PortRange range = new PortRange(portStart, portEnd, false);
                range.setDevice(device);
                ranges.add(range);
            }
        }
        return ranges;
    }

    private static int parseInt(String s) throws IOException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            throw new IOException("Invalid number in config file", nfe);
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof EntityInvalidationEvent))
            return;
        EntityInvalidationEvent evt = (EntityInvalidationEvent)event;
        if (!SsgConnector.class.isAssignableFrom(evt.getEntityClass()))
            return;
        long[] ids = evt.getEntityIds();
        char[] ops = evt.getEntityOperations();
        for (int i = 0; i < ops.length; i++) {
            char op = ops[i];
            long id = ids[i];

            switch (op) {
                case EntityInvalidationEvent.DELETE:
                    knownConnectors.remove(id);
                    break;
                default:
                    onConnectorChanged(id);
            }
        }
        httpConnectorsByService.clear();
        httpsConnectorsByService.clear();
        updateDefaultPorts();
        writeFirewallDropfile();
    }

    private void onConnectorChanged(long id) {
        try {
            SsgConnector connector = findByPrimaryKey(id);
            if (connector != null && connector.isEnabled())
                knownConnectors.put(id, connector);
            else
                knownConnectors.remove(id);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find just-added or -updated connector with oid " + id + ": " + ExceptionUtils.getMessage(e), e);
        }
    }
}
