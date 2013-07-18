/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

import com.l7tech.gateway.common.transport.SsgConnector;

import java.util.HashMap;
import java.util.Map;

/** @author alex */
public class ConnectorConfig extends SsgConnector {
    private NodeConfig node;

    public NodeConfig getNode() {
        return node;
    }

    public void setNode(NodeConfig node) {
        this.node = node;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("      <connector ");
        sb.append("id=\"").append(getGoid()).append("\" ");
        if (_name != null) sb.append("name=\"").append(_name).append("\" ");
        sb.append("port=\"").append(getPort()).append("\" ");
        sb.append("protocol=\"").append(getScheme()).append("\"");

        Map<String,String> props = getProperties();
        if (!props.isEmpty()) {
            sb.append("\n        <properties>\n");
            for (Map.Entry<String,String> prop : props.entrySet()) {
                sb.append("        <property ");
                sb.append("id=\"").append(-1).append("\">");
                sb.append(prop.getValue());
                sb.append("        </property>\n");
            }
            sb.append("      </connector>");
        } else {
            sb.append("/>");
        }
        return sb.toString();
    }

    public void copyFrom(SsgConnector sc) {
        setGoid(sc.getGoid());
        setName(sc.getName());
        setVersion(sc.getVersion());
        setEnabled(sc.isEnabled());
        setPort(sc.getPort());
        setScheme(sc.getScheme());
        setSecure(sc.isSecure());
        setEndpoints(sc.getEndpoints());
        setClientAuth(sc.getClientAuth());
        setKeystoreOid(sc.getKeystoreOid());
        setKeyAlias(sc.getKeyAlias());

        Map<String,String> props = new HashMap<String, String>();
        for (String name : sc.getPropertyNames()) {
            String prop = sc.getProperty(name);
            props.put(name, prop);
        }
        setProperties(props);
    }
}
