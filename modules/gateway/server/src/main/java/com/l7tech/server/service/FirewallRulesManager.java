package com.l7tech.server.service;

/**
 * <p>An interface for controlling firewall rules, at the moment it allows for;
 *  <ul>
 *      <li>opening a port</li>
 *      <li>removing an existing rule</li>
 *      <li>redirecting a port</li>
 *  </ul>
 * </p>
 * @author K.Diep
 */
public interface FirewallRulesManager {

    /**
     * Configure the firewall to allow connection on the specified port.
     * @param ruleName a unique rule name.
     * @param port the port to allow connection through.  Must be between 1 and 65535.
     */
    public void openPort(String ruleName, int port);

    /**
     * Remove an existing rule from the firewall.
     * @param ruleName the name of the rule to remove.
     */
    public void removeRule(String ruleName);

    /**
     * Configure a rule to redirect traffic from the source port to the destination port.
     * @param ruleName a unique rule name.
     * @param sourcePort the source port.
     * @param destinationPort the destination port.
     */
    public void redirectPort(String ruleName, int sourcePort, int destinationPort);

}
