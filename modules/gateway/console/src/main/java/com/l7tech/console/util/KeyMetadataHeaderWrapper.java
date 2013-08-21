package com.l7tech.console.util;

import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ZoneableEntityHeader;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps an SsgKeyMetadata into an entity header.
 */
public class KeyMetadataHeaderWrapper extends ZoneableEntityHeader {
    private final SsgKeyMetadata metadata;

    public KeyMetadataHeaderWrapper(@NotNull final SsgKeyMetadata metadata) {
        super(metadata.getGoid(), EntityType.SSG_KEY_METADATA, null, null, metadata.getVersion());
        this.metadata = metadata;
        setSecurityZoneId(metadata.getSecurityZone() == null ? null : metadata.getSecurityZone().getGoid());
    }

    public String getAlias() {
        return metadata.getAlias();
    }

    public Goid getKeystoreOid() {
        return metadata.getKeystoreGoid();
    }

}
