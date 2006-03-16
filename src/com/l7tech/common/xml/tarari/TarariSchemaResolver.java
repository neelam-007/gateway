/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

/**
 * Interface for looking up schemas by target namespace.
 */
public interface TarariSchemaResolver {
    public byte[] resolveSchema(String tns, String location, String baseURI);
}
