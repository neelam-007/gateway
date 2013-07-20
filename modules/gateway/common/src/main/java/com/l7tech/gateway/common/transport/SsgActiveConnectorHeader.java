package com.l7tech.gateway.common.transport;

import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.Nullable;

/**
 * Entity header for SsgActiveConnectors
 */
public class SsgActiveConnectorHeader extends ZoneableEntityHeader {
    private String connectorType;
    private boolean enabled;
    private boolean inbound;

    public SsgActiveConnectorHeader( final long id,
                                     final String name,
                                     final String connectorType,
                                     final int version,
                                     final boolean enabled,
                                     final boolean inbound,
                                     @Nullable final Goid securityZoneGoid) {
        super(id, EntityType.SSG_ACTIVE_CONNECTOR, name, null, version);
        this.connectorType = connectorType;
        this.enabled = enabled;
        this.inbound = inbound;
        this.securityZoneGoid = securityZoneGoid;
    }

    public SsgActiveConnectorHeader( final SsgActiveConnector connector ) {
        this( connector.getOid(),
                connector.getName(),
                connector.getType(),
                connector.getVersion(),
                connector.isEnabled(),
                connector.getBooleanProperty( SsgActiveConnector.PROPERTIES_KEY_IS_INBOUND, true ),
                connector.getSecurityZone() == null ? null : connector.getSecurityZone().getGoid());
    }

    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType( final String connectorType ) {
        this.connectorType = connectorType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled( final boolean enabled ) {
        this.enabled = enabled;
    }

    public boolean isInbound() {
        return inbound;
    }

    public void setInbound( final boolean inbound ) {
        this.inbound = inbound;
    }
}
