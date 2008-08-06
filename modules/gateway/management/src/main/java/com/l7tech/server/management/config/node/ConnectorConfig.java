/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.SsgConnectorProperty;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/** @author alex */
@Entity
@Table(name="pc_connector")
public class ConnectorConfig extends SsgConnector {
    private ServiceNodeConfig node;

    @ManyToOne(optional=false, cascade=CascadeType.ALL)
    public ServiceNodeConfig getNode() {
        return node;
    }

    public void setNode(ServiceNodeConfig node) {
        this.node = node;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("      <connector ");
        sb.append("id=\"").append(_oid).append("\" ");
        if (_name != null) sb.append("name=\"").append(_name).append("\" ");
        sb.append("port=\"").append(getPort()).append("\" ");
        sb.append("protocol=\"").append(getScheme()).append("\"");
        final Set<SsgConnectorProperty> props = getProperties();
        if (!props.isEmpty()) {
            sb.append("\n        <properties>\n");
            for (SsgConnectorProperty prop : props) {
                sb.append("        <property ");
                sb.append("id=\"").append(prop.getOid()).append("\">");
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
        setOid(sc.getOid());
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

        Set<SsgConnectorProperty> props = new HashSet<SsgConnectorProperty>();
        for (String name : sc.getPropertyNames()) {
            String prop = sc.getProperty(name);
            props.add(new SsgConnectorProperty(this, name, prop));
        }
        setProperties(props);
    }
}
