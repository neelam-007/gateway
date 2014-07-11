package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.server.service.FirewallRulesManager;

/**
 * User: kpak
 * Date: 4/12/13
 * Time: 1:33 PM
 */
public class MockFirewallRulesManager implements FirewallRulesManager {
    @Override
    public void openPort(String ruleName, int port) {
    }

    @Override
    public void removeRule(String ruleName) {
    }

    @Override
    public void redirectPort(String ruleName, int sourcePort, int destinationPort) {
    }
}