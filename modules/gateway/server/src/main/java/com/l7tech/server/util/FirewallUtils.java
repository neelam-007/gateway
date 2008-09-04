package com.l7tech.server.util;

import com.l7tech.util.SyspropUtil;
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
    private static final String SYSPROP_FIREWALL_UPDATE_PROGRAM = "com.l7tech.server.firewall.update.program";
    private static final String DEFAULT_FIREWALL_UPDATE_PROGRAM = "/opt/SecureSpan/Appliance/libexec/update_firewall";
    private static final String FIREWALL_RULES_FILENAME = SyspropUtil.getString(SYSPROP_FIREWALL_RULES_FILENAME,
                                                                                "firewall_rules");

    /**
     * Write firewall rules to the given directory and update the live rules.
     *
     * @param rulesDirectory The directory to store the rules file
     * @param connectors The connectors to permit
     */
    public static void openFirewallForConnectors( final File rulesDirectory, final Collection<SsgConnector> connectors ) {
        String firewallRules = new File(rulesDirectory, FIREWALL_RULES_FILENAME).getPath();

        try {
            FirewallRules.writeFirewallDropfile( firewallRules, connectors );
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to update port list dropfile " + FIREWALL_RULES_FILENAME + ": " + ExceptionUtils.getMessage(e), e);
        }

        runFirewallUpdater( firewallRules, true );
    }

    /**
     * Use firewall rules in the given directory and update the live rules.
     *
     * @param rulesDirectory The directory to store the rules file
     */
    public static void closeFirewallForConnectors( final File rulesDirectory ) {
        String firewallRules = new File(rulesDirectory, FIREWALL_RULES_FILENAME).getPath();
        runFirewallUpdater( firewallRules, false );
    }

    /**
     * Passes the specified firewall rules file to the firewall updater program, if one is configured.
     */
    private static void runFirewallUpdater( final String firewallRules, boolean start ) {
        File sudo = null;
        try {
            sudo = SudoUtils.findSudo();
        } catch (IOException e) {
            /* FALLTHROUGH and do without */
        }

        if (sudo == null)
            return;

        File program = getFirewallUpdater();
        if (program == null)
            return;

        logger.log(Level.FINE, "Using firewall rules updater program: sudo " + program);

        try {
            ProcUtils.exec(null, sudo, new String[] { program.getAbsolutePath(), firewallRules, start ? "start" : "stop" }, null, false);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to execute firewall rules program: " + program + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
        }
    }

    /** @return the program to be run whenever the firewall rules change, or null to take no such action. */
    private static File getFirewallUpdater() {
        File defaultProgram = new File(DEFAULT_FIREWALL_UPDATE_PROGRAM);
        String program = SyspropUtil.getString(SYSPROP_FIREWALL_UPDATE_PROGRAM, defaultProgram.getAbsolutePath());
        if (program == null || program.length() < 1)
            return null;
        File file = new File(program);
        return file.exists() && file.canExecute() ? file : null;
    }

}
