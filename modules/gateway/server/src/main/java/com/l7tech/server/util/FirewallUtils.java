package com.l7tech.server.util;

import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.IpProtocol;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SudoUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.common.io.ProcUtils;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;

/**
 * Utility for working with firewall
 */
public class FirewallUtils {

    private static final Logger logger = Logger.getLogger( FirewallUtils.class.getName() );

    private static final String SYSPROP_FIREWALL_RULES_FILENAME = "com.l7tech.server.firewall.rules.filename";
    private static final String SYSPROP_FIREWALL6_RULES_FILENAME = "com.l7tech.server.firewall6.rules.filename";

    private static final String FIREWALL_RULES_FILENAME = ConfigFactory.getProperty( SYSPROP_FIREWALL_RULES_FILENAME, "firewall_rules" );
    private static final String FIREWALL6_RULES_FILENAME = ConfigFactory.getProperty( SYSPROP_FIREWALL6_RULES_FILENAME, "firewall6_rules" );

    private static final String SYSPROP_FIREWALL_UPDATE_PROGRAM = "com.l7tech.server.firewall.update.program";
    private static final String DEFAULT_FIREWALL_UPDATE_PROGRAM = "/opt/SecureSpan/Appliance/libexec/update_firewall";

    /**
     * Initialize the firewall.
     *
     * <p>This should be called on startup before any connections are made.</p>
     */
    public static void initializeFirewall() {
        runFirewallUpdater( "-", IpProtocol.IPv4, false );
        runFirewallUpdater( "-", IpProtocol.IPv6, false );
    }

    /**
     * Write firewall rules to the given directory and update the live rules.
     *
     * @param rulesDirectory The directory to store the rules file
     * @param connectors The connectors to permit
     */
    public static void openFirewallForConnectors( final File rulesDirectory, final Collection<SsgConnector> connectors ) {
        String firewallRules = new File(rulesDirectory, "listen_ports").getPath();
        String firewall6Rules = new File(rulesDirectory, "listen6_ports").getPath();

        try {
            FirewallRules.writeFirewallDropfile( firewallRules, connectors, IpProtocol.IPv4 );
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to update port list dropfile " + "listen_ports" + ": " + ExceptionUtils.getMessage(e), e);
        }

        try {
            FirewallRules.writeFirewallDropfile( firewall6Rules, connectors, IpProtocol.IPv6 );
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to update port list dropfile " + "listen6_ports" + ": " + ExceptionUtils.getMessage(e), e);
        }

        runFirewallUpdater( firewallRules, IpProtocol.IPv4, true );
        runFirewallUpdater( firewall6Rules, IpProtocol.IPv6, true );
    }

    public static void openFirewallForRules( final File rulesDirectory, final Collection<SsgFirewallRule> rules ) {
        String firewallRules = new File(rulesDirectory, FIREWALL_RULES_FILENAME).getPath();
        String firewall6Rules = new File(rulesDirectory, FIREWALL6_RULES_FILENAME).getPath();

        try {
            FirewallRules.writeFirewallDropfileForRules( firewallRules, rules, IpProtocol.IPv4 );
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to update port list dropfile " + FIREWALL_RULES_FILENAME + ": " + ExceptionUtils.getMessage(e), e);
        }

        try {
            FirewallRules.writeFirewallDropfileForRules( firewall6Rules, rules, IpProtocol.IPv6 );
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to update port list dropfile " + FIREWALL6_RULES_FILENAME + ": " + ExceptionUtils.getMessage(e), e);
        }

        runFirewallUpdater( firewallRules, IpProtocol.IPv4, true );
        runFirewallUpdater( firewall6Rules, IpProtocol.IPv6, true );
    }

    /**
     * Use firewall rules in the given directory and update the live rules.
     *
     * @param rulesDirectory The directory to store the rules file
     */
    public static void closeFirewallForConnectors( final File rulesDirectory ) {
        String firewallRules = new File(rulesDirectory, FIREWALL_RULES_FILENAME).getPath();
        String firewall6Rules = new File(rulesDirectory, FIREWALL6_RULES_FILENAME).getPath();
        runFirewallUpdater( firewallRules, IpProtocol.IPv4, false );
        runFirewallUpdater( firewall6Rules, IpProtocol.IPv6, false );
    }

    /**
     * Passes the specified firewall rules file to the firewall updater program, if one is configured.
     */
    private static void runFirewallUpdater( final String firewallRules, IpProtocol ipProtocol, boolean start ) {
        File sudo = null;
        try {
            sudo = SudoUtils.findSudo();
        } catch (IOException e) {
            /* FALLTHROUGH and do without */
        }

        if (! ipProtocol.isEnabled() )
            return;

        if (sudo == null)
            return;

        File program = getFirewallUpdater();
        if (program == null)
            return;

        logger.log(Level.FINE, "Using firewall rules updater program: sudo " + program);

        try {
            ProcUtils.exec(null, sudo, new String[] { program.getAbsolutePath(), ipProtocol.name().toLowerCase(), firewallRules, start ? "start" : "stop" }, (byte[])null, false);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to execute firewall rules program: " + program + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
        }
    }

    /** @return the program to be run whenever the firewall rules change, or null to take no such action. */
    private static File getFirewallUpdater() {
        File defaultProgram = new File(DEFAULT_FIREWALL_UPDATE_PROGRAM);
        String program = ConfigFactory.getProperty( SYSPROP_FIREWALL_UPDATE_PROGRAM, defaultProgram.getAbsolutePath() );
        if (program == null || program.length() < 1)
            return null;
        File file = new File(program);
        return file.exists() && file.canExecute() ? file : null;
    }

}
