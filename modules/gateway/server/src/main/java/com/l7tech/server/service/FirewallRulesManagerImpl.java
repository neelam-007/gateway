package com.l7tech.server.service;

import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.transport.firewall.SsgFirewallRulesManager;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * <p>  Implementation of the {@link FirewallRulesManager} to open a port, redirect a port and to remove an existing firewall rule.</p>
 *
 * @author K.Diep
 */
public class FirewallRulesManagerImpl implements FirewallRulesManager {
    private static final Logger logger = Logger.getLogger(FirewallRulesManagerImpl.class.getName());

    @Inject
    private SsgFirewallRulesManager firewallRulesManager;

    @Override
    public void openPort(@NotNull final String ruleName, final int port) {
        if(ruleName == null || ruleName.trim().isEmpty()) throw new IllegalArgumentException("name is required");
        validatePort(port);
        SsgFirewallRule rule = new SsgFirewallRule();
        rule.setEnabled(true);
        rule.setName(ruleName);
        rule.putProperty("destination-port", String.valueOf(port));
        rule.putProperty("protocol", "tcp");
        rule.putProperty("table", "filter");
        rule.putProperty("jump", "ACCEPT");
        rule.putProperty("chain", "INPUT");
        try {
            SsgFirewallRule c = firewallRulesManager.findByUniqueName(ruleName);
            if(c == null){
                firewallRulesManager.save(rule);
            }
        } catch (SaveException e) {
            logger.warning("Error saving firewall rule for port " + port + ": " + ExceptionUtils.getDebugException(e));
        } catch (FindException e) {
            logger.warning("Error looking for duplicate firewall rule " + ExceptionUtils.getDebugException(e));
        }
    }

    @Override
    public void removeRule(@NotNull final String ruleName) {
        if(ruleName == null || ruleName.trim().isEmpty()) throw new IllegalArgumentException("name is required");
        try {
            SsgFirewallRule c = firewallRulesManager.findByUniqueName(ruleName);
            if(c != null){
                firewallRulesManager.delete(c);
            }
        } catch (FindException e) {
            logger.warning("Error looking for existing firewall rule " + ExceptionUtils.getDebugException(e));
        } catch (DeleteException e) {
            logger.warning("Error deleting firewall rule " + ExceptionUtils.getDebugException(e));
        }
    }

    @Override
    public void redirectPort(@NotNull final String ruleName, final int sourcePort, final int destinationPort) {
        if(ruleName == null || ruleName.trim().isEmpty()) throw new IllegalArgumentException("name is required");
        validatePort(sourcePort);
        validatePort(destinationPort);

        SsgFirewallRule rule = new SsgFirewallRule();
        rule.setEnabled(true);
        rule.setName(ruleName);
        rule.putProperty("destination-port", String.valueOf(sourcePort));
        rule.putProperty("protocol", "tcp");
        rule.putProperty("table", "NAT");
        rule.putProperty("jump", "REDIRECT");
        rule.putProperty("chain", "PREROUTING");
        rule.putProperty("to-ports", String.valueOf(destinationPort));
        try {
            SsgFirewallRule c = firewallRulesManager.findByUniqueName(ruleName);
            if(c == null){
                firewallRulesManager.save(c);
            }
        } catch (SaveException e) {
            logger.warning("Error saving firewall rule: " + ExceptionUtils.getDebugException(e));
        } catch (FindException e) {
            logger.warning("Error looking for duplicate firewall rule " + ExceptionUtils.getDebugException(e));
        }
    }

    private void validatePort(int port) throws IllegalArgumentException{
        if(port < 1 || port > 65535) throw new IllegalArgumentException("port must be between 1 and 65535");
    }
}
