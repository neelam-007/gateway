/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.tarari;

/**
 * Interface implemented by objects that will be usable as sources for hardware-accelerated schemas.
 */
public interface TarariSchemaSource {
    /** @return the schema XML that should be fed to Tarari for this schema document. */
    byte[] getNamespaceNormalizedSchemaDocument();

    /** @return the systemId of the schema. */
    String getSystemId();

    boolean isRejectedByTarari();

    /** Notify that the hardware does not like this schema XML and should not be asked to load it again. */
    void setRejectedByTarari(boolean rejectedByTarari);

    boolean isLoaded();

    /** Notify this schema that it's hardware-loaded status has changed. */
    void setLoaded(boolean loaded);
}
