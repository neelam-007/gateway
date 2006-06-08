/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

/**
 * Interface for looking up schemas by target namespace.
 */
public interface TarariSchemaResolver {
    /** This byte array will be returned by a tarari schema resolver to indicate "Resource not resolved, and don't try to get it over the network at all." */
    byte[] TARARISCHEMA_UNRESOLVED = new byte[0];

    public byte[] resolveSchema(String tns, String location, String baseURI);
}
