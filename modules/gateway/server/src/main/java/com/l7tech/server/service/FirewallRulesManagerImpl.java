package com.l7tech.server.service;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.transport.SsgConnectorManager;
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
    private SsgConnectorManager connectorManager;

    @Override
    public void openPort(@NotNull final String ruleName, final int port) {
        if(ruleName == null || ruleName.trim().isEmpty()) throw new IllegalArgumentException("name is required");
        validatePort(port);
        SsgConnector connector = new SsgConnector();
        connector.setEnabled(true);
        connector.setPort(port);
        connector.setScheme(SsgConnector.SCHEME_NA);
        connector.setName(ruleName);
        connector.putProperty("destination-port", String.valueOf(port));
        connector.putProperty("protocol", "tcp");
        connector.putProperty("table", "filter");
        connector.putProperty("jump", "ACCEPT");
        connector.putProperty("chain", "INPUT");
        try {
            SsgConnector c = connectorManager.findByUniqueName(ruleName);
            if(c == null){
                connectorManager.save(connector);
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
            SsgConnector c = connectorManager.findByUniqueName(ruleName);
            if(c != null){
                connectorManager.delete(c);
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

        SsgConnector connector = new SsgConnector();
        connector.setEnabled(true);
        connector.setPort(sourcePort);
        connector.setScheme(SsgConnector.SCHEME_NA);
        connector.setName(ruleName);
        connector.putProperty("destination-port", String.valueOf(sourcePort));
        connector.putProperty("protocol", "tcp");
        connector.putProperty("table", "NAT");
        connector.putProperty("jump", "REDIRECT");
        connector.putProperty("chain", "PREROUTING");
        connector.putProperty("to-ports", String.valueOf(destinationPort));
        try {
            SsgConnector c = connectorManager.findByUniqueName(ruleName);
            if(c == null){
                connectorManager.save(connector);
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
