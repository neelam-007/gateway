/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(namespace="http://ns.l7tech.com/secureSpan/1.0/monitoring/notification")
public class SnmpTrapNotificationRule extends NotificationRule {
    private String snmpHost;
    private int port = 162;
    private String community;
    private String text;
    private int oidSuffix;

    public SnmpTrapNotificationRule() {
        super(Type.SNMP);
    }

    public String getSnmpHost() {
        return snmpHost;
    }

    public void setSnmpHost(String snmpHost) {
        this.snmpHost = snmpHost;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getOidSuffix() {
        return oidSuffix;
    }

    public void setOidSuffix(int oidSuffix) {
        this.oidSuffix = oidSuffix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SnmpTrapNotificationRule that = (SnmpTrapNotificationRule) o;

        if (oidSuffix != that.oidSuffix) return false;
        if (port != that.port) return false;
        if (community != null ? !community.equals(that.community) : that.community != null) return false;
        if (snmpHost != null ? !snmpHost.equals(that.snmpHost) : that.snmpHost != null) return false;
        if (text != null ? !text.equals(that.text) : that.text != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (snmpHost != null ? snmpHost.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (community != null ? community.hashCode() : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + oidSuffix;
        return result;
    }

    @Override
    public String toString() {
        return "SnmpTrapNotificationRule{" +
                "snmpHost='" + snmpHost + '\'' +
                ", text='" + text + '\'' +
                ", oidSuffix=" + oidSuffix +
                "} " + super.toString();
    }
}
