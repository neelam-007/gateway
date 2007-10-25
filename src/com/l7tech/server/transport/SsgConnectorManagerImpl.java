package com.l7tech.server.transport;

import com.l7tech.objectmodel.*;
import com.l7tech.common.transport.SsgConnector;
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
    private static final String DEFAULT_FIREWALL_UPDATE_PROGRAM = "bin/partition_firewall.pl";
    private static final String FIREWALL_RULES_FILENAME = SyspropUtil.getString(SYSPROP_FIREWALL_RULES_FILENAME,
                                                                                "firewall_rules");

    private final ServerConfig serverConfig;
    private final Map<Long, SsgConnector> knownConnectors = new LinkedHashMap<Long, SsgConnector>();

    public SsgConnectorManagerImpl(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
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

    protected void initDao() throws Exception {
        super.initDao();

        // Initialize known connections
        for (SsgConnector connector : findAll())
            knownConnectors.put(connector.getOid(), connector);

        writeFirewallDropfile();
        File program = getFirewallUpdater();
        if (program != null)
            logger.log(Level.INFO, "Using firewall rules updater program: " + program);
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
        File program = getFirewallUpdater();
        if (program == null)
            return;

        try {
            ProcUtils.exec(null, program, new String[] { rulesFile, "start" }, null, false);
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
            for (SsgConnector connector : new ArrayList<SsgConnector>(knownConnectors.values())) {
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
        writeFirewallDropfile();
    }

    private void onConnectorChanged(long id) {
        try {
            SsgConnector connector = findByPrimaryKey(id);
            knownConnectors.put(id, connector);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find just-added or -updated connector with oid " + id + ": " + ExceptionUtils.getMessage(e), e);
        }
    }
}
