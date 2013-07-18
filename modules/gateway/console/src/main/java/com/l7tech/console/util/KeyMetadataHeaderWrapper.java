package com.l7tech.console.util;

import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ZoneableEntityHeader;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps an SsgKeyMetadata into an entity header.
 */
public class KeyMetadataHeaderWrapper extends ZoneableEntityHeader {
    private final SsgKeyMetadata metadata;

    public KeyMetadataHeaderWrapper(@NotNull final SsgKeyMetadata metadata) {
        super(metadata.getOid(), EntityType.SSG_KEY_METADATA, null, null, metadata.getVersion());
        this.metadata = metadata;
        setSecurityZoneOid(metadata.getSecurityZone() == null ? null : metadata.getSecurityZone().getOid());
    }

    public String getAlias() {
        return metadata.getAlias();
    }

    public long getKeystoreOid() {
        return metadata.getKeystoreOid();
    }

}
